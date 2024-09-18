package org.ndexbio.ndexsearch.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.TreeSet;
import java.util.UUID;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.ndexbio.ndexsearch.rest.model.QueryResults;
import org.ndexbio.ndexsearch.rest.model.SourceQueryResult;
import org.ndexbio.ndexsearch.rest.model.SourceQueryResults;
import org.ndexbio.ndexsearch.rest.model.ValidatedQueryGenes;

/**
 *
 * @author churas
 */
public class TestQueryResultsCopier {
	
	protected QueryResults _sourceQr = null;
	protected ValidatedQueryGenes _sourceVQG = null;
	public TestQueryResultsCopier() {
	}
	
	@BeforeClass
	public static void setUpClass() {
	}
	
	@AfterClass
	public static void tearDownClass() {
	}
	
	@Before
	public void setUp() {
		_sourceQr = new QueryResults();
		_sourceQr.setMessage("message");
		_sourceQr.setNumberOfHits(1);
		_sourceQr.setProgress(2);
		_sourceQr.setSize(0);
		_sourceQr.setSource("source");
		_sourceQr.setStart(3);
		_sourceQr.setStartTime(4);
		_sourceQr.setStatus("status");
		_sourceQr.setWallTime(5);
		_sourceQr.setQuery(Arrays.asList("invalid1", "invalid2", "valid1", "valid2"));
		
		ArrayList<String> inputSrcList = new ArrayList<>();
		inputSrcList.add("one");
		inputSrcList.add("two");
		_sourceQr.setInputSourceList(inputSrcList);
		
		_sourceVQG = new ValidatedQueryGenes();
		
		TreeSet<String> invalid = new TreeSet<>();
		invalid.add("invalid1");
		invalid.add("invalid2");
		_sourceVQG.setInvalid(invalid);
		
		TreeSet<String> query = new TreeSet<>();
		query.add("valid1");
		query.add("valid2");
		query.add("invalid1");
		query.add("invalid2");
		_sourceVQG.setQueryGenes(query);
		
		HashMap<String, String> normGenes = new HashMap<>();
		normGenes.put("valid1", "valid1");
		normGenes.put("valid2", "validtwo");
		_sourceVQG.setNormalizedGenes(normGenes);
		
		_sourceQr.setValidatedGenes(_sourceVQG);
		SourceQueryResults sqrOne = new SourceQueryResults();
		sqrOne.setMessage("sqrmessage");
		sqrOne.setNumberOfHits(10);
		sqrOne.setProgress(20);
		sqrOne.setSourceName("sqrOne");
		sqrOne.setSourceRank(30);
		sqrOne.setSourceTaskId("sqrOneTaskId");
		sqrOne.setSourceUUID(UUID.randomUUID());
		sqrOne.setStatus("sqrOneStatus");
		sqrOne.setWallTime(40);
		
		SourceQueryResult sOne = new SourceQueryResult();
		sOne.setDescription("sOne");
		sOne.setEdges(100);
		sOne.setImageURL("sOneImageUrl");
		sOne.setLegendURL("sOneLegendUrl");
		sOne.setNetworkUUID("sOneNetworkUUID");
		sOne.setNodes(200);
		sOne.setPercentOverlap(26);
		sOne.setRank(42);
		sOne.setTotalGeneCount(99);
		sOne.setUrl("sOneUrl");
		TreeSet<String> hitGenes = new TreeSet<>();
		hitGenes.add("valid1");
		sOne.setHitGenes(hitGenes);
		HashMap<String, Object> details = new HashMap<>();
		details.put("pvalue", (Object)new String("1"));
		sOne.setDetails(details);
		sqrOne.setResults(Arrays.asList(sOne));
		_sourceQr.setSources(Arrays.asList(sqrOne));
	}
	
	@After
	public void tearDown() {
	}

	@Test
	public void testCopyNull(){
		assertNull(QueryResultsCopier.copy(null));
	}
	protected void testCopyHelper(QueryResults copyOne, boolean resultsOmitted){
		
		assertEquals(copyOne.getMessage(), _sourceQr.getMessage());
		assertEquals(copyOne.getNumberOfHits(), _sourceQr.getNumberOfHits());
		assertEquals(copyOne.getProgress(), _sourceQr.getProgress());
		assertEquals(copyOne.getQuery().size(), _sourceQr.getQuery().size());
		assertTrue(copyOne.getQuery().containsAll(_sourceQr.getQuery()));
		assertEquals(copyOne.getSize(), _sourceQr.getSize());
		assertEquals(copyOne.getSource(), _sourceQr.getSource());
		assertEquals(copyOne.getStart(), _sourceQr.getStart());
		assertEquals(copyOne.getStartTime(), _sourceQr.getStartTime());
		assertEquals(copyOne.getStatus(), _sourceQr.getStatus());
		assertEquals(copyOne.getWallTime(), _sourceQr.getWallTime());
		assertEquals(copyOne.getValidatedGenes().getInvalid().size(), _sourceQr.getValidatedGenes().getInvalid().size());
		assertTrue(copyOne.getValidatedGenes().getInvalid().containsAll(_sourceQr.getValidatedGenes().getInvalid()));
		assertEquals(copyOne.getInputSourceList().size(), _sourceQr.getInputSourceList().size());
		assertTrue(copyOne.getInputSourceList().containsAll(_sourceQr.getInputSourceList()));
		assertEquals(copyOne.getSources().size(), _sourceQr.getSources().size());
		for (int i = 0 ; i < copyOne.getSources().size(); i++){
			SourceQueryResults copySqr = copyOne.getSources().get(i);
			SourceQueryResults sourceSqr = _sourceQr.getSources().get(i);
			assertEquals(copySqr.getMessage(),sourceSqr.getMessage());
			assertEquals(copySqr.getNumberOfHits(), sourceSqr.getNumberOfHits());
			assertEquals(copySqr.getProgress(), sourceSqr.getProgress());
			assertEquals(copySqr.getSourceName(), sourceSqr.getSourceName());
			assertEquals(copySqr.getSourceRank(), sourceSqr.getSourceRank());
			assertEquals(copySqr.getSourceTaskId(), sourceSqr.getSourceTaskId());
			assertEquals(copySqr.getSourceUUID(), sourceSqr.getSourceUUID());
			assertEquals(copySqr.getStatus(), sourceSqr.getStatus());
			assertEquals(copySqr.getWallTime(), sourceSqr.getWallTime());
			if (resultsOmitted == true){
				assertNull(copySqr.getResults());
			} else {
				assertEquals(copySqr.getResults().size(), sourceSqr.getResults().size());

				for (int j = 0 ; j < copySqr.getResults().size(); j++){
					SourceQueryResult copySq = copySqr.getResults().get(j);
					SourceQueryResult sourceSq = sourceSqr.getResults().get(j);
					assertEquals(copySq.getDescription(),sourceSq.getDescription());
					assertEquals(copySq.getEdges(), sourceSq.getEdges());
					assertEquals(copySq.getHitGenes().size(), sourceSq.getHitGenes().size());
					assertTrue(copySq.getHitGenes().containsAll(sourceSq.getHitGenes()));
					assertEquals(copySq.getImageURL(), sourceSq.getImageURL());
					assertEquals(copySq.getLegendURL(), sourceSq.getLegendURL());
					assertEquals(copySq.getNetworkUUID(), sourceSq.getNetworkUUID());
					assertEquals(copySq.getNodes(), sourceSq.getNodes());
					assertEquals(copySq.getPercentOverlap(), sourceSq.getPercentOverlap());
					assertEquals(copySq.getRank(), sourceSq.getRank());
					assertEquals(copySq.getTotalGeneCount(), sourceSq.getTotalGeneCount());
					assertEquals(copySq.getUrl(), sourceSq.getUrl());
					assertEquals(copySq.getDetails().size(), sourceSq.getDetails().size());
					assertTrue(copySq.getDetails().keySet().containsAll(sourceSq.getDetails().keySet()));
					assertTrue(copySq.getDetails().entrySet().containsAll(sourceSq.getDetails().entrySet()));			
				}
			}
		}
	
	}
	@Test
	public void testCopy(){
		QueryResults copyOne = QueryResultsCopier.copy(_sourceQr);
		testCopyHelper(copyOne, false);
	}
	
	@Test
	public void testCopyNoSourceResults(){
		QueryResults copyOne = QueryResultsCopier.copyNoSourceResults(_sourceQr);
		testCopyHelper(copyOne, true);
	}
	
}
