package org.ndexbio.ndexsearch.rest.engine;


import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;
import org.checkerframework.checker.units.qual.s;
import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
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



/**
 *
 * @author churas
 */
public class TestEnrichmentSourceEngine {

	@Rule
	public TemporaryFolder _folder = new TemporaryFolder();
	
	@Test
	public void testgetSourceQueryResultsQueryThrowsException() throws EnrichmentException {
		EnrichmentRestClient mockClient = mock(EnrichmentRestClient.class);
		when(mockClient.query(any(EnrichmentQuery.class))).thenThrow(new EnrichmentException("error"));
		EnrichmentSourceEngine engine = new EnrichmentSourceEngine(mockClient);
		Query query = new Query();
		query.setGeneList(Arrays.asList("gene1"));
		SourceQueryResults sqr = engine.getSourceQueryResults(query);
		assertEquals("Enrichment failed: error", sqr.getMessage());
		assertEquals(100, sqr.getProgress());
		assertEquals(QueryResults.FAILED_STATUS, sqr.getStatus());
		verify(mockClient).query(any(EnrichmentQuery.class));
	}

	@Test
	public void testgetSourceQueryResultsQueryReturnsNull() throws EnrichmentException {
		EnrichmentRestClient mockClient = mock(EnrichmentRestClient.class);
		when(mockClient.query(any(EnrichmentQuery.class))).thenReturn(null);
		EnrichmentSourceEngine engine = new EnrichmentSourceEngine(mockClient);
		Query query = new Query();
		query.setGeneList(Arrays.asList("gene1"));
		SourceQueryResults sqr = engine.getSourceQueryResults(query);
		assertEquals("Enrichment failed for unknown reason", sqr.getMessage());
		assertEquals(100, sqr.getProgress());
		assertEquals(QueryResults.FAILED_STATUS, sqr.getStatus());
		verify(mockClient).query(any(EnrichmentQuery.class));
	}
	
	@Test
	public void testgetSourceQueryResultsSuccess() throws EnrichmentException{
		EnrichmentRestClient mockClient = mock(EnrichmentRestClient.class);
		when(mockClient.query(any(EnrichmentQuery.class))).thenReturn("taskid");
		EnrichmentSourceEngine engine = new EnrichmentSourceEngine(mockClient);
		Query query = new Query();
		query.setGeneList(Arrays.asList("gene1"));
		SourceQueryResults sqr = engine.getSourceQueryResults(query);
		assertEquals(null, sqr.getMessage());
		assertEquals(0, sqr.getProgress());
		assertEquals(QueryResults.SUBMITTED_STATUS, sqr.getStatus());
		assertEquals("taskid", sqr.getSourceTaskId());
		verify(mockClient).query(any(EnrichmentQuery.class));
	}
	
	@Test
	public void testUpdateSourceResultWhereClientThrowsException() throws EnrichmentException {
		EnrichmentRestClient mockClient = mock(EnrichmentRestClient.class);
		when(mockClient.getDatabaseResults()).thenThrow(new EnrichmentException("error"));
		EnrichmentSourceEngine engine = new EnrichmentSourceEngine(mockClient);
		SourceResult sRes = new SourceResult();
		sRes.setName("foo");
		DatabaseResult dbOne = new DatabaseResult();
		dbOne.setName("dbOne");
		sRes.setDatabases(Arrays.asList(dbOne));
		engine.updateSourceResult(sRes);
		assertEquals(1, sRes.getDatabases().size());
		assertEquals("dbOne", sRes.getDatabases().get(0).getName());
		verify(mockClient).getDatabaseResults();
	}
	
	@Test
	public void testUpdateSourceResultNoResults() throws EnrichmentException {
		EnrichmentRestClient mockClient = mock(EnrichmentRestClient.class);
		DatabaseResults dbResults = new DatabaseResults();
		dbResults.setResults(null);
		
		when(mockClient.getDatabaseResults()).thenReturn(dbResults);
		EnrichmentSourceEngine engine = new EnrichmentSourceEngine(mockClient);
		SourceResult sRes = new SourceResult();
		sRes.setName("foo");
		DatabaseResult dbOne = new DatabaseResult();
		dbOne.setName("dbOne");
		sRes.setDatabases(Arrays.asList(dbOne));
		engine.updateSourceResult(sRes);
		assertNull(sRes.getDatabases());
		assertEquals("ok", sRes.getStatus());
		assertEquals("0.1.0", sRes.getVersion());
		verify(mockClient).getDatabaseResults();
	}
	
	@Test
	public void testUpdateSourceResultTwoResultsBothWithInvalidNumberOfNetworks() throws EnrichmentException {
		EnrichmentRestClient mockClient = mock(EnrichmentRestClient.class);
		DatabaseResults dbResults = new DatabaseResults();
		DatabaseResult dOne = new DatabaseResult();
		dOne.setNumberOfNetworks("asdf");
		dOne.setName("dOne");
		
		DatabaseResult dTwo = new DatabaseResult();
		dTwo.setName("dTwo");
		
		dbResults.setResults(Arrays.asList(dOne, dTwo));
		
		when(mockClient.getDatabaseResults()).thenReturn(dbResults);
		EnrichmentSourceEngine engine = new EnrichmentSourceEngine(mockClient);
		SourceResult sRes = new SourceResult();
		sRes.setName("foo");
		DatabaseResult dbOne = new DatabaseResult();
		dbOne.setName("dbOne");
		sRes.setDatabases(Arrays.asList(dbOne));
		engine.updateSourceResult(sRes);
		assertEquals("ok", sRes.getStatus());
		assertEquals("0.1.0", sRes.getVersion());
		assertEquals(2, sRes.getDatabases().size());
		assertEquals(0, sRes.getNumberOfNetworks());
		assertEquals("dOne", sRes.getDatabases().get(0).getName());
		assertEquals("dTwo", sRes.getDatabases().get(1).getName());

		verify(mockClient).getDatabaseResults();
	}
	
	@Test
	public void testUpdateSourceResultTwoResultSuccess() throws EnrichmentException {
		EnrichmentRestClient mockClient = mock(EnrichmentRestClient.class);
		DatabaseResults dbResults = new DatabaseResults();
		DatabaseResult dOne = new DatabaseResult();
		dOne.setNumberOfNetworks("4");
		dOne.setName("dOne");
		
		DatabaseResult dTwo = new DatabaseResult();
		dTwo.setNumberOfNetworks("5");
		dTwo.setName("dTwo");
		
		dbResults.setResults(Arrays.asList(dOne, dTwo));
		
		when(mockClient.getDatabaseResults()).thenReturn(dbResults);
		EnrichmentSourceEngine engine = new EnrichmentSourceEngine(mockClient);
		SourceResult sRes = new SourceResult();
		sRes.setName("foo");
		DatabaseResult dbOne = new DatabaseResult();
		dbOne.setName("dbOne");
		sRes.setDatabases(Arrays.asList(dbOne));
		engine.updateSourceResult(sRes);
		assertEquals("ok", sRes.getStatus());
		assertEquals("0.1.0", sRes.getVersion());
		assertEquals(2, sRes.getDatabases().size());
		assertEquals(9, sRes.getNumberOfNetworks());
		assertEquals("dOne", sRes.getDatabases().get(0).getName());
		assertEquals("dTwo", sRes.getDatabases().get(1).getName());

		verify(mockClient).getDatabaseResults();
	}
	
	@Test
	public void testShutdownNullClient() throws EnrichmentException {
		EnrichmentSourceEngine engine = new EnrichmentSourceEngine(null);
		engine.shutdown();
	}
	
	@Test
	public void testShutdownClientThrowsException() throws EnrichmentException {
		EnrichmentRestClient mockClient = mock(EnrichmentRestClient.class);
		doThrow(new EnrichmentException("error")).when(mockClient).shutdown();
		EnrichmentSourceEngine engine = new EnrichmentSourceEngine(mockClient);
		engine.shutdown();
		verify(mockClient).shutdown();
	}
	
	@Test
	public void testShutdownClientSuccess() throws EnrichmentException {
		EnrichmentRestClient mockClient = mock(EnrichmentRestClient.class);
		EnrichmentSourceEngine engine = new EnrichmentSourceEngine(mockClient);
		engine.shutdown();
		verify(mockClient).shutdown();
	}
	
	@Test
	public void testGetDatabasesClientThrowsException() throws EnrichmentException {
		EnrichmentRestClient mockClient = mock(EnrichmentRestClient.class);
		when(mockClient.getDatabaseResults()).thenThrow(new EnrichmentException("error"));
		EnrichmentSourceEngine engine = new EnrichmentSourceEngine(mockClient);
		try {
			engine.getDatabases();
			fail("Expected SearchException");
		} catch(SearchException se){
			assertEquals("caught error trying to get databases: error", se.getMessage());
		}
		verify(mockClient).getDatabaseResults();
	}
	
	@Test
	public void testGetDatabasesSuccess() throws SearchException, EnrichmentException {
		EnrichmentRestClient mockClient = mock(EnrichmentRestClient.class);
		DatabaseResults dbOne = new DatabaseResults();
		when(mockClient.getDatabaseResults()).thenReturn(dbOne);
		EnrichmentSourceEngine engine = new EnrichmentSourceEngine(mockClient);
		assertEquals(dbOne, engine.getDatabases());
		verify(mockClient).getDatabaseResults();
	}
	
	@Test
	public void testDeleteClientThrowsException() throws EnrichmentException {
		EnrichmentRestClient mockClient = mock(EnrichmentRestClient.class);
		doThrow(new EnrichmentException("error")).when(mockClient).delete(any(String.class));
		EnrichmentSourceEngine engine = new EnrichmentSourceEngine(mockClient);
		try {
			engine.delete("123");
			fail("Expected SearchException");
		} catch(SearchException se){
			assertEquals("caught error trying to delete enrichment: error", se.getMessage());
		}
		verify(mockClient).delete("123");
	}
	
	@Test
	public void testDeleteSuccess() throws SearchException, EnrichmentException {
		EnrichmentRestClient mockClient = mock(EnrichmentRestClient.class);
		
		EnrichmentSourceEngine engine = new EnrichmentSourceEngine(mockClient);
		engine.delete("123");
		verify(mockClient).delete("123");
	}
	
	@Test
	public void testGetOverlaidNetworksAsCXStreamClientThrowsException() throws EnrichmentException {
		EnrichmentRestClient mockClient = mock(EnrichmentRestClient.class);
		when(mockClient.getNetworkOverlayAsCX(any(String.class), any(String.class), any(String.class))).thenThrow(new EnrichmentException("error"));
		EnrichmentSourceEngine engine = new EnrichmentSourceEngine(mockClient);
		try {
			engine.getOverlaidNetworkAsCXStream("id", "netid");
			fail("Expected SearchException");
		} catch(SearchException se){
			assertEquals("Unable to get network: error", se.getMessage());
		}
		verify(mockClient).getNetworkOverlayAsCX("id", "", "netid");
	}
	
	@Test
	public void testGetOverlaidNetworksAsCXStreamSuccess() throws EnrichmentException, SearchException {
		EnrichmentRestClient mockClient = mock(EnrichmentRestClient.class);
		ByteArrayInputStream bis = new ByteArrayInputStream(new byte[5]);
		when(mockClient.getNetworkOverlayAsCX(any(String.class), any(String.class), any(String.class))).thenReturn(bis);
		EnrichmentSourceEngine engine = new EnrichmentSourceEngine(mockClient);
		InputStream is = engine.getOverlaidNetworkAsCXStream("id", "netid");
		assertNotNull(is);
		
		verify(mockClient).getNetworkOverlayAsCX("id", "", "netid");
	}
	
	@Test
	public void testUpdateSourceQueryResultsWhereResultsIsNull() throws EnrichmentException, SearchException {
		EnrichmentRestClient mockClient = mock(EnrichmentRestClient.class);
		EnrichmentQueryResults eqr = new EnrichmentQueryResults();
		eqr.setMessage("message");
		eqr.setProgress(50);
		eqr.setStatus("status");
		eqr.setWallTime(1);
		
		when(mockClient.getQueryResults(any(String.class), eq(0), eq(0))).thenReturn(eqr);
		EnrichmentSourceEngine engine = new EnrichmentSourceEngine(mockClient);
		
		SourceQueryResults sqr = new SourceQueryResults();
		sqr.setSourceTaskId("id");
		
		engine.updateSourceQueryResults(sqr);
		assertEquals(eqr.getMessage(), sqr.getMessage());
		assertEquals(eqr.getProgress(), sqr.getProgress());
		assertEquals(eqr.getStatus(), sqr.getStatus());
		assertEquals(0, sqr.getNumberOfHits());
		assertNotNull(sqr.getResults());
		verify(mockClient).getQueryResults("id", 0, 0);
	}
	
	@Test
	public void testUpdateSourceQueryResultsSuccess() throws EnrichmentException, SearchException {
		EnrichmentRestClient mockClient = mock(EnrichmentRestClient.class);
		EnrichmentQueryResults eqr = new EnrichmentQueryResults();
		eqr.setMessage("message");
		eqr.setProgress(50);
		eqr.setStatus("status");
		eqr.setWallTime(1);
		EnrichmentQueryResult eOne = new EnrichmentQueryResult();
		eOne.setDatabaseName("dbname");
		eOne.setDescription("description");
		eOne.setEdges(1);
		Set<String> geneSet = new TreeSet<>();
		geneSet.add("gene1");
		eOne.setHitGenes(geneSet);
		eOne.setNodes(2);
		eOne.setPercentOverlap(3);
		eOne.setRank(4);
		eOne.setNetworkUUID("netid");
		eOne.setImageURL("image");
		eOne.setpValue(5.0);
		eOne.setSimilarity(6.0);
		eOne.setTotalNetworkCount(7);
		eOne.setUrl("url");
		
		eqr.setResults(Arrays.asList(eOne));
		
		when(mockClient.getQueryResults(any(String.class), eq(0), eq(0))).thenReturn(eqr);
		EnrichmentSourceEngine engine = new EnrichmentSourceEngine(mockClient);
		
		SourceQueryResults sqr = new SourceQueryResults();
		sqr.setSourceTaskId("id");
		
		engine.updateSourceQueryResults(sqr);
		assertEquals(eqr.getMessage(), sqr.getMessage());
		assertEquals(eqr.getProgress(), sqr.getProgress());
		assertEquals(eqr.getStatus(), sqr.getStatus());
		assertEquals(1, sqr.getNumberOfHits());
		assertNotNull(sqr.getResults());
		SourceQueryResult sRes = sqr.getResults().get(0);
		assertEquals("dbname: description", sRes.getDescription());
		assertEquals(1, sRes.getEdges());
		assertEquals(1, sRes.getHitGenes().size());
		assertTrue(sRes.getHitGenes().contains("gene1"));
		assertEquals("netid", sRes.getNetworkUUID());
		assertEquals(eOne.getNodes(), sRes.getNodes());
		assertEquals(eOne.getPercentOverlap(), sRes.getPercentOverlap());
		assertEquals(eOne.getRank(), sRes.getRank());
		assertEquals(eOne.getImageURL(), sRes.getImageURL());
		assertEquals(eOne.getUrl(), sRes.getUrl());
		assertEquals(eOne.getpValue(), sRes.getDetails().get("PValue"));
		assertEquals(eOne.getSimilarity(), sRes.getDetails().get("similarity"));
		assertEquals(eOne.getTotalNetworkCount(), sRes.getDetails().get("totalNetworkCount"));
		
		verify(mockClient).getQueryResults("id", 0, 0);
	}
}
