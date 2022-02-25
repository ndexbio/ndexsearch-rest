package org.ndexbio.ndexsearch.rest.engine;

import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeSet;
import org.ndexbio.enrichment.rest.client.EnrichmentRestClient;
import org.ndexbio.enrichment.rest.model.DatabaseResults;
import org.ndexbio.enrichment.rest.model.EnrichmentQuery;
import org.ndexbio.enrichment.rest.model.EnrichmentQueryResult;
import org.ndexbio.enrichment.rest.model.EnrichmentQueryResults;
import org.ndexbio.enrichment.rest.model.exceptions.EnrichmentException;
import org.ndexbio.ndexsearch.rest.exceptions.SearchException;
import org.ndexbio.ndexsearch.rest.model.DatabaseResult;
import org.ndexbio.ndexsearch.rest.model.Query;
import org.ndexbio.ndexsearch.rest.model.QueryResults;
import org.ndexbio.ndexsearch.rest.model.SourceQueryResult;
import org.ndexbio.ndexsearch.rest.model.SourceQueryResults;
import org.ndexbio.ndexsearch.rest.model.SourceResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author churas
 */
public class EnrichmentSourceEngine implements SourceEngine {

	static Logger _logger = LoggerFactory.getLogger(EnrichmentSourceEngine.class);

	private EnrichmentRestClient _enrichClient;
	private TreeSet<String> _databaseNameSet = new TreeSet<>();
	private String _sourceName;
	
	public EnrichmentSourceEngine(EnrichmentRestClient enrichClient){
		_enrichClient = enrichClient;
		_sourceName = SourceResult.ENRICHMENT_SERVICE;
	}
	
	public EnrichmentSourceEngine(EnrichmentRestClient enrichClient,
			final String sourceName){
		_enrichClient = enrichClient;
		_sourceName = sourceName;
	}
	
	/**
	 * update database names for enrichment with data from sourceResult
	 * @return 
	 */
	private void updateDatabaseNameSet(final SourceResult sourceResult) {
		_databaseNameSet.clear();
		try {
			if (sourceResult.getDatabases() == null){
				throw new SearchException("No databases found in "
						+ "enrichment service");
			}
			for (DatabaseResult dr : sourceResult.getDatabases()) {
				_databaseNameSet.add(dr.getName());
			}
		} catch (SearchException e) {
			_logger.error("Exception encountered", e);
		}
	}
	
	@Override
	public SourceQueryResults getSourceQueryResults(final Query query) {
		EnrichmentQuery equery = new EnrichmentQuery();
		equery.setDatabaseList(_databaseNameSet);
		equery.setGeneList(new TreeSet<>(query.getGeneList()));
		equery.setGeneAnnotationServices(query.getGeneAnnotationServices());
		SourceQueryResults sqr = new SourceQueryResults();
		sqr.setSourceName(_sourceName);
		sqr.setSourceRank(0);
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
	
	/**
	 * Attempts to convert {@link org.ndexbio.ndexsearch.rest.model.DatabaseResult.getNumberOfNetworks()}
	 * to int. 
	 * @param dr
	 * @return value of number of networks as int. If value is null or not convertible to int
	 *         then 0 is returned
	 */
	private int getNumberOfNetworks(DatabaseResult dr){
		if (dr.getNumberOfNetworks() == null){
			_logger.error("Number of networks was null for Database {}. Setting to 0", 
					dr.getName());
			return 0;
		}
		try {
			return Integer.parseInt(dr.getNumberOfNetworks());
		} catch(NumberFormatException nfe){
			_logger.error("Setting number for networks to 0 for database {}. "
					+ "Error converting {} to int : {}",
					new Object[]{dr.getName(), dr.getNumberOfNetworks(),
						nfe.getMessage()});
		}
		return 0;
	}
	
	@Override
	public void updateSourceResult(SourceResult sRes) {
		try {
			DatabaseResults dbResults = _enrichClient.getDatabaseResults();
			sRes.setDatabases(dbResults.getResults());
			sRes.setVersion("0.1.0");

			if (dbResults.getResults() != null){
				int totalNetworks = 0;
				for (DatabaseResult dr : dbResults.getResults()){
					totalNetworks += getNumberOfNetworks(dr);
				}
				sRes.setNumberOfNetworks(totalNetworks);
			} else {
				_logger.error("No results to parse for total networks");
			}

			sRes.setStatus("ok");

		} catch (javax.ws.rs.ProcessingException|EnrichmentException e) {
			_logger.error("Exception while querying sources. "
					+ "Not changing current database name set", e);
			return;
		} 
		updateDatabaseNameSet(sRes);
	}

	@Override
	public void updateSourceQueryResults(SourceQueryResults sqRes) {
		try {
			if (sqRes.getProgress() == 100){
				// nothing needs to be done since progress is 100
				return;
			}
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
					sqr.setUrl(qRes.getUrl());
					sqr.getDetails().put("PValue", Double.valueOf(qRes.getpValue()));
					sqr.getDetails().put("similarity", Double.valueOf(qRes.getSimilarity()));
					sqr.getDetails().put("totalNetworkCount", Integer.valueOf(qRes.getTotalNetworkCount()));
					sqr.setTotalGeneCount(qRes.getTotalGeneCount());
					sqResults.add(sqr);
				}
			}
			sqRes.setResults(sqResults);
			sqRes.setNumberOfHits(sqResults.size());
		} catch (EnrichmentException ee) {
			_logger.error("caught exception", ee);
		}
	}
	
	@Override
	public InputStream getOverlaidNetworkAsCXStream(final String id,
			final String networkId) throws SearchException {
		try {
			return _enrichClient.getNetworkOverlayAsCX(id, "", networkId);
		} catch (EnrichmentException ee) {
			throw new SearchException("Unable to get network: " + ee.getMessage());
		}
	}

	@Override
	public void delete(final String id) throws SearchException {
		try {
			_logger.debug("Calling enrichment DELETE on id: {}", id);
			_enrichClient.delete(id);
		} catch (EnrichmentException ee) {
			throw new SearchException("caught error trying to delete enrichment: " + ee.getMessage());
		}
	}
	
	@Override
	public Object getDatabases() throws SearchException {
		try{
			return _enrichClient.getDatabaseResults();
		} catch(EnrichmentException ee){
			throw new SearchException("caught error trying to get databases: "
					+ ee.getMessage());
		}
	}

	@Override
	public void shutdown() {
		try {
			if (_enrichClient != null){
				_enrichClient.shutdown();
			}
		} catch(EnrichmentException ee){
			_logger.error("caught exception invoking shutdown on enrichment client",
					ee);
		}
	}
	
	
}
