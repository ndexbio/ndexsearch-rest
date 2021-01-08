package org.ndexbio.ndexsearch.rest.engine;


import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
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



/**
 *
 * @author churas
 */
public class TestInteractomeSourceEngine {

	@Rule
	public TemporaryFolder _folder = new TemporaryFolder();
	
	@Test
	public void testgetSourceQueryResultsQueryThrowsNdexException() throws NdexException {
		InteractomeRestClient mockClient = mock(InteractomeRestClient.class);
	
		when(mockClient.search(any(List.class))).thenThrow(new NdexException("error"));
		InteractomeSourceEngine engine = new InteractomeSourceEngine("sourceName", mockClient, 1);
		Query query = new Query();
		query.setGeneList(Arrays.asList("gene1"));
		SourceQueryResults sqr = engine.getSourceQueryResults(query);
		assertEquals("sourceName failed : error", sqr.getMessage());
		assertEquals(100, sqr.getProgress());
		assertEquals(QueryResults.FAILED_STATUS, sqr.getStatus());
		verify(mockClient).search(any(List.class));
	}


	@Test
	public void testgetSourceQueryResultsQueryReturnsNull() throws NdexException {
		InteractomeRestClient mockClient = mock(InteractomeRestClient.class);
		when(mockClient.search(any(List.class))).thenReturn(null);
		InteractomeSourceEngine engine = new InteractomeSourceEngine("sourceName", mockClient, 1);
		Query query = new Query();
		query.setGeneList(Arrays.asList("gene1"));
		SourceQueryResults sqr = engine.getSourceQueryResults(query);
		assertEquals("sourceName failed for unknown reason", sqr.getMessage());
		assertEquals(100, sqr.getProgress());
		assertEquals(QueryResults.FAILED_STATUS, sqr.getStatus());
		verify(mockClient).search(any(List.class));
	}
	
	@Test
	public void testgetSourceQueryResultsSuccess() throws NdexException {
		InteractomeRestClient mockClient = mock(InteractomeRestClient.class);
		UUID resUUID = UUID.randomUUID();
		when(mockClient.search(any(List.class))).thenReturn(resUUID);
		InteractomeSourceEngine engine = new InteractomeSourceEngine("sourceName", mockClient, 1);
		Query query = new Query();
		query.setGeneList(Arrays.asList("gene1"));
		SourceQueryResults sqr = engine.getSourceQueryResults(query);
		assertEquals(null, sqr.getMessage());
		assertEquals(0, sqr.getProgress());
		assertEquals(QueryResults.SUBMITTED_STATUS, sqr.getStatus());
		assertEquals(resUUID.toString(), sqr.getSourceTaskId());
		verify(mockClient).search(any(List.class));
	}
	
	@Test
	public void testUpdateSourceResultWhereClientThrowsException() throws NdexException {
		InteractomeRestClient mockClient = mock(InteractomeRestClient.class);
		when(mockClient.getDatabase()).thenThrow(new NdexException("error"));
		InteractomeSourceEngine engine = new InteractomeSourceEngine("sourceName", mockClient, 1);
		SourceResult sRes = new SourceResult();
		sRes.setStatus("ok");
		sRes.setNumberOfNetworks(10);
		engine.updateSourceResult(sRes);
		assertEquals("error", sRes.getStatus());
		assertEquals(10, sRes.getNumberOfNetworks());
		verify(mockClient).getDatabase();
	}
	
	@Test
	public void testUpdateSourceResultNullResults() throws NdexException {
		InteractomeRestClient mockClient = mock(InteractomeRestClient.class);
		when(mockClient.getDatabase()).thenReturn(null);
		InteractomeSourceEngine engine = new InteractomeSourceEngine("sourceName", mockClient, 1);
		SourceResult sRes = new SourceResult();
		
		engine.updateSourceResult(sRes);
		assertNull(sRes.getDatabases());
		assertEquals("error", sRes.getStatus());
		assertEquals("1.0", sRes.getVersion());
		assertEquals(0, sRes.getNumberOfNetworks());
		verify(mockClient).getDatabase();
	}
	
	@Test
	public void testUpdateSourceResultSuccess() throws NdexException {
		InteractomeRestClient mockClient = mock(InteractomeRestClient.class);
		InteractomeRefNetworkEntry e1 = new InteractomeRefNetworkEntry();
		InteractomeRefNetworkEntry e2 = new InteractomeRefNetworkEntry();
		
		when(mockClient.getDatabase()).thenReturn(Arrays.asList(e1, e2));
		InteractomeSourceEngine engine = new InteractomeSourceEngine("sourceName", mockClient, 1);
		SourceResult sRes = new SourceResult();
		
		engine.updateSourceResult(sRes);
		assertNull(sRes.getDatabases());
		assertEquals("ok", sRes.getStatus());
		assertEquals("1.0", sRes.getVersion());
		assertEquals(2, sRes.getNumberOfNetworks());
		verify(mockClient).getDatabase();
	}
	
	@Test
	public void testShutdown() throws NdexException {
		InteractomeSourceEngine engine = new InteractomeSourceEngine("sourceName", null, 1);
		engine.shutdown();
	}

	@Test
	public void testGetOverlaidNetworksAsCXStreamSuccess() throws NdexException, SearchException {
		InteractomeRestClient mockClient = mock(InteractomeRestClient.class);
		ByteArrayInputStream bis = new ByteArrayInputStream(new byte[5]);
		UUID idUUID = UUID.randomUUID();
		UUID netUUID = UUID.randomUUID();
		InteractomeSourceEngine engine = new InteractomeSourceEngine("sourceName", mockClient, 1);
		when(mockClient.getOverlaidNetworkStream(any(UUID.class), any(UUID.class))).thenReturn(bis);
		InputStream is = engine.getOverlaidNetworkAsCXStream(idUUID.toString(), netUUID.toString());
		assertNotNull(is);
		
		verify(mockClient).getOverlaidNetworkStream(idUUID, netUUID);
	}
	
	@Test
	public void testUpdateSourceQueryResultsSearchStatusThrowsException() throws NdexException {
		InteractomeRestClient mockClient = mock(InteractomeRestClient.class);
		
		when(mockClient.getSearchStatus(any(UUID.class))).thenThrow(new NdexException("error"));
		
		InteractomeSourceEngine engine = new InteractomeSourceEngine("sourceName", mockClient, 1);
		
		SourceQueryResults sqRes = new SourceQueryResults();
		UUID taskUUID = UUID.randomUUID();
		sqRes.setSourceTaskId(taskUUID.toString());
		engine.updateSourceQueryResults(sqRes);
		
		assertEquals(null, sqRes.getMessage());
		assertEquals(0, sqRes.getProgress());
		assertEquals(null, sqRes.getStatus());
		assertEquals(0, sqRes.getNumberOfHits());
		assertEquals(null, sqRes.getResults());
		assertEquals(0, sqRes.getWallTime());
		verify(mockClient).getSearchStatus(taskUUID);
	}
	
	@Test
	public void testUpdateSourceQueryResultsCompleteWhereSearchResultsThrowsException() throws NdexException {
		InteractomeRestClient mockClient = mock(InteractomeRestClient.class);
		
		SearchStatus searchStatus = new SearchStatus();
		searchStatus.setMessage("message");
		searchStatus.setProgress(100);
		searchStatus.setStatus(SearchStatus.complete);
		searchStatus.setWallTime(2);
		
		when(mockClient.getSearchStatus(any(UUID.class))).thenReturn(searchStatus);
		when(mockClient.getSearchResult(any(UUID.class))).thenThrow(new NdexException("This search has no result ready. "
							+ "Search status: processing"));
		
		InteractomeSourceEngine engine = new InteractomeSourceEngine("sourceName", mockClient, 1);
		
		SourceQueryResults sqRes = new SourceQueryResults();
		UUID taskUUID = UUID.randomUUID();
		sqRes.setSourceTaskId(taskUUID.toString());
		engine.updateSourceQueryResults(sqRes);
		
		assertEquals(searchStatus.getMessage(), sqRes.getMessage());
		assertEquals(searchStatus.getProgress(), sqRes.getProgress());
		assertEquals(searchStatus.getStatus(), sqRes.getStatus());
		assertEquals(0, sqRes.getNumberOfHits());
		assertEquals(null, sqRes.getResults());
		assertEquals(searchStatus.getWallTime(), sqRes.getWallTime());

		verify(mockClient).getSearchStatus(taskUUID);
		verify(mockClient).getSearchResult(taskUUID);
	}
	
	@Test
	public void testUpdateSourceQueryResultsCompleteWhereResultsIsNull() throws NdexException {
		InteractomeRestClient mockClient = mock(InteractomeRestClient.class);
		
		SearchStatus searchStatus = new SearchStatus();
		searchStatus.setMessage("message");
		searchStatus.setProgress(100);
		searchStatus.setStatus(SearchStatus.complete);
		searchStatus.setWallTime(2);
		
		when(mockClient.getSearchStatus(any(UUID.class))).thenReturn(searchStatus);
		when(mockClient.getSearchResult(any(UUID.class))).thenReturn(null);
		
		InteractomeSourceEngine engine = new InteractomeSourceEngine("sourceName", mockClient, 1);
		
		SourceQueryResults sqRes = new SourceQueryResults();
		UUID taskUUID = UUID.randomUUID();
		sqRes.setSourceTaskId(taskUUID.toString());
		engine.updateSourceQueryResults(sqRes);
		
		assertEquals(searchStatus.getMessage(), sqRes.getMessage());
		assertEquals(searchStatus.getProgress(), sqRes.getProgress());
		assertEquals(searchStatus.getStatus(), sqRes.getStatus());
		assertEquals(0, sqRes.getNumberOfHits());
		assertEquals(0, sqRes.getResults().size());
		assertEquals(searchStatus.getWallTime(), sqRes.getWallTime());

		verify(mockClient).getSearchStatus(taskUUID);
		verify(mockClient).getSearchResult(taskUUID);
	}
	
	@Test
	public void testUpdateSourceQueryResultsInComplete() throws NdexException {
		InteractomeRestClient mockClient = mock(InteractomeRestClient.class);
		
		SearchStatus searchStatus = new SearchStatus();
		searchStatus.setMessage("message");
		searchStatus.setProgress(50);
		searchStatus.setStatus(SearchStatus.processing);
		searchStatus.setWallTime(2);
		
		when(mockClient.getSearchStatus(any(UUID.class))).thenReturn(searchStatus);
		
		InteractomeSourceEngine engine = new InteractomeSourceEngine("sourceName", mockClient, 1);
		
		SourceQueryResults sqRes = new SourceQueryResults();
		UUID taskUUID = UUID.randomUUID();
		sqRes.setSourceTaskId(taskUUID.toString());
		engine.updateSourceQueryResults(sqRes);
		
		assertEquals(searchStatus.getMessage(), sqRes.getMessage());
		assertEquals(searchStatus.getProgress(), sqRes.getProgress());
		assertEquals(searchStatus.getStatus(), sqRes.getStatus());
		assertEquals(0, sqRes.getNumberOfHits());
		assertEquals(0, sqRes.getResults().size());
		assertEquals(searchStatus.getWallTime(), sqRes.getWallTime());
		verify(mockClient).getSearchStatus(taskUUID);
	}
	
	@Test
	public void testUpdateSourceQueryResultsCompleteWithResults() throws NdexException {
		InteractomeRestClient mockClient = mock(InteractomeRestClient.class);
		
		SearchStatus searchStatus = new SearchStatus();
		searchStatus.setMessage("message");
		searchStatus.setProgress(100);
		searchStatus.setStatus(SearchStatus.complete);
		searchStatus.setWallTime(2);
		
		when(mockClient.getSearchStatus(any(UUID.class))).thenReturn(searchStatus);
		
		InteractomeSearchResult isrOne = new InteractomeSearchResult();
		isrOne.setDescription("description1");
		isrOne.setEdgeCount(1);
		Set<String> hitGenesOne = new TreeSet<>();
		hitGenesOne.add("g1");
		isrOne.setHitGenes(hitGenesOne);
		isrOne.setNetworkUUID("netid");
		isrOne.setNodeCount(2);
		isrOne.setPercentOverlap(3);
		isrOne.setImageURL("imageurl1");
		isrOne.setRank(4);
		isrOne.setDetails(null);
		
		InteractomeSearchResult isrTwo = new InteractomeSearchResult();
		isrOne.setDescription("description2");
		isrOne.setEdgeCount(5);
		Set<String> hitGenesTwo = new TreeSet<>();
		hitGenesOne.add("g2");
		isrOne.setHitGenes(hitGenesTwo);
		isrOne.setNetworkUUID("netid2");
		isrOne.setNodeCount(6);
		isrOne.setPercentOverlap(7);
		isrOne.setImageURL("imageurl2");
		isrOne.setRank(8);
		isrOne.setDetails(new HashMap<>());
		
		when(mockClient.getSearchResult(any(UUID.class))).thenReturn(Arrays.asList(isrOne, isrTwo));
		
		InteractomeSourceEngine engine = new InteractomeSourceEngine("sourceName", mockClient, 1);
		
		SourceQueryResults sqRes = new SourceQueryResults();
		UUID taskUUID = UUID.randomUUID();
		sqRes.setSourceTaskId(taskUUID.toString());
		engine.updateSourceQueryResults(sqRes);
		
		assertEquals(searchStatus.getMessage(), sqRes.getMessage());
		assertEquals(searchStatus.getProgress(), sqRes.getProgress());
		assertEquals(searchStatus.getStatus(), sqRes.getStatus());
		assertEquals(2, sqRes.getNumberOfHits());
		assertEquals(2, sqRes.getResults().size());
		assertEquals(searchStatus.getWallTime(), sqRes.getWallTime());

		SourceQueryResult resOne = sqRes.getResults().get(0);
		assertEquals(isrOne.getDescription(), resOne.getDescription());
		assertEquals(isrOne.getDetails(), resOne.getDetails());
		assertEquals(isrOne.getEdgeCount(), resOne.getEdges());
		assertEquals(isrOne.getHitGenes(), resOne.getHitGenes());
		assertEquals(isrOne.getImageURL(), resOne.getImageURL());
		assertEquals(isrOne.getNetworkUUID(), resOne.getNetworkUUID());
		assertEquals(isrOne.getNodeCount(), resOne.getNodes());
		assertEquals(isrOne.getPercentOverlap(), resOne.getPercentOverlap());
		assertEquals(isrOne.getRank(), resOne.getRank());
		
		SourceQueryResult resTwo = sqRes.getResults().get(1);
		assertEquals(isrTwo.getDescription(), resTwo.getDescription());
		assertEquals(isrTwo.getDetails(), resTwo.getDetails());
		assertEquals(isrTwo.getEdgeCount(), resTwo.getEdges());
		assertEquals(isrTwo.getHitGenes(), resTwo.getHitGenes());
		assertEquals(isrTwo.getImageURL(), resTwo.getImageURL());
		assertEquals(isrTwo.getNetworkUUID(), resTwo.getNetworkUUID());
		assertEquals(isrTwo.getNodeCount(), resTwo.getNodes());
		assertEquals(isrTwo.getPercentOverlap(), resTwo.getPercentOverlap());
		assertEquals(isrTwo.getRank(), resTwo.getRank());
		
		verify(mockClient).getSearchStatus(taskUUID);
		verify(mockClient).getSearchResult(taskUUID);
	}
	
}
