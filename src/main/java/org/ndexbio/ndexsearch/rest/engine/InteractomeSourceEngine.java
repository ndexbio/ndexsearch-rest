package org.ndexbio.ndexsearch.rest.engine;

import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import javax.ws.rs.ProcessingException;
import org.ndexbio.interactomesearch.client.InteractomeRestClient;
import org.ndexbio.interactomesearch.object.InteractomeRefNetworkEntry;
import org.ndexbio.interactomesearch.object.InteractomeSearchResult;
import org.ndexbio.interactomesearch.object.SearchStatus;
import org.ndexbio.model.exceptions.NdexException;
import org.ndexbio.ndexsearch.rest.exceptions.SearchException;
import org.ndexbio.ndexsearch.rest.model.Query;
import org.ndexbio.ndexsearch.rest.model.QueryResults;
import org.ndexbio.ndexsearch.rest.model.SourceQueryResult;
import org.ndexbio.ndexsearch.rest.model.SourceQueryResults;
import org.ndexbio.ndexsearch.rest.model.SourceResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Wrapper around InteractomeRestClient to provide a clean consistent
 * interface to interactome service
 * 
 * @author churas
 */
public class InteractomeSourceEngine implements SourceEngine {

	static Logger _logger = LoggerFactory.getLogger(InteractomeSourceEngine.class);

	private final String _sourceName;
	private InteractomeRestClient _client;
	private int _rank;
	private int _searchStatusMaxRetries;
	private long _searchStatusRetryWaitMillis;

	/**
	 * Constructor 
	 *
	 * @param sourceName Name of source
	 * @param client Client used to connect to interactome service
	 * @param rank denotes rank of results
	 */
	public InteractomeSourceEngine(final String sourceName, InteractomeRestClient client,
			int rank){
		_sourceName = sourceName;
		_client = client;
		_rank = rank;
		_searchStatusMaxRetries = 3;
		_searchStatusRetryWaitMillis = 500;
	}
	
	/**
	 * Runs query passed in and returns result object with status
	 * of processing or failure if task failed
	 * @param query
	 * @return 
	 */
	@Override
	public SourceQueryResults getSourceQueryResults(Query query) {
		SourceQueryResults sqr = new SourceQueryResults();
		sqr.setSourceName(_sourceName);
		sqr.setSourceRank(_rank);
		try { 
			UUID interactomeTaskId = _client.search(query.getGeneList());
			if (interactomeTaskId == null) {
				_logger.error("Query failed");
				sqr.setMessage(_sourceName + " failed for unknown reason");
				sqr.setStatus(QueryResults.FAILED_STATUS);
				sqr.setProgress(100);
				return sqr;
			}
			sqr.setStatus(QueryResults.SUBMITTED_STATUS);
			sqr.setSourceTaskId(interactomeTaskId.toString());
			return sqr;
		} catch (Exception ex) {
			_logger.error("Caught exception running " + _sourceName, ex);
			sqr.setMessage(_sourceName + " failed : " + ex.getMessage());
		}
		sqr.setStatus(QueryResults.FAILED_STATUS);
		sqr.setProgress(100);
		return sqr;
	}

	/**
	 * Updates sRes in place by querying client
	 * @param sRes 
	 */
	@Override
	public void updateSourceResult(SourceResult sRes) {
		try {
			List<InteractomeRefNetworkEntry> dbResults = _client.getDatabase();
			sRes.setVersion("1.0");
			if (dbResults == null){
				_logger.error("For {} source no "
						+ "results found when querying "
						+ "for sources", this._sourceName);
				sRes.setNumberOfNetworks(0);
				sRes.setStatus("error");
				return;
			}
			sRes.setNumberOfNetworks(dbResults.size());
			sRes.setStatus("ok");
		} catch (Exception e) {
			_logger.error("For " + this._sourceName
					+ " Exception while querying sources", e);
			sRes.setStatus("error");
		}
	}

	/**
	 * Gets the result from a completed search. Now if search is not complete
	 * this will raise an exception
	 * @param interactomeTaskid
	 * @return
	 * @throws NdexException if search is missing, failed, or incomplete
	 */
	private List<SourceQueryResult> getSearchResults(final UUID interactomeTaskid) throws NdexException {
		List<SourceQueryResult> sqResults = new LinkedList<>();
		List<InteractomeSearchResult> qr = _client.getSearchResult(interactomeTaskid);
		if (qr != null) {
			_logger.debug("For {} source task {} had {} results",
					new Object[]{_sourceName,
						interactomeTaskid.toString(),
						qr.size()});
			for (InteractomeSearchResult qRes : qr) {
				SourceQueryResult sqr = new SourceQueryResult();
				sqr.setDescription(qRes.getDescription());
				sqr.setEdges(qRes.getEdgeCount());
				sqr.setHitGenes(qRes.getHitGenes());
				sqr.setNetworkUUID(qRes.getNetworkUUID());
				sqr.setNodes(qRes.getNodeCount());
				sqr.setPercentOverlap(qRes.getPercentOverlap());
				sqr.setImageURL(qRes.getImageURL());
				//sqr.setLegendURL(qRes.get);
				sqr.setRank(qRes.getRank());
				sqr.setDetails(qRes.getDetails());
				sqResults.add(sqr);
			}
		}
		return sqResults;
	}
	
	private SearchStatus getSearchStatus(final UUID interactomeTaskId) throws NdexException {
		int count = 1;
		
		Exception lastException = null;
		while (count <= _searchStatusMaxRetries){
			try {
				return _client.getSearchStatus(interactomeTaskId);
			} catch(ProcessingException pe){
				_logger.info("On try " + Integer.toString(count)
						+ " of "
						+ Integer.toString(this._searchStatusMaxRetries) 
						+ " caught exception: " + pe.getMessage(), pe);
				lastException = pe;
			} catch(NdexException ne){
				_logger.info("On try " + Integer.toString(count)
						+ " of "
						+ Integer.toString(this._searchStatusMaxRetries) 
						+ " caught exception: " + ne.getMessage(), ne);
				lastException = ne;
			}
			try {
				Thread.sleep(_searchStatusRetryWaitMillis);
			} catch(InterruptedException ie){
				
			}
			count++;
		}
		if (lastException != null){
			throw new NdexException(lastException.getMessage(), lastException);
		}
		throw new NdexException("Exceeded retry count");
	}
	
	/**
	 * Queries for status of task associated with sqRes and updates sqRes
	 * with progress, message and if progress of task is 100 the results are
	 * also updated, otherwise results are an empty list
	 * 
	 * @param sqRes 
	 */
	@Override
	public void updateSourceQueryResults(SourceQueryResults sqRes) {
		try {
			if (sqRes.getProgress() == 100){
				// nothing needs to be updated cause it is already 100
			}
			UUID interactomeTaskId = UUID.fromString(sqRes.getSourceTaskId());
			
			SearchStatus status = getSearchStatus(interactomeTaskId);
			sqRes.setMessage(status.getMessage());
			sqRes.setProgress(status.getProgress());
			sqRes.setStatus(status.getStatus());

			sqRes.setWallTime(status.getWallTime());
			List<SourceQueryResult> sqResults = null;
			if (status.getProgress() == 100){
				_logger.debug("For source {} task {} progress is 100", 
						_sourceName, interactomeTaskId.toString());
				sqResults = this.getSearchResults(interactomeTaskId);
			} else {
				sqResults = new LinkedList<>();
			}
			sqRes.setResults(sqResults);
			sqRes.setNumberOfHits(sqResults.size());
		} catch (NdexException ee) {
            String errmsg = ee.getMessage();
            // This may no longer occur now that code has been refactored, but
		   // this is a fix for UD-1420 to not log an error if interactome 
            // raises an exception cause the task is still processing
            if (errmsg == null || 
                    !errmsg.contains("This search has no result ready. "
							+ "Search status: processing")){
                _logger.error("caught exception", ee);
            } else {
                _logger.warn("caught exception querying for search status", ee);
            }
		}
	}

	/**
	 * Does nothing
	 * @param id
	 * @throws SearchException 
	 */
	@Override
	public void delete(final String id) throws SearchException {
	}

	/**
	 * Gets the network as CX stream from client
	 * @param id
	 * @param networkId
	 * @return
	 * @throws SearchException
	 * @throws NdexException 
	 */
	@Override
	public InputStream getOverlaidNetworkAsCXStream(final String id,
			final String networkId) throws SearchException, NdexException {
		try {
			return _client.getOverlaidNetworkStream(UUID.fromString(id),
						UUID.fromString(networkId));
		} catch(NdexException ne){
			_logger.error("Caught Ndex exception trying to get network:  " + networkId + " for task: " + id, ne);
			try {
				Thread.sleep(200);
			} catch(InterruptedException ie){
				
			}
			return _client.getOverlaidNetworkStream(UUID.fromString(id),
						UUID.fromString(networkId));
		}
	}

	/**
	 * Gets databases from client
	 * @return
	 * @throws SearchException
	 * @throws NdexException 
	 */
	@Override
	public Object getDatabases() throws SearchException, NdexException {
		return _client.getDatabase();
	}

	/**
	 * Does nothing
	 */
	@Override
	public void shutdown() {
	}
	
}
