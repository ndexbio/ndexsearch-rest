package org.ndexbio.ndexsearch.rest.engine;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.ndexbio.enrichment.rest.client.EnrichmentRestClient;
import org.ndexbio.enrichment.rest.model.DatabaseResult;
import org.ndexbio.enrichment.rest.model.DatabaseResults;
import org.ndexbio.enrichment.rest.model.EnrichmentQuery;
import org.ndexbio.enrichment.rest.model.EnrichmentQueryResult;
import org.ndexbio.enrichment.rest.model.EnrichmentQueryResults;
import org.ndexbio.enrichment.rest.model.exceptions.EnrichmentException;
import org.ndexbio.model.exceptions.NdexException;
import org.ndexbio.model.object.NetworkSearchResult;
import org.ndexbio.model.object.network.NetworkSummary;
import org.ndexbio.ndexsearch.rest.exceptions.SearchException;
import org.ndexbio.ndexsearch.rest.model.InternalSourceResults;
import org.ndexbio.ndexsearch.rest.model.SourceResults;
import org.ndexbio.ndexsearch.rest.model.Query;
import org.ndexbio.ndexsearch.rest.model.QueryResults;
import org.ndexbio.ndexsearch.rest.model.QueryStatus;
import org.ndexbio.ndexsearch.rest.model.SourceConfiguration;
import org.ndexbio.ndexsearch.rest.model.SourceConfigurations;
import org.ndexbio.ndexsearch.rest.model.SourceQueryResult;
import org.ndexbio.ndexsearch.rest.model.SourceQueryResults;
import org.ndexbio.ndexsearch.rest.model.SourceResult;
import org.ndexbio.ndexsearch.rest.model.comparators.SourceQueryResultByRank;
import org.ndexbio.ndexsearch.rest.model.comparators.SourceQueryResultsBySourceRank;

import org.ndexbio.rest.client.NdexRestClientModelAccessLayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ndexbio.interactomesearch.client.InteractomeRestClient;
import org.ndexbio.interactomesearch.object.InteractomeRefNetworkEntry;
import org.ndexbio.interactomesearch.object.InteractomeSearchResult;
import org.ndexbio.interactomesearch.object.SearchStatus;
import org.ndexbio.ndexsearch.rest.services.Configuration;

/**
 * Runs search
 * 
 * @author churas
 */
public class BasicSearchEngineImpl implements SearchEngine {

	public static final String QR_JSON_FILE = "queryresults.json";

	static Logger _logger = LoggerFactory.getLogger(BasicSearchEngineImpl.class);

	ScheduledExecutorService _servicePollExecutor;
	private long _sourcePollingInterval;
	
	private String _dbDir;
	private String _taskDir;
	private boolean _shutdown;

	/**
	 * This should be a map of <query UUID> => Query object
	 */
	private ConcurrentHashMap<String, Query> _queryTasks;

	private ConcurrentLinkedQueue<String> _queryTaskIds;

	/**
	 * This should be a map of <query UUID> => QueryResults object
	 */
	private ConcurrentHashMap<String, QueryResults> _queryResults;

	/**
	 * This should be a map of <database UUID> => Map<Gene => Set of network UUIDs>
	 */
	private ConcurrentHashMap<String, ConcurrentHashMap<String, HashSet<String>>> _databases;

	private AtomicReference<SourceConfigurations> _sourceConfigurations;
	private AtomicReference<SourceResults> _sourceResults;
	private NdexRestClientModelAccessLayer _keywordclient;
	private EnrichmentRestClient _enrichClient;
	private InteractomeRestClient _interactomeClient_ppi;
	private InteractomeRestClient _interactomeClient_association;


	private long _threadSleep = 10;
	private SourceQueryResultsBySourceRank _sourceRankSorter;
	private SourceQueryResultByRank _rankSorter;

	/**
	 * Constructor
	 * 
	 * @param dbDir             directory path containing networks in database
	 * @param taskDir           directory path where tasks will be stored
	 * @param sourceResults
	 * @param keywordclient     REST client for keyword search
	 * @param enrichClient      REST client for enrichment query
	 * @param interactomeClient REST client for interactome query
	 */
	public BasicSearchEngineImpl(final String dbDir, final String taskDir, SourceConfigurations sourceConfigurations, long sourcePollingInterval,
			NdexRestClientModelAccessLayer keywordclient, EnrichmentRestClient enrichClient,
			InteractomeRestClient interactomeClient_ppi, InteractomeRestClient interactomeClient_association) {
		_shutdown = false;
		_dbDir = dbDir;
		_taskDir = taskDir;
		_keywordclient = keywordclient;
		_queryTasks = new ConcurrentHashMap<>();
		_queryResults = new ConcurrentHashMap<>();
		_sourceConfigurations = new AtomicReference<>();
		_sourcePollingInterval = sourcePollingInterval;
		_sourceResults = new AtomicReference<>();
		_queryTaskIds = new ConcurrentLinkedQueue<>();
		_sourceConfigurations.set(sourceConfigurations);
		_enrichClient = enrichClient;
		_interactomeClient_ppi = interactomeClient_ppi;
		this._interactomeClient_association = interactomeClient_association;
		_sourceRankSorter = new SourceQueryResultsBySourceRank();
		_rankSorter = new SourceQueryResultByRank();
		_servicePollExecutor = Executors.newSingleThreadScheduledExecutor();
	}

	/**
	 * Sets milliseconds thread should sleep if no work needs to be done.
	 * 
	 * @param sleepTime
	 */
	public void updateThreadSleepTime(long sleepTime) {
		_threadSleep = sleepTime;
	}

	/**
	 * Calls {@link java.lang.Thread.sleep()} setting sleep time to value set by
	 * {@link #updateThreadSleepTime(long)} which by default is set to 10ms
	 */
	protected void threadSleep() {
		try {
			Thread.sleep(_threadSleep);
		} catch (InterruptedException ie) {

		}
	}

	/**
	 * Processes any query tasks, looping until {@link #shutdown()} is invoked Note:
	 * There is a delay in the loop which can be modified by
	 * {@link #updateThreadSleepTime(long)
	 */
	@Override
	public void run() {
		ScheduledFuture<?> servicePollFuture = _servicePollExecutor.scheduleWithFixedDelay(
				() -> {
					updateSourceResults();
				}, 0, _sourcePollingInterval,TimeUnit.MILLISECONDS);
		
		while (_shutdown == false) {
			String id = _queryTaskIds.poll();
			if (id == null) {
				threadSleep();
				continue;
			}
				processQuery(id, _queryTasks.remove(id));
		}
		
		_logger.debug("Shutdown was invoked");
		
		servicePollFuture.cancel(true);
		_servicePollExecutor.shutdown();
		
		if (this._enrichClient != null) {
			try {
				_enrichClient.shutdown();
			} catch (EnrichmentException ee) {
				_logger.error("Caught exception shutting down enrichment client", ee);
			}
		}
	}

	/**
	 * Tells the thread to exit after finishing its current processing task which is
	 * processed in {@link #run()} method
	 */
	@Override
	public void shutdown() {
		_shutdown = true;
	}

	public void setDatabaseResults(SourceConfigurations sc) {
		_sourceConfigurations.set(sc);
	}

	protected String getQueryResultsFilePath(final String id) {
		return this._taskDir + File.separator + id + File.separator + BasicSearchEngineImpl.QR_JSON_FILE;
	}

	protected void saveQueryResultsToFilesystem(final String id) {
		QueryResults eqr = getQueryResultsFromDb(id);
		if (eqr == null) {
			return;
		}
		File destFile = new File(getQueryResultsFilePath(id));
		ObjectMapper mappy = new ObjectMapper();
		try (FileOutputStream out = new FileOutputStream(destFile)) {
			mappy.writeValue(out, eqr);
		} catch (IOException io) {
			_logger.error("Caught exception writing " + destFile.getAbsolutePath(), io);
		}
		_queryResults.remove(id);
	}

	/**
	 * First tries to get EnrichmentQueryResults from _queryResults list and if that
	 * fails method creates a new EnrichmentQueryResults setting current time in
	 * constructor.
	 * 
	 * @param id
	 * @return
	 */
	protected QueryResults getQueryResultsFromDb(final String id) {
		QueryResults qr = _queryResults.get(id);
		if (qr == null) {
			qr = new QueryResults(System.currentTimeMillis());
		}
		return qr;
	}

	protected QueryResults getQueryResultsFromDbOrFilesystem(final String id) {
		QueryResults qr = _queryResults.get(id);
		if (qr != null) {
			return qr;
		}
		ObjectMapper mappy = new ObjectMapper();
		File qrFile = new File(getQueryResultsFilePath(id));
		if (qrFile.isFile() == false) {
			_logger.error(qrFile.getAbsolutePath() + " is not a file");
			return null;
		}
		try {
			return mappy.readValue(qrFile, QueryResults.class);
		} catch (IOException io) {
			_logger.error("Caught exception trying to load " + qrFile.getAbsolutePath(), io);
		}
		return null;
	}

	protected void updateQueryResultsInDb(final String id, QueryResults updatedQueryResults) {
		_queryResults.merge(id, updatedQueryResults, (oldval, newval) -> newval.updateStartTime(oldval));
	}

	protected SourceQueryResults processEnrichment(final String sourceName, Query query) {
		EnrichmentQuery equery = new EnrichmentQuery();
		equery.setDatabaseList(getEnrichmentDatabaseList(sourceName));
		equery.setGeneList(query.getGeneList());
		SourceQueryResults sqr = new SourceQueryResults();
		sqr.setSourceName(sourceName);
		try {
			String enrichTaskId = _enrichClient.query(equery);
			if (enrichTaskId == null) {
				_logger.error("Query failed");
				sqr.setMessage("Enrichment failed for unknown reason");
				sqr.setStatus(QueryResults.FAILED_STATUS);
				sqr.setProgress(100);
				return sqr;
			}
			sqr.setStatus(QueryResults.SUBMITTED_STATUS);
			sqr.setSourceTaskId(enrichTaskId);
			return sqr;
		} catch (EnrichmentException ee) {
			_logger.error("Caught exception running enrichment", ee);
			sqr.setMessage("Enrichment failed: " + ee.getMessage());
			sqr.setStatus(QueryResults.FAILED_STATUS);
			sqr.setProgress(100);
			return sqr;
		}
	}

	protected SourceQueryResults processInteractome_PPI(final String sourceName, Query query) {

		SourceQueryResults sqr = new SourceQueryResults();
		sqr.setSourceName(sourceName);
		try {
			String interactomeTaskId = this._interactomeClient_ppi.search(query.getGeneList()).toString();
			if (interactomeTaskId == null) {
				_logger.error("Query failed");
				sqr.setMessage("Interactome-ppi failed for unknown reason");
				sqr.setStatus(QueryResults.FAILED_STATUS);
				sqr.setProgress(100);
				return sqr;
			}
			sqr.setStatus(QueryResults.SUBMITTED_STATUS);
			sqr.setSourceTaskId(interactomeTaskId);
			return sqr;
		} catch (NdexException ee) {
			_logger.error("Caught ndexexception running interactome-ppi", ee);
			sqr.setMessage("Interactome failed: " + ee.getMessage());
		} catch (Exception ex) {
			_logger.error("Caught exception running interactome-ppi", ex);
			sqr.setMessage("Interactome-ppi failed: " + ex.getMessage());
		}
		sqr.setStatus(QueryResults.FAILED_STATUS);
		sqr.setProgress(100);
		return sqr;
	}

	protected SourceQueryResults processInteractome_association(final String sourceName, Query query) {

		SourceQueryResults sqr = new SourceQueryResults();
		sqr.setSourceName(sourceName);
		try {
			String interactomeTaskId = this._interactomeClient_association.search(query.getGeneList()).toString();
			if (interactomeTaskId == null) {
				_logger.error("Query failed");
				sqr.setMessage("Interactome-association failed for unknown reason");
				sqr.setStatus(QueryResults.FAILED_STATUS);
				sqr.setProgress(100);
				return sqr;
			}
			sqr.setStatus(QueryResults.SUBMITTED_STATUS);
			sqr.setSourceTaskId(interactomeTaskId);
			return sqr;
		} catch (NdexException ee) {
			_logger.error("Caught ndexexception running interactome-association", ee);
			sqr.setMessage("Interactome-association failed: " + ee.getMessage());
		} catch (Exception ex) {
			_logger.error("Caught exception running interactome-association", ex);
			sqr.setMessage("Interactome-association failed: " + ex.getMessage());
		}
		sqr.setStatus(QueryResults.FAILED_STATUS);
		sqr.setProgress(100);
		return sqr;
	}

	
	protected String getUUIDOfSourceByName(final String sourceName) {
		if (sourceName == null) {
			return null;
		}
		try {
			SourceResults isr = getSourceResults();
			for (SourceResult sr : isr.getResults()) {
				if (sr.getName() == null) {
					continue;
				}
				if (sr.getName().equals(sourceName)) {
					return sr.getUuid();
				}
			}
		} catch (SearchException e) {
			e.printStackTrace();
		}
		return null;
	}

	protected List<String> getEnrichmentDatabaseList(final String sourceName) {
		if (sourceName == null) {
			return null;
		}
		List<String> dbList = new LinkedList<>();
		try {
			SourceResults isr = getSourceResults();
			for (SourceResult sr : isr.getResults()) {
				if (sr.getName() == null) {
					continue;
				}
				if (sr.getName().equals(sourceName)) {
					for (DatabaseResult dr : sr.getDatabases()) {
						dbList.add(dr.getName());
					}
				}
			}
		} catch (SearchException e) {
			e.printStackTrace();
		}
		return dbList;
	}

	protected SourceQueryResults processKeyword(final String sourceName, Query query) {

		try {
			SourceQueryResults sqr = new SourceQueryResults();
			sqr.setSourceName(sourceName);
			StringBuilder sb = new StringBuilder();
			for (String gene : query.getGeneList()) {
				if (gene.isEmpty() == false) {
					sb.append(" ");
				}
				sb.append(gene);
			}
			_logger.info("Query being sent: " + sb.toString());
			NetworkSearchResult nrs = _keywordclient.findNetworks(sb.toString(), null, 0, 100);
			if (nrs == null) {
				_logger.error("Query failed");
				sqr.setMessage(sourceName + " failed for unknown reason");
				sqr.setStatus(QueryResults.FAILED_STATUS);
				sqr.setProgress(100);
				return sqr;
			}
			int rankCounter = 0;
			List<SourceQueryResult> sqrList = new LinkedList<>();
			for (NetworkSummary ns : nrs.getNetworks()) {
				SourceQueryResult sr = new SourceQueryResult();
				sr.setDescription(ns.getName());
				sr.setEdges(ns.getEdgeCount());
				sr.setNodes(ns.getNodeCount());
				sr.setNetworkUUID(ns.getExternalId().toString());
				sr.setPercentOverlap(0);
				sr.setRank(rankCounter++);
				sr.setImageURL(Configuration.getInstance().getUnsetImageURL());
				sqrList.add(sr);
			}
			sqr.setNumberOfHits(sqrList.size());
			sqr.setProgress(100);
			sqr.setStatus(QueryResults.COMPLETE_STATUS);
			sqr.setSourceTaskId(getUUIDOfSourceByName(sourceName));
			sqr.setResults(sqrList);
			_logger.info("Returning sqr");
			return sqr;
		} catch (IOException io) {
			_logger.error("caught ioexceptin ", io);
		} catch (NdexException ne) {
			_logger.error("caught", ne);
		}
		return null;
	}

	/**
	 * 
	 * @param id task id
	 * @param query
	 * @throws NdexException 
	 */
	protected void processQuery(final String id, Query query) {

		QueryResults qr = getQueryResultsFromDb(id);
		qr.setQuery(query.getGeneList());
		qr.setInputSourceList(query.getSourceList());
		qr.setStatus(QueryResults.PROCESSING_STATUS);
		File taskDir = new File(this._taskDir + File.separator + id);
		_logger.info("Creating new task directory:" + taskDir.getAbsolutePath());

		if (taskDir.mkdirs() == false) {
			_logger.error("Unable to create task directory: " + taskDir.getAbsolutePath());
			qr.setStatus(QueryResults.FAILED_STATUS);
			qr.setMessage("Internal error unable to create directory on filesystem");
			qr.setProgress(100);
			updateQueryResultsInDb(id, qr);
			return;
		}

		List<SourceQueryResults> sqrList = new LinkedList<>();
		qr.setSources(sqrList);
		SourceQueryResults sqr = null;
		for (String source : query.getSourceList()) {
			_logger.info("Querying service: " + source);
			
			SourceConfiguration sourceConf = this._sourceConfigurations.get().getSourceConfigurationByName(source);
			if ( sourceConf == null) {
				String message = "Source " + source + " is not configured in this server."; 
				_logger.error(message);
				qr.setStatus(QueryResults.FAILED_STATUS);
				qr.setMessage(message);
				qr.setProgress(100);
				updateQueryResultsInDb(id, qr);
				return;
			}	
			
			
			if (source.equals(SourceResult.ENRICHMENT_SERVICE)) {
				sqr = processEnrichment(source, query);
				sqr.setSourceRank(0);
			} else if (source.equals(SourceResult.KEYWORD_SERVICE)) {
				sqr = processKeyword(source, query);
				sqr.setSourceRank(1);
			} else if (source.equals(SourceResult.INTERACTOME_PPI_SERVICE)) {
				sqr = processInteractome_PPI(source, query);
				sqr.setSourceRank(2);
			} else if (source.equals(SourceResult.INTERACTOME_GENEASSOCIATION_SERVICE)) {
				sqr = processInteractome_association(source, query);
				sqr.setSourceRank(3);
			}

			if (sqr != null) {
				_logger.info("Adding SourceQueryResult for " + source);
				sqr.setSourceUUID(sourceConf.getUuid());
				sqrList.add(sqr);
				qr.setSources(sqrList);
				updateQueryResultsInDb(id, qr);
			}
			sqr = null;
		}
		saveQueryResultsToFilesystem(id);
	}

	@Override
	public String query(Query thequery) throws SearchException {
		if (thequery.getSourceList() == null || thequery.getSourceList().isEmpty()) {
			throw new SearchException("No databases selected");
		}
		_logger.debug("Received query request");
		// @TODO get Jing's uuid generator code that can be a poormans cache
		String id = UUID.randomUUID().toString();
		_queryTasks.put(id, thequery);
		_queryTaskIds.add(id);
		QueryResults qr = new QueryResults(System.currentTimeMillis());
		qr.setInputSourceList(thequery.getSourceList());
		qr.setQuery(thequery.getGeneList());
		qr.setStatus(QueryResults.SUBMITTED_STATUS);
		_queryResults.merge(id, qr, (oldval, newval) -> newval.updateStartTime(oldval));
		return id;
	}

	private void updateSourceResults() {
		SourceConfigurations sourceConfigurations = _sourceConfigurations.get();
		final List<SourceResult> sourceResults = sourceConfigurations.getSources().stream()
				.map((sourceConfiguration) -> {

					final SourceResult sourceResult = new SourceResult();
					sourceResult.setName(sourceConfiguration.getName());
					sourceResult.setUuid(sourceConfiguration.getUuid().toString());
					sourceResult.setDescription(sourceConfiguration.getDescription());
					sourceResult.setEndPoint(sourceConfiguration.getEndPoint());
					String sourceName = sourceConfiguration.getName();

					if (SourceResult.ENRICHMENT_SERVICE.equals(sourceName)) {
						try {
							DatabaseResults dbResults = this._enrichClient.getDatabaseResults();
							sourceResult.setDatabases(dbResults.getResults());
							sourceResult.setVersion("0.1.0");
							//sourceResult.setUuid("eeb4af50-83c4-4e33-ac21-87142403589b");
							sourceResult.setNumberOfNetworks(242);
							sourceResult.setStatus("ok");
						} catch (javax.ws.rs.ProcessingException e) {
								sourceResult.setStatus("error");
							}
						 catch (EnrichmentException e) {
							sourceResult.setStatus("error");
						}
					} else if (SourceResult.INTERACTOME_PPI_SERVICE.equals(sourceName)) {
						try {
							List<InteractomeRefNetworkEntry> dbResults = this._interactomeClient_ppi.getDatabase();
							sourceResult.setVersion("0.1.1a1");
							//sourceResult.setUuid("0857a397-3453-4ae4-8208-e33a283c85ec");
							sourceResult.setNumberOfNetworks(dbResults.size());
							sourceResult.setStatus("ok");
						} catch (javax.ws.rs.ProcessingException e) {
							sourceResult.setStatus("error");
						}
						catch (NdexException e) {
							sourceResult.setStatus("error");
						}
					} else if (SourceResult.INTERACTOME_GENEASSOCIATION_SERVICE.equals(sourceName)) {
						try {
							List<InteractomeRefNetworkEntry> dbResults = this._interactomeClient_association.getDatabase();
							sourceResult.setVersion("0.2");
							//sourceResult.setUuid("1857a397-3453-4ae4-8208-e33a283c85ec");
							sourceResult.setNumberOfNetworks(dbResults.size());
							sourceResult.setStatus("ok");
						} catch (javax.ws.rs.ProcessingException e) {
							sourceResult.setStatus("error");
						}
						catch (NdexException e) {
							sourceResult.setStatus("error");
						}
					}  else if (sourceName == SourceResult.KEYWORD_SERVICE) {
						sourceResult.setVersion("0.2.0");
						//sourceResult.setUuid("33b9c3ca-13e5-48b9-bcd2-09070203350a");
						sourceResult.setNumberOfNetworks(2009);
						sourceResult.setStatus("ok");
					}
					return sourceResult;
				}).collect(Collectors.toList());
		InternalSourceResults internalSourceResults = new InternalSourceResults();
		internalSourceResults.setResults(sourceResults);
		_sourceResults.set(internalSourceResults);
	}
	
	@Override
	public SourceResults getSourceResults() throws SearchException {
		
		return _sourceResults.get();
	}

	/**
	 * 
	 * @param sqRes
	 * @return number of hits for this source
	 */
	protected int updateEnrichmentSourceQueryResults(SourceQueryResults sqRes) {
		try {
			EnrichmentQueryResults qr = this._enrichClient.getQueryResults(sqRes.getSourceTaskId(), 0, 0);
			sqRes.setMessage(qr.getMessage());
			sqRes.setProgress(qr.getProgress());
			sqRes.setStatus(qr.getStatus());

			sqRes.setWallTime(qr.getWallTime());
			List<SourceQueryResult> sqResults = new LinkedList<>();
			if (qr.getResults() != null) {
				for (EnrichmentQueryResult qRes : qr.getResults()) {
					SourceQueryResult sqr = new SourceQueryResult();
					sqr.setDescription(qRes.getDatabaseName() + ": " + qRes.getDescription());
					sqr.setEdges(qRes.getEdges());
					sqr.setHitGenes(qRes.getHitGenes());
					sqr.setNetworkUUID(qRes.getNetworkUUID());
					sqr.setNodes(qRes.getNodes());
					sqr.setPercentOverlap(qRes.getPercentOverlap());
					sqr.setRank(qRes.getRank());
					sqr.setImageURL(qRes.getImageURL());
					sqr.getDetails().put("PValue", Double.valueOf(qRes.getpValue()));
					sqResults.add(sqr);
				}
			}
			sqRes.setResults(sqResults);
			sqRes.setNumberOfHits(sqResults.size());
			return sqRes.getNumberOfHits();
		} catch (EnrichmentException ee) {
			_logger.error("caught exception", ee);
		}
		return 0;
	}

	// type should be either 'i' or 'a' which maps to interactom ppi and association. 
	protected int updateInteractomeSourceQueryResults(SourceQueryResults sqRes, String type) {
		
		try {
			InteractomeRestClient client = type.equals("i")? 
					_interactomeClient_ppi : _interactomeClient_association;
			
			UUID interactomeTaskId = UUID.fromString(sqRes.getSourceTaskId());
			List<InteractomeSearchResult> qr = client.getSearchResult(interactomeTaskId);
			SearchStatus status = client.getSearchStatus(interactomeTaskId);
			sqRes.setMessage(status.getMessage());
			sqRes.setProgress(status.getProgress());
			sqRes.setStatus(status.getStatus());

			sqRes.setWallTime(status.getWallTime());
			List<SourceQueryResult> sqResults = new LinkedList<>();
			if (qr != null) {
				for (InteractomeSearchResult qRes : qr) {
					SourceQueryResult sqr = new SourceQueryResult();
					sqr.setDescription(qRes.getDescription());
					sqr.setEdges(qRes.getEdgeCount());
					sqr.setHitGenes(qRes.getHitGenes());
					sqr.setNetworkUUID(qRes.getNetworkUUID());
					sqr.setNodes(qRes.getNodeCount());
					sqr.setPercentOverlap(qRes.getPercentOverlap());
					sqr.setImageURL(qRes.getImageURL());
					sqr.setRank(qRes.getRank());
					sqr.setDetails(qRes.getDetails());
					sqResults.add(sqr);
				}
			}
			sqRes.setResults(sqResults);
			sqRes.setNumberOfHits(sqResults.size());
			return sqRes.getNumberOfHits();
		} catch (NdexException ee) {
			_logger.error("caught exception", ee);
		}
		return 0;
	}

	protected void checkAndUpdateQueryResults(QueryResults qr) {
		// if its complete just return
		if (qr.getStatus().equals(QueryResults.COMPLETE_STATUS)) {
			return;
		}
		if (qr.getStatus().equals(QueryResults.FAILED_STATUS)) {
			return;
		}
		int hitCount = 0;
		int numComplete = 0;
		if (qr.getSources() != null) {
			for (SourceQueryResults sqRes : qr.getSources()) {
				_logger.info("Examining status of " + sqRes.getSourceName());
				if (sqRes.getProgress() == 100) {
					hitCount += sqRes.getNumberOfHits();
					numComplete++;
					continue;
				}
				if (sqRes.getSourceName().equals(SourceResult.ENRICHMENT_SERVICE)) {
					_logger.info("adding hits to hit count");
					hitCount += updateEnrichmentSourceQueryResults(sqRes);
					if (sqRes.getProgress() == 100) {
						numComplete++;
					}
				} else if (sqRes.getSourceName().equals(SourceResult.INTERACTOME_PPI_SERVICE) || 
						sqRes.getSourceName().equals(SourceResult.INTERACTOME_GENEASSOCIATION_SERVICE)) {
					_logger.info("adding hits to hit count");
					hitCount += updateInteractomeSourceQueryResults(sqRes,
							(sqRes.getSourceName().equals(SourceResult.INTERACTOME_PPI_SERVICE)? "i": "a"));
					if (sqRes.getProgress() == 100) {
						numComplete++;
					}
				}
			}
			if (numComplete >= qr.getSources().size()) {
				qr.setProgress(100);
				qr.setStatus(QueryStatus.COMPLETE_STATUS);
			} else {
				qr.setProgress(Math.round(((float) numComplete / (float) qr.getSources().size()) * 100));
			}
			qr.setNumberOfHits(hitCount);
		}
	}

	protected void filterQueryResultsBySourceList(QueryResults qr, final String source) {
		if (source == null || source.isEmpty() || source.trim().isEmpty()) {
			_logger.debug("Source not defined leave all results in");
			return;
		}

		String[] splitStr = source.split("\\s+,\\s+");
		HashSet<String> sourceSet = new HashSet<>();
		for (String entry : splitStr) {
			sourceSet.add(entry);
		}
		List<SourceQueryResults> newSqrList = new LinkedList<>();
		for (SourceQueryResults sqr : qr.getSources()) {
			if (!sourceSet.contains(sqr.getSourceName())) {
				_logger.debug(sqr.getSourceName() + " not in source list. Skipping.");
				continue;
			}
			newSqrList.add(sqr);
		}
		qr.setSources(newSqrList);
		return;
	}

	protected void filterQueryResultsByStartAndSize(QueryResults qr, int start, int size) {
		if (start == 0 && size == 0) {
			return;
		}
		if (qr.getSources() == null || qr.getSources().isEmpty()) {
			return;
		}
		int counter = 0;
		List<SourceQueryResults> newSQRS = null;
		List<SourceQueryResult> srcQueryRes = null;
		Collections.sort(qr.getSources(), _sourceRankSorter);
		newSQRS = new LinkedList<>();
		for (SourceQueryResults sqr : qr.getSources()) {
			Collections.sort(sqr.getResults(), _rankSorter);

			srcQueryRes = null;
			for (SourceQueryResult sq : sqr.getResults()) {

				if (counter < start) {
					counter++;
					continue;
				}
				if (counter >= size) {
					break;
				}
				if (srcQueryRes == null) {
					srcQueryRes = new LinkedList<>();
				}
				srcQueryRes.add(sq);
				counter++;
			}
			if (srcQueryRes != null) {
				sqr.setResults(srcQueryRes);
				newSQRS.add(sqr);
			}
		}
		qr.setSources(newSQRS);
	}

	/**
	 * Returns
	 * 
	 * @param id     Id of the query.
	 * @param start  starting index to return from. Starting index is 0.
	 * @param size   Number of results to return. If 0 means all from starting index
	 *               so to get all set both {@code start} and {@code size} to 0.
	 * @param source comma delimited list of sources to return. null or empty string
	 *               means all.
	 * @return {@link org.ndexbio.ndexsearch.rest.model.QueryResults} object or null
	 *         if no result could be found.
	 * @throws SearchException If there was an error getting the results
	 */
	@Override
	public QueryResults getQueryResults(final String id, final String source, int start, int size)
			throws SearchException {
		_logger.info("Got queryresults request: " + id);
		QueryResults qr = this.getQueryResultsFromDbOrFilesystem(id);
		if (qr == null) {
			return null;
		}
		if (start < 0) {
			throw new SearchException("start parameter must be value of 0 or greater");
		}
		if (size < 0) {
			throw new SearchException("size parameter must be value of 0 or greater");
		}
		checkAndUpdateQueryResults(qr);
		filterQueryResultsBySourceList(qr, source);
		filterQueryResultsByStartAndSize(qr, start, size);
		return qr;
	}

	@Override
	public QueryStatus getQueryStatus(String id) throws SearchException {
		QueryResults qr = this.getQueryResultsFromDbOrFilesystem(id);
		if (qr == null) {
			return null;
		}
		checkAndUpdateQueryResults(qr);
		if (qr.getSources() != null) {
			for (SourceQueryResults sqr : qr.getSources()) {
				sqr.setResults(null);
			}
		}
		return qr;
	}

	@Override
	public void delete(String id) throws SearchException {
		_logger.debug("Deleting task " + id);
		QueryResults qr = this.getQueryResultsFromDbOrFilesystem(id);
		if (qr == null) {
			_logger.error("cant find " + id + " to delete");
			return;
		}
		if (qr.getSources() == null) {
			return;
		}
		for (SourceQueryResults sqr : qr.getSources()) {
			if (sqr.getSourceName().equals(SourceResult.ENRICHMENT_SERVICE)) {
				try {
					_logger.debug("Calling enrichment DELETE on id: " + sqr.getSourceTaskId());
					_enrichClient.delete(sqr.getSourceTaskId());
				} catch (EnrichmentException ee) {
					throw new SearchException("caught error trying to delete enrichment: " + ee.getMessage());
				}
			} else if ( sqr.getSourceName().equals(SourceResult.INTERACTOME_PPI_SERVICE) || 
					    sqr.getSourceName().equals(SourceResult.INTERACTOME_GENEASSOCIATION_SERVICE)) {
				// TODO handle this when a delete function is added to the interactome search.
			}
		}
		// TODO delete the local file system copy
		File thisTaskDir = new File(this._taskDir + File.separator + id);
		if (thisTaskDir.exists() == false) {
			return;
		}
		_logger.debug("Attempting to delete task from filesystem: " + thisTaskDir.getAbsolutePath());
		if (FileUtils.deleteQuietly(thisTaskDir) == false) {
			_logger.error("There was a problem deleting the directory: " + thisTaskDir.getAbsolutePath());
		}
	}

	@Override
	public InputStream getNetworkOverlayAsCX(final String id, final String sourceUUID, String networkUUID)
			throws SearchException, NdexException {
		QueryResults qr = this.getQueryResultsFromDbOrFilesystem(id);
		checkAndUpdateQueryResults(qr);
		for (SourceQueryResults sqRes : qr.getSources()) {
			if ( ! sqRes.getSourceUUID().equals(UUID.fromString(sourceUUID))) {
				continue;
			}
			
			if (sqRes.getSourceName().equals(SourceResult.ENRICHMENT_SERVICE)) {
				try {
					return _enrichClient.getNetworkOverlayAsCX(sqRes.getSourceTaskId(), "", networkUUID);
				} catch (EnrichmentException ee) {
					throw new SearchException("unable to get network: " + ee.getMessage());
				}
			} else if (sqRes.getSourceName().equals(SourceResult.KEYWORD_SERVICE)) {
				try {
					_logger.info("Returning network as stream: " + networkUUID);
					return this._keywordclient.getNetworkAsCXStream(UUID.fromString(networkUUID));
				} catch (IOException ee) {
					throw new SearchException("unable to get network: " + ee.getMessage());
				} catch (NdexException ne) {
					throw new SearchException("unable to get network " + ne.getMessage());
				}
			} else if (sqRes.getSourceName().equals(SourceResult.INTERACTOME_PPI_SERVICE)) {
				return this._interactomeClient_ppi.getOverlaidNetworkStream(UUID.fromString(sqRes.getSourceTaskId()),
						UUID.fromString(networkUUID));
			} else if (sqRes.getSourceName().equals(SourceResult.INTERACTOME_GENEASSOCIATION_SERVICE)) {
				return this._interactomeClient_association.getOverlaidNetworkStream(UUID.fromString(sqRes.getSourceTaskId()),
						UUID.fromString(networkUUID));
			}
			
/*			for (SourceQueryResult sqr : sqRes.getResults()) {
				if (sqr.getNetworkUUID() == null || !sqr.getNetworkUUID().equals(networkUUID)) {
					continue;
				}
				if (sqRes.getSourceName().equals(SourceResult.ENRICHMENT_SERVICE)) {
					try {
						return _enrichClient.getNetworkOverlayAsCX(sqRes.getSourceTaskId(), "", sqr.getNetworkUUID());
					} catch (EnrichmentException ee) {
						throw new SearchException("unable to get network: " + ee.getMessage());
					}
				} else if (sqRes.getSourceName().equals(SourceResult.KEYWORD_SERVICE)) {
					try {
						_logger.info("Returning network as stream: " + networkUUID);
						return this._keywordclient.getNetworkAsCXStream(UUID.fromString(networkUUID));
					} catch (IOException ee) {
						throw new SearchException("unable to get network: " + ee.getMessage());
					} catch (NdexException ne) {
						throw new SearchException("unable to get network " + ne.getMessage());
					}
				} else if (sqRes.getSourceName().equals(SourceResult.INTERACTOME_PPI_SERVICE)) {
					return this._interactomeClient_ppi.getOverlaidNetworkStream(UUID.fromString(sqRes.getSourceTaskId()),
							UUID.fromString(sqr.getNetworkUUID()));
				} else if (sqRes.getSourceName().equals(SourceResult.INTERACTOME_GENEASSOCIATION_SERVICE)) {
					return this._interactomeClient_association.getOverlaidNetworkStream(UUID.fromString(sqRes.getSourceTaskId()),
							UUID.fromString(sqr.getNetworkUUID()));
				}
			} */
		}
		return null;
	}
	
	
	

}
