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
import java.util.Map;
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
import org.ndexbio.enrichment.rest.model.DatabaseResults;
import org.ndexbio.model.exceptions.NdexException;
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ndexbio.interactomesearch.object.InteractomeRefNetworkEntry;

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

	private long _threadSleep = 10;
	private SourceQueryResultsBySourceRank _sourceRankSorter;
	private SourceQueryResultByRank _rankSorter;
	private Map<String, SourceEngine> _sources;

	/**
	 * Constructor
	 * 
	 * @param dbDir             directory path containing networks in database
	 * @param taskDir           directory path where tasks will be stored
	 * @param sourceConfigurations Sources to query against
	 * @param sourcePollingInterval Interval in ms to poll for updates on sources
	 * @param sources Map of source name to SourceEngine
	 */
	public BasicSearchEngineImpl(final String dbDir, final String taskDir,
            SourceConfigurations sourceConfigurations, long sourcePollingInterval,
			Map<String, SourceEngine> sources) throws SearchException {
		_shutdown = false;
		_dbDir = dbDir;
		_taskDir = taskDir;
		_queryTasks = new ConcurrentHashMap<>();
		_queryResults = new ConcurrentHashMap<>();
		_sourceConfigurations = new AtomicReference<>();
		_sourcePollingInterval = sourcePollingInterval;
		_sourceResults = new AtomicReference<>();
		_queryTaskIds = new ConcurrentLinkedQueue<>();
		_sourceConfigurations.set(sourceConfigurations);
		_sourceRankSorter = new SourceQueryResultsBySourceRank();
		_rankSorter = new SourceQueryResultByRank();
		_servicePollExecutor = Executors.newSingleThreadScheduledExecutor();
		if (sources == null){
			throw new SearchException("Sources cannot be null");
		}
		_sources = sources;
	}

	/**
	 * Sets SourceResults
	 * @param sres 
	 */
	protected void setSourceResults(SourceResults sres){
		_sourceResults.set(sres);
	}
	
	/**
	 * Sets milliseconds thread should sleep if no work needs to be done.
	 * 
	 * @param sleepTime
	 */
	public void updateThreadSleepTime(long sleepTime) {
        _logger.debug("Thread sleep time updated to {} ms", sleepTime);
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
	 * Before entering the loop, this method also starts up a separate thread that periodically checks for 
	 * changes with sources
	 */
	@Override
	public void run() {
        _logger.debug("Starting service update polling task "
                + "with update interval of {} ms", _sourcePollingInterval);
		ScheduledFuture<?> servicePollFuture = _servicePollExecutor.scheduleWithFixedDelay(
				() -> {
					updateSourceResults();
				}, 0, _sourcePollingInterval,TimeUnit.MILLISECONDS);
		
        _logger.info("Starting monitoring loop");
		while (_shutdown == false) {
			String id = _queryTaskIds.poll();
			if (id == null) {
				threadSleep();
				continue;
			}
				processQuery(id, _queryTasks.remove(id));
		}
		
		_logger.info("Stopping monitoring loop");
		
		servicePollFuture.cancel(true);
		_servicePollExecutor.shutdown();
		
		_sources.values().forEach((se) -> {
			se.shutdown();
		});
	}

	/**
	 * Tells the thread to exit after finishing its current processing task which is
	 * processed in {@link #run()} method
	 */
	@Override
	public void shutdown() {
        _logger.info("Shutdown requested");
		_shutdown = true;
	}

	protected String getQueryResultsFilePath(final String id) {
		return this._taskDir + File.separator + id + File.separator + BasicSearchEngineImpl.QR_JSON_FILE;
	}

	protected void saveQueryResultsToFilesystem(final String id) {
		QueryResults eqr = getQueryResultsFromDb(id);

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
	 * First tries to get QueryResults from _queryResults list and if that
	 * fails method creates a new QueryResults setting current time in
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

	/**
	 * First tries to get QueryResults from _queryResults and if not found there
	 * attempts to load it from the filesystem
	 * @param id
	 * @return 
	 */
	protected QueryResults getQueryResultsFromDbOrFilesystem(final String id) {
		QueryResults qr = _queryResults.get(id);
		if (qr != null) {
			return qr;
		}
		ObjectMapper mappy = new ObjectMapper();
		File qrFile = new File(getQueryResultsFilePath(id));
		if (qrFile.isFile() == false) {
			_logger.error("{} is not a file", qrFile.getAbsolutePath());
			return null;
		}
		try {
			return mappy.readValue(qrFile, QueryResults.class);
		} catch (IOException io) {
			_logger.error("Caught exception trying to load " + qrFile.getAbsolutePath(), io);
		}
		return null;
	}

	/**
	 * Updates the database with updatedQueryResults passed in
	 * @param id UUID of query results to update
	 * @param updatedQueryResults data to update minus start time which keeps the value found
	 *                            previously
	 */
	protected void updateQueryResultsInDb(final String id, QueryResults updatedQueryResults) {
		_queryResults.merge(id, updatedQueryResults, (oldval, newval) -> newval.updateStartTime(oldval));
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
		_logger.debug("Creating new task directory {}", taskDir.getAbsolutePath());

		if (taskDir.mkdirs() == false) {
			_logger.error("Unable to create task directory: {}", taskDir.getAbsolutePath());
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
			_logger.debug("Querying service: {}", source);
			
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
			if (_sources.containsKey(source)){
				sqr = _sources.get(source).getSourceQueryResults(query);
			} else {
				_logger.error("Unknown source {} no query performed", source);
			}

			if (sqr != null) {
				_logger.debug("Adding SourceQueryResult for {}", source);
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
		_logger.info("Received query request {}", thequery.toString());
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

	/**
	 * Queries source services to get updated configuration information about 
	 * what data they contain
	 */
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
					_logger.debug("Updating source {}", sourceName);
					if (_sources.containsKey(sourceName)){
						_sources.get(sourceName).updateSourceResult(sourceResult);
					} else {
						_logger.error("Unknown source {} no update performed", sourceName);
					}
					/**
					
					}  else if (SourceResult.KEYWORD_SERVICE.equals(sourceName)) {
						sourceResult.setVersion("0.2.0");
						sourceResult.setNumberOfNetworks(2009);
						sourceResult.setStatus("ok");
					} else {
						_logger.error("Unknown source {} no update performed", sourceName);
					}
					*/
					return sourceResult;
				}).collect(Collectors.toList());
		InternalSourceResults internalSourceResults = new InternalSourceResults();
		internalSourceResults.setResults(sourceResults);
		setSourceResults(internalSourceResults);
	}
	
	/**
	 * Gets SourceResults
	 * @return
	 * @throws SearchException 
	 */
	@Override
	public SourceResults getSourceResults() throws SearchException {
		_logger.debug("Request to get SourceResults");
		return _sourceResults.get();
	}

	protected void checkAndUpdateQueryResults(QueryResults qr) {
		// if its complete just return
		if (qr.getStatus().equals(QueryResults.COMPLETE_STATUS)) {
			_logger.debug("Returning completed query");
			return;
		}
		if (qr.getStatus().equals(QueryResults.FAILED_STATUS)) {
			_logger.debug("Returning failed query");
			return;
		}
		int hitCount = 0;
		int numComplete = 0;
		if (qr.getSources() != null) {
			for (SourceQueryResults sqRes : qr.getSources()) {
				_logger.debug("Examining status of {}", sqRes.getSourceName());
				if (sqRes.getProgress() == 100) {
					_logger.debug("{} already completed processing",
								sqRes.getSourceName());
					hitCount += sqRes.getNumberOfHits();
					numComplete++;
					continue;
				}
				if (_sources.containsKey(sqRes.getSourceName())){
					hitCount += _sources.get(sqRes.getSourceName()).updateSourceQueryResults(sqRes);
					if (sqRes.getProgress() == 100) {
						_logger.debug("{} completed processing",
								sqRes.getSourceName());
						numComplete++;
					}
				} else {
					_logger.error("Unknown source {}", sqRes.getSourceName());
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
				_logger.debug("{} not in source list. Skipping.", sqr.getSourceName());
				continue;
			}
			newSqrList.add(sqr);
		}
		qr.setSources(newSqrList);
	}

	protected void filterQueryResultsByStartAndSize(QueryResults qr, int start, int size) {
		if (start == 0 && size == 0) {
			_logger.debug("That is odd start of 0 and size of 0 requested");
			return;
		}
		if (qr.getSources() == null || qr.getSources().isEmpty()) {
			_logger.debug("No sources or source list is empty");
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
		_logger.debug("Got query results request: {}", id);
		QueryResults qr = this.getQueryResultsFromDbOrFilesystem(id);
		if (qr == null) {
			_logger.debug("No results for id {} found", id);
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
	public QueryStatus getQueryStatus(final String id) throws SearchException {
		_logger.debug("Got query status request: {}", id);
		QueryResults qr = this.getQueryResultsFromDbOrFilesystem(id);
		if (qr == null) {
			_logger.debug("No results for id {} found", id);
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
	public void delete(final String id) throws SearchException {
		_logger.debug("Deleting task " + id);
		QueryResults qr = this.getQueryResultsFromDbOrFilesystem(id);
		if (qr == null) {
			_logger.error("Can not find task {} to delete", id);
			return;
		}
		if (qr.getSources() == null) {
			_logger.error("No sources found for task {}", id);
			return;
		}
		for (SourceQueryResults sqr : qr.getSources()) {
			if (_sources.containsKey(sqr.getSourceName())){
				_sources.get(sqr.getSourceName()).delete(id);
			} else {
				throw new SearchException("No Source matching name "
						+ sqr.getSourceName() + " found");
			}
		}

		//Delete local file system copy
		File thisTaskDir = new File(this._taskDir + File.separator + id);
		if (thisTaskDir.exists() == false) {
			_logger.debug("{} directory does not exist. returning",
					thisTaskDir.getAbsolutePath());
			return;
		}
		_logger.debug("Attempting to delete task from filesystem: {} ",
				thisTaskDir.getAbsolutePath());
		if (FileUtils.deleteQuietly(thisTaskDir) == false) {
			_logger.error("There was a problem deleting the directory: {}",
					thisTaskDir.getAbsolutePath());
		}
	}

	@Override
	public InputStream getNetworkOverlayAsCX(final String id, final String sourceUUID,
			final String networkUUID)
			throws SearchException, NdexException {
		QueryResults qr = this.getQueryResultsFromDbOrFilesystem(id);
		checkAndUpdateQueryResults(qr);
		for (SourceQueryResults sqRes : qr.getSources()) {
			if ( ! sqRes.getSourceUUID().equals(UUID.fromString(sourceUUID))) {
				continue;
			}
			if (_sources.containsKey(sqRes.getSourceName())){
				return _sources.get(sqRes.getSourceName()).getOverlaidNetworkAsCXStream(sqRes.getSourceTaskId(),
						networkUUID);
			
			} else {
				throw new SearchException("No Source matching name "
						+ sqRes.getSourceName() + " found");
			}
		}
		return null;
	}
	
	@Override
	public DatabaseResults getEnrichmentDatabases() throws SearchException {
		try {
			return (DatabaseResults)_sources.get(SourceResult.ENRICHMENT_SERVICE).getDatabases();
		} catch(NdexException ne){
			throw new SearchException(ne.getMessage());
		}
	}
	
	@Override
	public List<InteractomeRefNetworkEntry> getInteractomePpiDatabases() throws NdexException {
		try {
			return (List<InteractomeRefNetworkEntry>)_sources.get(SourceResult.INTERACTOME_PPI_SERVICE).getDatabases();
		}
		catch(SearchException se){
				throw new NdexException(se.getMessage());
		}
	}
	
	@Override
	public List<InteractomeRefNetworkEntry> getInteractomeGeneAssociationDatabases() throws NdexException {
		try {
			return (List<InteractomeRefNetworkEntry>)_sources.get(SourceResult.INTERACTOME_GENEASSOCIATION_SERVICE).getDatabases();
		}
		catch(SearchException se){
				throw new NdexException(se.getMessage());
		}
	} 
}
