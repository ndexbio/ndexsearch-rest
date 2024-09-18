package org.ndexbio.ndexsearch.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import org.ndexbio.ndexsearch.rest.model.QueryResults;
import org.ndexbio.ndexsearch.rest.model.SourceQueryResult;
import org.ndexbio.ndexsearch.rest.model.SourceQueryResults;
import org.ndexbio.ndexsearch.rest.model.ValidatedQueryGenes;

/**
 * Makes a copy of QueryResults object
 * @author churas
 */
public class QueryResultsCopier {
	
	public static QueryResults copy(QueryResults sourceQr){
		QueryResults qr = copyMinusSourceResults(sourceQr);
		if (qr == null){
			return null;
		}
		qr.setSources(copySourceQueryResults(sourceQr.getSources(), true));
		return qr;
	}
	
	public static QueryResults copyNoSourceResults(QueryResults sourceQr){
		QueryResults qr = copyMinusSourceResults(sourceQr);
		if (qr == null){
			return null;
		}
		qr.setSources(copySourceQueryResults(sourceQr.getSources(), false));
		return qr;
	}
	
	private static QueryResults copyMinusSourceResults(QueryResults sourceQr){
		if (sourceQr == null){
			return sourceQr;
		}
		QueryResults qr = new QueryResults();
		qr.setStartTime(sourceQr.getStartTime());
		qr.setMessage(sourceQr.getMessage());
		qr.setNumberOfHits(sourceQr.getNumberOfHits());
		qr.setProgress(sourceQr.getProgress());
		qr.setInputSourceList(sourceQr.getInputSourceList());
		qr.setQuery(sourceQr.getQuery());
		qr.setSource(sourceQr.getSource());
		qr.setStatus(sourceQr.getStatus());
		qr.setStart(sourceQr.getStart());
		qr.setWallTime(sourceQr.getWallTime());
		qr.setValidatedGenes(copyValidatedGenes(sourceQr.getValidatedGenes()));
		return qr;
	}
	
	
	
	public static List<SourceQueryResults> copySourceQueryResults(List<SourceQueryResults> sourceQrs, boolean includeResults){
		if (sourceQrs == null){
			return null;
		}
		
		ArrayList<SourceQueryResults> destSqrList = new ArrayList<>();
		for (SourceQueryResults sqr : sourceQrs){
			SourceQueryResults destSqr = new SourceQueryResults();
			destSqr.setMessage(sqr.getMessage());
			destSqr.setNumberOfHits(sqr.getNumberOfHits());
			destSqr.setProgress(sqr.getProgress());
			destSqr.setSourceName(sqr.getSourceName());
			destSqr.setSourceRank(sqr.getSourceRank());
			destSqr.setSourceTaskId(sqr.getSourceTaskId());
			destSqr.setSourceUUID(sqr.getSourceUUID());
			destSqr.setStatus(sqr.getStatus());
			destSqr.setWallTime(sqr.getWallTime());
			if (includeResults == false){
				destSqr.setResults(null);
			} else {
				destSqr.setResults(copySourceQueryResultList(sqr.getResults()));
			}
			destSqrList.add(destSqr);
		}
		return destSqrList;
	}
	
	public static List<SourceQueryResult> copySourceQueryResultList(List<SourceQueryResult> sourceQr){
		if (sourceQr == null){
			return null;
		}
		ArrayList<SourceQueryResult> destQr = new ArrayList<>();
		for(SourceQueryResult sqr : sourceQr){
			SourceQueryResult dest = new SourceQueryResult();
			dest.setDescription(sqr.getDescription());
			dest.setEdges(sqr.getEdges());
			dest.setNodes(sqr.getNodes());
			dest.setHitGenes(copySet(sqr.getHitGenes()));
			dest.setImageURL(sqr.getImageURL());
			dest.setLegendURL(sqr.getLegendURL());
			dest.setNetworkUUID(sqr.getNetworkUUID());
			dest.setPercentOverlap(sqr.getPercentOverlap());
			dest.setRank(sqr.getRank());
			dest.setTotalGeneCount(sqr.getTotalGeneCount());
			dest.setUrl(sqr.getUrl());
			if (sqr.getDetails() == null){
				dest.setDetails(null);
			} else {
				HashMap<String, Object> details = new HashMap<>();
				for (String key : sqr.getDetails().keySet()){
					details.put(key, sqr.getDetails().get(key));
				}
				dest.setDetails(details);
			}
			destQr.add(dest);
		}
		return destQr;
	}
	
	public static ValidatedQueryGenes copyValidatedGenes(ValidatedQueryGenes sourceGenes){
		if (sourceGenes == null){
			return null;
		}
		ValidatedQueryGenes dest = new ValidatedQueryGenes();
		dest.setInvalid(copySet(sourceGenes.getInvalid()));
		dest.setQueryGenes(copySet(sourceGenes.getQueryGenes()));
		
		if (sourceGenes.getNormalizedGenes() != null){
			HashMap<String, String> normGenes = new HashMap<>();
			for (String key : sourceGenes.getNormalizedGenes().keySet()){
				normGenes.put(key, sourceGenes.getNormalizedGenes().get(key));
			}
			dest.setNormalizedGenes(normGenes);
		} else {
			dest.setNormalizedGenes(null);
		}
		return dest;
	}
	
	public static Set<String> copySet(Set<String> srcSet){
		if (srcSet == null){
			return null;
		}
		TreeSet<String> dest = new TreeSet<>();
		for (String s: srcSet){
			dest.add(s);
		}
		return dest;
	}
	
	public static List<String> copyStringList(List<String> srcList){
		if (srcList == null){
			return null;
		}
		ArrayList<String> destList = new ArrayList<>();
		for (String s : srcList){
			destList.add(s);
		}
		return destList;
	}
	
}
