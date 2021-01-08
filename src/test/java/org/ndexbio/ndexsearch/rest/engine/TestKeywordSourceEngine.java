package org.ndexbio.ndexsearch.rest.engine;


import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.UUID;
import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
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



/**
 *
 * @author churas
 */
public class TestKeywordSourceEngine {

	@Rule
	public TemporaryFolder _folder = new TemporaryFolder();
	
	@Test
	public void testGetSourceQueryResultsQueryThrowsNdexException() throws Exception {
		NdexRestClientModelAccessLayer mockClient = mock(NdexRestClientModelAccessLayer.class);
	
		when(mockClient.findNetworks(any(String.class), eq(null), eq(0), eq(100))).thenThrow(new NdexException("error"));
		
		KeywordSourceEngine engine = new KeywordSourceEngine(mockClient, "srcTaskid", "imageurl", 1);
		Query query = new Query();
		query.setGeneList(Arrays.asList("gene1"));
		SourceQueryResults sqr = engine.getSourceQueryResults(query);
		assertEquals(SourceResult.KEYWORD_SERVICE + " failed : error", sqr.getMessage());
		assertEquals(100, sqr.getProgress());
		assertEquals(QueryResults.FAILED_STATUS, sqr.getStatus());
		verify(mockClient).findNetworks(" gene1", null, 0, 100);
	}
	
	@Test
	public void testGetSourceQueryResultsQueryFindNetworksReturnsNull() throws Exception {
		NdexRestClientModelAccessLayer mockClient = mock(NdexRestClientModelAccessLayer.class);
	
		when(mockClient.findNetworks(any(String.class), eq(null), eq(0), eq(100))).thenReturn(null);
		
		KeywordSourceEngine engine = new KeywordSourceEngine(mockClient, "srcTaskid", "imageurl", 1);
		Query query = new Query();
		query.setGeneList(Arrays.asList("gene1"));
		SourceQueryResults sqr = engine.getSourceQueryResults(query);
		assertEquals("failed for unknown reason", sqr.getMessage());
		assertEquals(100, sqr.getProgress());
		assertEquals(QueryResults.FAILED_STATUS, sqr.getStatus());
		verify(mockClient).findNetworks(" gene1", null, 0, 100);
	}
	
	@Test
	public void testGetSourceQueryResultsNetworksInNRSIsNull() throws Exception {
		NdexRestClientModelAccessLayer mockClient = mock(NdexRestClientModelAccessLayer.class);
	
		NetworkSearchResult nrs = new NetworkSearchResult();
		nrs.setNetworks(null);
		when(mockClient.findNetworks(any(String.class), eq(null), eq(0), eq(100))).thenReturn(nrs);
		
		KeywordSourceEngine engine = new KeywordSourceEngine(mockClient, "srcTaskid", "imageurl", 1);
		Query query = new Query();
		query.setGeneList(Arrays.asList("gene1", "gene2", "gene3"));
		SourceQueryResults sqr = engine.getSourceQueryResults(query);
		assertEquals(null, sqr.getMessage());
		assertEquals(100, sqr.getProgress());
		assertEquals(QueryResults.COMPLETE_STATUS, sqr.getStatus());
		
		assertEquals(0, sqr.getResults().size());
		
		verify(mockClient).findNetworks(" gene1 gene2 gene3", null, 0, 100);
	}
	
	@Test
	public void testGetSourceQueryResultsSuccess() throws Exception {
		NdexRestClientModelAccessLayer mockClient = mock(NdexRestClientModelAccessLayer.class);
	
		NetworkSearchResult nrs = new NetworkSearchResult();
		NetworkSummary nsOne = new NetworkSummary();
		nsOne.setName("name1");
		nsOne.setEdgeCount(1);
		nsOne.setNodeCount(2);
		UUID externalIdOne = UUID.randomUUID();
		nsOne.setExternalId(externalIdOne);
		
		NetworkSummary nsTwo = new NetworkSummary();
		nsTwo.setName("name2");
		nsTwo.setEdgeCount(3);
		nsTwo.setNodeCount(4);
		UUID externalIdTwo = UUID.randomUUID();
		nsTwo.setExternalId(externalIdTwo);
		
		nrs.setNetworks(Arrays.asList(nsOne, nsTwo));
		
		when(mockClient.findNetworks(any(String.class), eq(null), eq(0), eq(100))).thenReturn(nrs);
		
		KeywordSourceEngine engine = new KeywordSourceEngine(mockClient, "srcTaskid", "imageurl", 1);
		Query query = new Query();
		query.setGeneList(Arrays.asList("gene1", "gene2"));
		SourceQueryResults sqr = engine.getSourceQueryResults(query);
		assertEquals(null, sqr.getMessage());
		assertEquals(100, sqr.getProgress());
		assertEquals(QueryResults.COMPLETE_STATUS, sqr.getStatus());
		
		SourceQueryResult two = sqr.getResults().get(1);
		assertEquals(nsTwo.getName(), two.getDescription());
		assertEquals(nsTwo.getEdgeCount(), two.getEdges());
		assertEquals(nsTwo.getNodeCount(), two.getNodes());
		assertEquals(nsTwo.getExternalId().toString(), two.getNetworkUUID());
		assertEquals(0, two.getPercentOverlap());
		assertEquals(1, two.getRank());
		assertEquals("imageurl", two.getImageURL());
		
		verify(mockClient).findNetworks(" gene1 gene2", null, 0, 100);
	}

	@Test
	public void testGetOverlaidNetworksAsCXStreamThrowsException() throws Exception {
		NdexRestClientModelAccessLayer mockClient = mock(NdexRestClientModelAccessLayer.class);
		KeywordSourceEngine engine = new KeywordSourceEngine(mockClient, "srcTaskid", "imageurl", 1);
		
		UUID idUUID = UUID.randomUUID();
		UUID netUUID = UUID.randomUUID();
		when(mockClient.getNetworkAsCXStream(any(UUID.class))).thenThrow(new NdexException("error"));
		try {
			engine.getOverlaidNetworkAsCXStream(idUUID.toString(), netUUID.toString());
			fail("Expected SearchException");
		} catch(SearchException se){
			assertEquals("Unable to get network: error", se.getMessage());
		}
		
		verify(mockClient).getNetworkAsCXStream(netUUID);
	}
	
	@Test
	public void testUpdateSourceResultThrowsException() throws Exception {
		NdexRestClientModelAccessLayer mockClient = mock(NdexRestClientModelAccessLayer.class);
		when(mockClient.getServerStatus()).thenThrow(new NdexException("error"));

		KeywordSourceEngine engine = new KeywordSourceEngine(mockClient, "srcTaskid", "imageurl", 1);
		SourceResult sRes = new SourceResult();
		engine.updateSourceResult(sRes);
		assertEquals("error", sRes.getStatus());
	}
	
	@Test
	public void testUpdateSourceResultMessageIsNotNullAndDoesNotContainOnline() throws Exception {
		NdexRestClientModelAccessLayer mockClient = mock(NdexRestClientModelAccessLayer.class);
		NdexStatus status = new NdexStatus();
		status.setMessage("some error");
		status.setProperties(null);
		when(mockClient.getServerStatus()).thenReturn(status);

		KeywordSourceEngine engine = new KeywordSourceEngine(mockClient, "srcTaskid", "imageurl", 1);
		SourceResult sRes = new SourceResult();
		engine.updateSourceResult(sRes);
		assertEquals(0, sRes.getNumberOfNetworks());
		assertEquals("error", sRes.getStatus());
		// since properties is unset
		assertEquals("unknown", sRes.getVersion());
	}
	
	@Test
	public void testUpdateSourceResultSuccess() throws Exception {
		NdexRestClientModelAccessLayer mockClient = mock(NdexRestClientModelAccessLayer.class);
		NdexStatus status = new NdexStatus();
		status.setMessage("Online");
		status.getProperties().put("ServerVersion", "2.5.0");
		status.setNetworkCount(500);
		when(mockClient.getServerStatus()).thenReturn(status);

		KeywordSourceEngine engine = new KeywordSourceEngine(mockClient, "srcTaskid", "imageurl", 1);
		SourceResult sRes = new SourceResult();
		engine.updateSourceResult(sRes);
		
		assertEquals(500, sRes.getNumberOfNetworks());
		assertEquals("ok", sRes.getStatus());
		assertEquals("2.5.0", sRes.getVersion());
	}
	
	@Test
	public void testGetOverlaidNetworksAsCXStreamSuccess() throws Exception {
		NdexRestClientModelAccessLayer mockClient = mock(NdexRestClientModelAccessLayer.class);
		KeywordSourceEngine engine = new KeywordSourceEngine(mockClient, "srcTaskid", "imageurl", 1);
		
		ByteArrayInputStream bis = new ByteArrayInputStream(new byte[5]);
		UUID idUUID = UUID.randomUUID();
		UUID netUUID = UUID.randomUUID();
		when(mockClient.getNetworkAsCXStream(any(UUID.class))).thenReturn(bis);
		InputStream is = engine.getOverlaidNetworkAsCXStream(idUUID.toString(), netUUID.toString());
		assertNotNull(is);
		
		verify(mockClient).getNetworkAsCXStream(netUUID);
	}
	
	@Test
	public void testGetDatabases() throws Exception{
		//this always returns null
		NdexRestClientModelAccessLayer mockClient = mock(NdexRestClientModelAccessLayer.class);
		KeywordSourceEngine engine = new KeywordSourceEngine(mockClient, "srcTaskid", "imageurl", 1);
		assertNull(engine.getDatabases());
		verifyNoInteractions(mockClient);
	}
	
	@Test
	public void testDelete() throws Exception{
		//delete is a no op
		NdexRestClientModelAccessLayer mockClient = mock(NdexRestClientModelAccessLayer.class);
		KeywordSourceEngine engine = new KeywordSourceEngine(mockClient, "srcTaskid", "imageurl", 1);
		engine.delete(null);
		verifyNoInteractions(mockClient);
	}
	
	@Test
	public void testShutdown() throws Exception{
		//shutdown is a no op
		NdexRestClientModelAccessLayer mockClient = mock(NdexRestClientModelAccessLayer.class);
		KeywordSourceEngine engine = new KeywordSourceEngine(mockClient, "srcTaskid", "imageurl", 1);
		engine.shutdown();
		verifyNoInteractions(mockClient);
	}
	
	@Test
	public void testUpdateSourceQueryResults() throws Exception{
		//shutdown is a no op
		NdexRestClientModelAccessLayer mockClient = mock(NdexRestClientModelAccessLayer.class);
		KeywordSourceEngine engine = new KeywordSourceEngine(mockClient, "srcTaskid", "imageurl", 1);
		engine.updateSourceQueryResults(new SourceQueryResults());
		verifyNoInteractions(mockClient);
	}
}
