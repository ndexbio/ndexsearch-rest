package org.ndexbio.ndexsearch.rest.engine;

import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
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
 *
 * @author churas
 */
public class InteractomeSourceEngine implements SourceEngine {

	static Logger _logger = LoggerFactory.getLogger(InteractomeSourceEngine.class);

	private final String _sourceName;
	private InteractomeRestClient _client;
	private int _rank;

	public InteractomeSourceEngine(final String sourceName, InteractomeRestClient client,
			int rank){
		_sourceName = sourceName;
		_client = client;
		_rank = rank;
	}
	
	@Override
	public SourceQueryResults getSourceQueryResults(Query query) {
		SourceQueryResults sqr = new SourceQueryResults();
		sqr.setSourceName(_sourceName);
		sqr.setSourceRank(_rank);
		try {
			String interactomeTaskId = _client.search(query.getGeneList()).toString();
			if (interactomeTaskId == null) {
				_logger.error("Query failed");
				sqr.setMessage(_sourceName + " failed for unknown reason");
				sqr.setStatus(QueryResults.FAILED_STATUS);
				sqr.setProgress(100);
				return sqr;
			}
			sqr.setStatus(QueryResults.SUBMITTED_STATUS);
			sqr.setSourceTaskId(interactomeTaskId);
			return sqr;
		} catch (NdexException ee) {
			_logger.error("Caught ndexexception running " + _sourceName, ee);
			sqr.setMessage(_sourceName + " failed : " + ee.getMessage());
		} catch (Exception ex) {
			_logger.error("Caught exception running " + _sourceName, ex);
			sqr.setMessage(_sourceName + " failed : " + ex.getMessage());
		}
		sqr.setStatus(QueryResults.FAILED_STATUS);
		sqr.setProgress(100);
		return sqr;
	}

	@Override
	public void updateSourceResult(SourceResult sRes) {
		try {
			List<InteractomeRefNetworkEntry> dbResults = _client.getDatabase();
			sRes.setVersion("1.0");
			sRes.setNumberOfNetworks(dbResults.size());
			sRes.setStatus("ok");
		} catch (javax.ws.rs.ProcessingException|NdexException e) {
			_logger.error("Exception while querying sources", e);
			sRes.setStatus("error");
		}
	}

	@Override
	public void updateSourceQueryResults(SourceQueryResults sqRes) {
		try {
			UUID interactomeTaskId = UUID.fromString(sqRes.getSourceTaskId());
			List<InteractomeSearchResult> qr = _client.getSearchResult(interactomeTaskId);
			SearchStatus status = _client.getSearchStatus(interactomeTaskId);
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
			return;
		} catch (NdexException ee) {
            String errmsg = ee.getMessage();
            // fix for UD-1420 do not log an error if interactome 
            // raises an exception cause the task is still processing
            if (errmsg == null || 
                    !errmsg.contains("This search has no result ready. Search status: processing")){
                _logger.error("caught exception", ee);
            } else {
                _logger.debug("caught exception", ee);
            }
		}
	}

	@Override
	public void delete(final String id) throws SearchException {
	}

	@Override
	public InputStream getOverlaidNetworkAsCXStream(final String id,
			final String networkId) throws SearchException, NdexException {
		return _client.getOverlaidNetworkStream(UUID.fromString(id),
					UUID.fromString(networkId));
	}

	@Override
	public Object getDatabases() throws SearchException, NdexException {
		return _client.getDatabase();
	}

	@Override
	public void shutdown() {
	}
	
}
