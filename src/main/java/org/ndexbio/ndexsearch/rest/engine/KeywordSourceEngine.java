package org.ndexbio.ndexsearch.rest.engine;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.ndexbio.model.exceptions.NdexException;
import org.ndexbio.model.object.NdexStatus;
import org.ndexbio.model.object.NetworkSearchResult;
import org.ndexbio.model.object.network.NetworkSummary;
import org.ndexbio.ndexsearch.rest.exceptions.SearchException;
import org.ndexbio.ndexsearch.rest.model.Query;
import org.ndexbio.ndexsearch.rest.model.QueryResults;
import org.ndexbio.ndexsearch.rest.model.SourceQueryResult;
import org.ndexbio.ndexsearch.rest.model.SourceQueryResults;
import org.ndexbio.ndexsearch.rest.model.SourceResult;
import org.ndexbio.rest.client.NdexRestClientModelAccessLayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Wrapper around NDEx REST client to provide a clean consistent
 * interface to keyword search query
 * @author churas
 */
public class KeywordSourceEngine implements SourceEngine {

	static Logger _logger = LoggerFactory.getLogger(KeywordSourceEngine.class);

	private NdexRestClientModelAccessLayer _keywordclient;
	private final String _sourceTaskId;
	private final String _unsetImageURL;
	private int _rank;
	
	public KeywordSourceEngine(NdexRestClientModelAccessLayer keywordclient,
			final String sourceTaskId, final String unsetImageURL,
			int rank){
		_keywordclient = keywordclient;
		_sourceTaskId = sourceTaskId;
		_unsetImageURL = unsetImageURL;
		_rank = rank;
	}
	
	List<SourceQueryResult> getSourceQueryResultListFromNetworkSearchResult(NetworkSearchResult nrs){
		int rankCounter = 0;
		List<SourceQueryResult> sqrList = new LinkedList<>();
		if (nrs.getNetworks() == null){
			return sqrList;
		}
		for (NetworkSummary ns : nrs.getNetworks()) {
			SourceQueryResult sr = new SourceQueryResult();
			sr.setDescription(ns.getName());
			sr.setEdges(ns.getEdgeCount());
			sr.setNodes(ns.getNodeCount());
			sr.setNetworkUUID(ns.getExternalId().toString());
			sr.setPercentOverlap(0);
			sr.setRank(rankCounter++);
			sr.setImageURL(_unsetImageURL);
			sqrList.add(sr);
		}
		return sqrList;
	}
	@Override
	public SourceQueryResults getSourceQueryResults(final Query query) {
		SourceQueryResults sqr = new SourceQueryResults();
		sqr.setSourceName(SourceResult.KEYWORD_SERVICE);
		sqr.setSourceRank(_rank);
		try {
			StringBuilder sb = new StringBuilder();
			for (String gene : query.getGeneList()) {
				if (gene.isEmpty() == false) {
					sb.append(" ");
				}
				sb.append(gene);
			}
			_logger.debug("Keyword query string: {}", sb.toString());
			NetworkSearchResult nrs = _keywordclient.findNetworks(sb.toString(), null, 0, 100);
			if (nrs == null) {
				_logger.error("Query failed");
				sqr.setMessage("failed for unknown reason");
				sqr.setStatus(QueryResults.FAILED_STATUS);
				sqr.setProgress(100);
				return sqr;
			}
			List<SourceQueryResult> sqrList = getSourceQueryResultListFromNetworkSearchResult(nrs);
			sqr.setNumberOfHits(sqrList.size());
			sqr.setProgress(100);
			sqr.setStatus(QueryResults.COMPLETE_STATUS);
			sqr.setSourceTaskId(_sourceTaskId);
			sqr.setResults(sqrList);
			_logger.debug("Returning result from keyword query");
			return sqr;
		} catch (NdexException|IOException io) {
			_logger.error("caught ioexception ", io);
			sqr.setMessage(SourceResult.KEYWORD_SERVICE + " failed : " + 
					io.getMessage());
		}
		sqr.setStatus(QueryResults.FAILED_STATUS);
		sqr.setProgress(100);
		return sqr;
	}

	/**
	 * The keyword call is run locally so there is nothing to refresh
	 * @param sRes 
	 */
	@Override
	public void updateSourceResult(final SourceResult sRes) {
		try {
			NdexStatus status = _keywordclient.getServerStatus();
			
			sRes.setNumberOfNetworks(status.getNetworkCount());
			if (status.getMessage() != null && status.getMessage().equalsIgnoreCase("online")){
				sRes.setStatus("ok");
			} else {
				sRes.setStatus("error");
			}
			Map<String, Object> props = status.getProperties();
			if (props != null){
				sRes.setVersion((String)props.getOrDefault("ServerVersion", "unknown"));
			} else {
				sRes.setVersion("unknown");
			}
		} catch(NdexException|IOException ie){
			_logger.error("Caught exception trying to get status of server", ie);
		}
	}

	/**
	 * Just returns number of hits from sqRes since get call fully runs query
	 * @param sqRes
	 * @return 
	 */
	@Override
	public void updateSourceQueryResults(final SourceQueryResults sqRes) {
	}

	/**
	 * There is nothing to delete so just return
	 * @param id
	 * @throws SearchException 
	 */
	@Override
	public void delete(String id) throws SearchException {
	}

	@Override
	public InputStream getOverlaidNetworkAsCXStream(final String id, 
			final String networkId) throws SearchException {
		try {
			return _keywordclient.getNetworkAsCXStream(UUID.fromString(networkId));
		} catch(NdexException|IOException ee){
			throw new SearchException("Unable to get network: " + ee.getMessage());
		}
	}

	/**
	 * Currently returns null cause
	 * 
	 * @return null
	 * @throws SearchException 
	 */
	@Override
	public Object getDatabases() throws SearchException {
		return null;
	}

	/**
	 * Nothing to shutdown so just return
	 */
	@Override
	public void shutdown() {
	}
	
}
