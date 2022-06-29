package org.ndexbio.ndexsearch.rest.engine;


import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.mockito.Mockito.*;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.ndexbio.model.exceptions.NdexException;
import org.ndexbio.ndexsearch.rest.exceptions.SearchException;
import org.ndexbio.ndexsearch.rest.model.Query;
import org.ndexbio.ndexsearch.rest.model.QueryResults;
import org.ndexbio.ndexsearch.rest.model.QueryStatus;
import org.ndexbio.ndexsearch.rest.model.SourceConfiguration;
import org.ndexbio.ndexsearch.rest.model.SourceConfigurations;
import org.ndexbio.ndexsearch.rest.model.SourceQueryResult;
import org.ndexbio.ndexsearch.rest.model.SourceQueryResults;
import org.ndexbio.ndexsearch.rest.model.SourceResult;


/**
 *
 * @author churas
 */
public class TestBasicSearchEngineImpl {

	private File geneSymbolFile;
	
	@Rule
	public TemporaryFolder _folder = new TemporaryFolder();
	

	public Map<String,SourceQueryResults> getSourceQueryResultsFromQueryResults(final QueryResults qr){
		
		Map<String,SourceQueryResults> resHash = new HashMap<>();
		for (SourceQueryResults sqr : qr.getSources()){
			resHash.put(sqr.getSourceName(), sqr);
		}
		return resHash;
	}
	
	@Before 
    public void setupGeneSymbol() throws URISyntaxException {
		geneSymbolFile = new File ( getClass().getClassLoader().getResource("test_genes.tsv").toURI() );
	}
	
	@Test
	public void testConstructor() {
		try {
			BasicSearchEngineImpl engine = new BasicSearchEngineImpl("/dbdir", "/task",
				null, 0, null,geneSymbolFile);
			fail("Expected SearchException");
		} catch(SearchException se){
			assertEquals("Sources cannot be null", se.getMessage());
		}
	}
	
	@Test
	public void testGetQueryResultsFilePath() throws SearchException {
		BasicSearchEngineImpl engine = new BasicSearchEngineImpl("/dbdir", "/task",
				null, 0, new HashMap<String,SourceEngine>(),geneSymbolFile);

		assertEquals("/task/someid/" + BasicSearchEngineImpl.QR_JSON_FILE,
				engine.getQueryResultsFilePath("someid"));
	}
	
	@Test
	public void testThreadSleepAndUpdateThreadSleepTime() throws SearchException {
		BasicSearchEngineImpl engine = new BasicSearchEngineImpl("/dbdir", "/task",
                                                                 null, 0,
				new HashMap<String,SourceEngine>(),geneSymbolFile);
		engine.updateThreadSleepTime(0L);
		engine.threadSleep();
	}
	
	/**
	 * NOTE: This test is relying on the assumption that setting polling interval to 600
	 * seconds mean updateSourceResults() will NOT be called by the service poll executor
	 */
	@Test
	public void testRunWithShutdownAlreadyInvoked() throws SearchException {
		SourceConfigurations sc = new SourceConfigurations();
		sc.setSources(new ArrayList<>());
		BasicSearchEngineImpl engine = new BasicSearchEngineImpl("/dbdir", "/task",
                                                                 sc, 600000,
				new HashMap<String,SourceEngine>(),geneSymbolFile);
		engine.shutdown();
		engine.run();
		
	}
	
	@Test
	public void testGetQueryResultsFromDb() throws SearchException {
		BasicSearchEngineImpl engine = new BasicSearchEngineImpl("/dbdir", "/task",
                                                                 null, 0, new HashMap<String,SourceEngine>(),geneSymbolFile);
		QueryResults qr = engine.getQueryResultsFromDb("someid");
		assertNotNull(qr);
		long startTime = qr.getStartTime();
		assertTrue(startTime > 0);
		qr.setMessage("updated");
		engine.updateQueryResultsInDb("someid", qr);
		QueryResults newQr = engine.getQueryResultsFromDb("someid");
		assertEquals(startTime, newQr.getStartTime());
		assertEquals("updated", newQr.getMessage());
	}
	
	@Test
	public void testGetQueryResultsFromDbOrFilesystem() throws Exception {
		File tempDir = _folder.newFolder();
		try {
			BasicSearchEngineImpl engine = new BasicSearchEngineImpl(tempDir.getAbsolutePath(), 
					tempDir.getAbsolutePath(), null, 0, new HashMap<String,SourceEngine>(),geneSymbolFile);
			File notAFile = new File(engine.getQueryResultsFilePath("someid"));
			assertTrue(notAFile.mkdirs());
			assertNull(engine.getQueryResultsFromDbOrFilesystem("someid"));
			notAFile.delete();
			
			QueryResults qr = engine.getQueryResultsFromDb("someid");
			qr.setMessage("updated");
			engine.updateQueryResultsInDb("someid", qr);
			qr = engine.getQueryResultsFromDbOrFilesystem("someid");
			assertNotNull(qr);
			assertEquals("updated", qr.getMessage());
			
			engine.saveQueryResultsToFilesystem("someid");
			qr = engine.getQueryResultsFromDbOrFilesystem("someid");
			assertNotNull(qr);
			assertEquals("updated", qr.getMessage());
			
			// try putting invalid file on task on filesystem
			File baddata = new File(engine.getQueryResultsFilePath("bad"));
			assertTrue(baddata.getParentFile().mkdirs());
			FileWriter fw = new FileWriter(baddata);
			fw.write("badbad[");
			fw.flush();
			fw.close();
			qr = engine.getQueryResultsFromDbOrFilesystem("bad");
			assertNull(qr);
		} finally {
			_folder.delete();
		}
	}
	
	@Test
	public void testsaveQueryResultsToFilesystem() throws Exception {
		File tempDir = _folder.newFolder();
		try {
			BasicSearchEngineImpl engine = new BasicSearchEngineImpl(tempDir.getAbsolutePath(), 
					tempDir.getAbsolutePath(), null, 0, new HashMap<String,SourceEngine>(),geneSymbolFile);
			
			
			// try putting invalid file on task on filesystem
			File baddata = new File(engine.getQueryResultsFilePath("someid"));
			assertTrue(baddata.mkdirs());
			engine.saveQueryResultsToFilesystem("someid");
			assertTrue(baddata.isDirectory());
		} finally {
			_folder.delete();
		}
	}
	
	@Test
	public void testQueryEmptyOrNullSourceList() throws SearchException, URISyntaxException {
		
		BasicSearchEngineImpl engine = new BasicSearchEngineImpl("/db", 
					"/task", null, 0, new HashMap<String,SourceEngine>(), geneSymbolFile);
		
		try {
			engine.query(null);
			fail("Expected SearchException");
		} catch(SearchException se){
			assertEquals("Query is null", se.getMessage());
		}
		
		// null source list
		try {
			engine.query(new Query());
			fail("Expected SearchException");
		} catch(SearchException se){
			assertEquals("No databases selected", se.getMessage());
		}
		
		// empty source list
		try {
			Query q = new Query();
			q.setSourceList(new ArrayList<>());
			engine.query(q);
			fail("Expected SearchException");
		} catch(SearchException se){
			assertEquals("No databases selected", se.getMessage());
		}
	}
	
	@Test
	public void testQuerySuccess() throws SearchException {
		BasicSearchEngineImpl engine = new BasicSearchEngineImpl("/db", 
					"/task", null, 0, new HashMap<String,SourceEngine>(),geneSymbolFile);
		Query q = new Query();
		q.setSourceList(Arrays.asList("db1"));
		q.setGeneList(Arrays.asList("A2M"));
		String id = engine.query(q);
		assertNotNull(id);
		QueryResults qr = engine.getQueryResultsFromDb(id);
		assertEquals(QueryResults.SUBMITTED_STATUS, qr.getStatus());
		assertTrue(qr.getStartTime() > 0);
		assertEquals("db1", qr.getInputSourceList().get(0));
		assertEquals("A2M", qr.getQuery().get(0));
	}
	
	@Test
	public void testProcessQueryErrorMakingTaskDirectory() throws Exception {
		File tempDir = _folder.newFolder();
		try {
			File taskFile = new File(tempDir.getAbsolutePath() + File.separator + "task");
			assertTrue(taskFile.createNewFile());
			
			BasicSearchEngineImpl engine = new BasicSearchEngineImpl(tempDir.getAbsolutePath(), 
					taskFile.getAbsolutePath(), null, 0, new HashMap<String,SourceEngine>(),geneSymbolFile);
			Query query = new Query();
			query.setGeneList(Arrays.asList("gene1","gene2"));
			query.setSourceList(Arrays.asList("source1"));
			engine.processQuery("queryid", query);
			
			QueryResults res = engine.getQueryResultsFromDbOrFilesystem("queryid");
			assertNotNull(res);
			assertEquals(QueryResults.FAILED_STATUS, res.getStatus());
			assertTrue(res.getStartTime() > 0);
			assertEquals("Internal error unable to create directory on filesystem", res.getMessage());
			assertEquals(100, res.getProgress());
		} finally {
			_folder.delete();
		}
	}
	
	@Test
	public void testProcessQueryInvalidSource() throws Exception {
		File tempDir = _folder.newFolder();
		try {
			File taskDir = new File(tempDir.getAbsolutePath() + File.separator + "task");
			assertTrue(taskDir.mkdirs());
			SourceConfigurations sc = new SourceConfigurations();
		sc.setSources(new ArrayList<>());
			BasicSearchEngineImpl engine = new BasicSearchEngineImpl(tempDir.getAbsolutePath(), 
					taskDir.getAbsolutePath(), sc, 0, new HashMap<String,SourceEngine>(),geneSymbolFile);
			Query query = new Query();
			query.setGeneList(Arrays.asList("gene1","gene2"));
			query.setSourceList(Arrays.asList("source1"));
			engine.processQuery("queryid", query);
			
			QueryResults res = engine.getQueryResultsFromDbOrFilesystem("queryid");
			assertNotNull(res);
			assertEquals(QueryResults.FAILED_STATUS, res.getStatus());
			assertTrue(res.getStartTime() > 0);
			assertEquals("Source source1 is not configured in this server", res.getMessage());
			assertEquals(100, res.getProgress());
		} finally {
			_folder.delete();
		}
	}
	
	@Test
	public void testProcessQuerySuccessSingleSource() throws Exception {
		File tempDir = _folder.newFolder();
		try {
			File taskDir = new File(tempDir.getAbsolutePath() + File.separator + "task");
			assertTrue(taskDir.mkdirs());
			UUID sourceUUID = UUID.randomUUID();
			SourceConfiguration srcConfig = new SourceConfiguration();
			srcConfig.setUuid(sourceUUID.toString());
			srcConfig.setName("source1");
			SourceConfigurations sc = new SourceConfigurations();
			sc.setSources(Arrays.asList(srcConfig));
			
			Map<String,SourceEngine> sourceEngines = new HashMap<>();
			
			SourceEngine mockSrcEngine = mock(SourceEngine.class);
			SourceQueryResults sqRes = new SourceQueryResults();
			sqRes.setMessage("myres");
			sqRes.setStatus(QueryResults.PROCESSING_STATUS);
			when(mockSrcEngine.getSourceQueryResults(any(Query.class))).thenReturn(sqRes);
			
			sourceEngines.put("source1", mockSrcEngine);
			BasicSearchEngineImpl engine = new BasicSearchEngineImpl(tempDir.getAbsolutePath(), 
					taskDir.getAbsolutePath(), sc, 0, sourceEngines,geneSymbolFile);
			Query query = new Query();
			query.setGeneList(Arrays.asList("gene1","gene2"));
			query.setSourceList(Arrays.asList("source1"));
			engine.processQuery("queryid", query);
			
			QueryResults res = engine.getQueryResultsFromDbOrFilesystem("queryid");
			assertNotNull(res);
			assertEquals(QueryResults.PROCESSING_STATUS, res.getStatus());
			assertTrue(res.getStartTime() > 0);
			List<SourceQueryResults> sqResList = res.getSources();
			assertEquals(1, sqResList.size());
			assertEquals("myres", sqResList.get(0).getMessage());
			assertEquals(sourceUUID.toString(), sqResList.get(0).getSourceUUID().toString());
			
			assertEquals(0, res.getProgress());
		} finally {
			_folder.delete();
		}
	}
	
	@Test
	public void testProcessQuerySuccessMultipleSourceWhereSQResultIsNullOnOne() throws Exception {
		File tempDir = _folder.newFolder();
		try {
			File taskDir = new File(tempDir.getAbsolutePath() + File.separator + "task");
			assertTrue(taskDir.mkdirs());
			UUID sourceUUID = UUID.randomUUID();
			SourceConfiguration srcConfig = new SourceConfiguration();
			srcConfig.setUuid(sourceUUID.toString());
			srcConfig.setName("source1");
			
			UUID sourceUUID2 = UUID.randomUUID();
			SourceConfiguration srcConfig2 = new SourceConfiguration();
			srcConfig2.setUuid(sourceUUID2.toString());
			srcConfig2.setName("source2");
			
			SourceConfigurations sc = new SourceConfigurations();
			sc.setSources(Arrays.asList(srcConfig, srcConfig2));
			
			Map<String,SourceEngine> sourceEngines = new HashMap<>();
			
			SourceEngine mockSrcEngine = mock(SourceEngine.class);
			SourceQueryResults sqRes = new SourceQueryResults();
			sqRes.setMessage("myres");
			sqRes.setStatus(QueryResults.PROCESSING_STATUS);
			when(mockSrcEngine.getSourceQueryResults(any(Query.class))).thenReturn(sqRes);
			sourceEngines.put("source1", mockSrcEngine);
			
			SourceEngine mockSrcEngine2 = mock(SourceEngine.class);
			when(mockSrcEngine2.getSourceQueryResults(any(Query.class))).thenReturn(null);
			sourceEngines.put("source2", mockSrcEngine2);
			
			BasicSearchEngineImpl engine = new BasicSearchEngineImpl(tempDir.getAbsolutePath(), 
					taskDir.getAbsolutePath(), sc, 0, sourceEngines,geneSymbolFile);
			Query query = new Query();
			query.setGeneList(Arrays.asList("gene1","gene2"));
			query.setSourceList(Arrays.asList("source1", "source2"));
			engine.processQuery("queryid", query);
			
			QueryResults res = engine.getQueryResultsFromDbOrFilesystem("queryid");
			assertNotNull(res);
			assertEquals(QueryResults.PROCESSING_STATUS, res.getStatus());
			assertTrue(res.getStartTime() > 0);
			List<SourceQueryResults> sqResList = res.getSources();
			assertEquals(2, sqResList.size());
			
			SourceQueryResults goodRes = null;
			SourceQueryResults failedRes = null;
			for (SourceQueryResults sqrResElement : sqResList){
				if (sqrResElement.getSourceUUID().toString().equals(sourceUUID.toString())){
					goodRes = sqrResElement;
					
				} else if (sqrResElement.getSourceUUID().toString().equals(sourceUUID2.toString())){
					failedRes = sqrResElement;
				}
			}
			assertNotNull(goodRes);
			assertEquals(sourceUUID.toString(), goodRes.getSourceUUID().toString());
			assertEquals(0, goodRes.getProgress());
			

			assertNotNull(failedRes);
			assertEquals("Result from source source2 was null", failedRes.getMessage());
			assertEquals(100, failedRes.getProgress());
			assertEquals(QueryResults.FAILED_STATUS, failedRes.getStatus());
			assertEquals(sourceUUID2.toString(), failedRes.getSourceUUID().toString());
		} finally {
			_folder.delete();
		}
	}
	
	@Test
	public void testcheckAndUpdateQueryResults_ResultIsComplete() throws SearchException {
		Map<String,SourceEngine> sourceEngines = new HashMap<>();
		SourceConfigurations sc = new SourceConfigurations();
		BasicSearchEngineImpl engine = new BasicSearchEngineImpl("/dbdir", 
					"/taskdir", sc, 0, sourceEngines,geneSymbolFile);
		QueryResults qr = new QueryResults();
		qr.setStatus(QueryResults.COMPLETE_STATUS);
		qr.setProgress(55);
		engine.checkAndUpdateQueryResults("uuid", qr);
		assertEquals(55, qr.getProgress());
		assertEquals(QueryResults.COMPLETE_STATUS, qr.getStatus());
	}
	
	@Test
	public void testcheckAndUpdateQueryResults_ResultIsFailed() throws SearchException {
		Map<String,SourceEngine> sourceEngines = new HashMap<>();
		SourceConfigurations sc = new SourceConfigurations();
		BasicSearchEngineImpl engine = new BasicSearchEngineImpl("/dbdir", 
					"/taskdir", sc, 0, sourceEngines,geneSymbolFile);
		QueryResults qr = new QueryResults();
		qr.setStatus(QueryResults.FAILED_STATUS);
		qr.setProgress(55);
		engine.checkAndUpdateQueryResults("uuid", qr);
		assertEquals(55, qr.getProgress());
		assertEquals(QueryResults.FAILED_STATUS, qr.getStatus());
	}
	
	@Test
	public void testcheckAndUpdateQueryResults_SourcesIsNull() throws SearchException {
		Map<String,SourceEngine> sourceEngines = new HashMap<>();
		SourceConfigurations sc = new SourceConfigurations();
		BasicSearchEngineImpl engine = new BasicSearchEngineImpl("/dbdir", 
					"/taskdir", sc, 0, sourceEngines,geneSymbolFile);
		QueryResults qr = new QueryResults();
		qr.setStatus(QueryResults.PROCESSING_STATUS);
		qr.setProgress(55);
		engine.checkAndUpdateQueryResults("uuid", qr);
		assertEquals(100, qr.getProgress());
		assertEquals(QueryResults.FAILED_STATUS, qr.getStatus());
		assertEquals("No sources in result", qr.getMessage());
	}
	
	@Test
	public void testcheckAndUpdateQueryResults_UnknownSource() throws SearchException {
		Map<String,SourceEngine> sourceEngines = new HashMap<>();
		SourceConfigurations sc = new SourceConfigurations();
		BasicSearchEngineImpl engine = new BasicSearchEngineImpl("/dbdir", 
					"/taskdir", sc, 0, sourceEngines,geneSymbolFile);
		QueryResults qr = new QueryResults();
		qr.setStatus(QueryResults.PROCESSING_STATUS);
		SourceQueryResults sqres = new SourceQueryResults();
		sqres.setSourceName("foo");
		qr.setSources(Arrays.asList(sqres));
		qr.setProgress(55);
		engine.checkAndUpdateQueryResults("uuid", qr);
		
		assertEquals(100, qr.getProgress());
		assertEquals(QueryResults.FAILED_STATUS, qr.getStatus());
		assertEquals("[foo] source(s) failed", qr.getMessage());
		assertEquals(1, qr.getSources().size());
		
		SourceQueryResults updatedsqres = qr.getSources().get(0);
		assertEquals(100, updatedsqres.getProgress());
		assertEquals(0, updatedsqres.getNumberOfHits());
		assertEquals("Unknown source", updatedsqres.getMessage());
		assertEquals(QueryResults.FAILED_STATUS, updatedsqres.getStatus());
	}
	
	@Test
	public void testcheckAndUpdateQueryResults_TwoCompleteOneUnfinished() throws SearchException {
		Map<String,SourceEngine> sourceEngines = new HashMap<>();
		
		SourceEngine emockSrcEngine = mock(SourceEngine.class);
		SourceEngine pmockSrcEngine = mock(SourceEngine.class);
		SourceEngine gmockSrcEngine = mock(SourceEngine.class);
		sourceEngines.put(SourceResult.ENRICHMENT_SERVICE, emockSrcEngine);
		sourceEngines.put(SourceResult.INTERACTOME_PPI_SERVICE, pmockSrcEngine);
		sourceEngines.put(SourceResult.INTERACTOME_GENEASSOCIATION_SERVICE, gmockSrcEngine);

		SourceConfigurations sc = new SourceConfigurations();
		BasicSearchEngineImpl engine = new BasicSearchEngineImpl("/dbdir", 
					"/taskdir", sc, 0, sourceEngines,geneSymbolFile);

		QueryResults qr = new QueryResults();
		qr.setStatus(QueryResults.PROCESSING_STATUS);
		
		SourceQueryResults esqres = new SourceQueryResults();
		esqres.setSourceName(SourceResult.ENRICHMENT_SERVICE);
		esqres.setStatus(QueryResults.PROCESSING_STATUS);
		esqres.setProgress(20);

		SourceQueryResults psqres = new SourceQueryResults();
		psqres.setSourceName(SourceResult.INTERACTOME_PPI_SERVICE);
		psqres.setStatus(QueryResults.COMPLETE_STATUS);
		psqres.setProgress(100);
		
		SourceQueryResults gsqres = new SourceQueryResults();
		gsqres.setSourceName(SourceResult.INTERACTOME_GENEASSOCIATION_SERVICE);
		gsqres.setStatus(QueryResults.COMPLETE_STATUS);
		gsqres.setProgress(100);
		
		qr.setSources(Arrays.asList(esqres, psqres, gsqres));
		qr.setProgress(0);
		engine.checkAndUpdateQueryResults("uuid", qr);
		
		assertEquals(67, qr.getProgress());
		assertEquals(QueryResults.PROCESSING_STATUS, qr.getStatus());
		assertEquals(null, qr.getMessage());
		assertEquals(3, qr.getSources().size());
		
		Map<String,SourceQueryResults> resHash = getSourceQueryResultsFromQueryResults(qr);
		
		SourceQueryResults eRes = resHash.get(SourceResult.ENRICHMENT_SERVICE);
		assertEquals(20, eRes.getProgress());
		assertEquals(QueryResults.PROCESSING_STATUS, eRes.getStatus());
		
		SourceQueryResults pRes = resHash.get(SourceResult.INTERACTOME_PPI_SERVICE);
		assertEquals(100, pRes.getProgress());
		assertEquals(QueryResults.COMPLETE_STATUS, pRes.getStatus());
		
		SourceQueryResults gRes = resHash.get(SourceResult.INTERACTOME_GENEASSOCIATION_SERVICE);
		assertEquals(100, gRes.getProgress());
		assertEquals(QueryResults.COMPLETE_STATUS, gRes.getStatus());
	}
	
	@Test
	public void testcheckAndUpdateQueryResults_AllComplete() throws SearchException {
		Map<String,SourceEngine> sourceEngines = new HashMap<>();
		
		SourceEngine emockSrcEngine = mock(SourceEngine.class);
		SourceEngine pmockSrcEngine = mock(SourceEngine.class);
		SourceEngine gmockSrcEngine = mock(SourceEngine.class);
		
		SourceQueryResults esqres = new SourceQueryResults();
		esqres.setSourceName(SourceResult.ENRICHMENT_SERVICE);
		esqres.setStatus(QueryResults.PROCESSING_STATUS);
		esqres.setProgress(20);
		
		doAnswer(invocation -> {
			SourceQueryResults theSQR = (SourceQueryResults)invocation.getArgument(0);
			theSQR.setNumberOfHits(1);
			theSQR.setProgress(100);
			theSQR.setStatus(QueryResults.COMPLETE_STATUS);
			return null;
		}).when(emockSrcEngine).updateSourceQueryResults(any(SourceQueryResults.class));
		
		sourceEngines.put(SourceResult.ENRICHMENT_SERVICE, emockSrcEngine);
		sourceEngines.put(SourceResult.INTERACTOME_PPI_SERVICE, pmockSrcEngine);
		sourceEngines.put(SourceResult.INTERACTOME_GENEASSOCIATION_SERVICE, gmockSrcEngine);

		SourceConfigurations sc = new SourceConfigurations();
		BasicSearchEngineImpl engine = new BasicSearchEngineImpl("/dbdir", 
					"/taskdir", sc, 0, sourceEngines,geneSymbolFile);

		QueryResults qr = new QueryResults();
		qr.setStatus(QueryResults.PROCESSING_STATUS);

		SourceQueryResults psqres = new SourceQueryResults();
		psqres.setSourceName(SourceResult.INTERACTOME_PPI_SERVICE);
		psqres.setStatus(QueryResults.COMPLETE_STATUS);
		psqres.setNumberOfHits(10);
		psqres.setProgress(100);
		
		SourceQueryResults gsqres = new SourceQueryResults();
		gsqres.setSourceName(SourceResult.INTERACTOME_GENEASSOCIATION_SERVICE);
		gsqres.setStatus(QueryResults.COMPLETE_STATUS);
		gsqres.setNumberOfHits(100);
		gsqres.setProgress(100);
		
		qr.setSources(Arrays.asList(esqres, psqres, gsqres));
		qr.setProgress(0);
		engine.checkAndUpdateQueryResults("uuid", qr);
		
		verify(emockSrcEngine, times(1)).updateSourceQueryResults(any(SourceQueryResults.class));
		
		assertEquals(100, qr.getProgress());
		assertEquals(111, qr.getNumberOfHits());
		assertEquals(QueryResults.COMPLETE_STATUS, qr.getStatus());
		assertEquals(null, qr.getMessage());
		assertEquals(3, qr.getSources().size());
		
		Map<String,SourceQueryResults> resHash = getSourceQueryResultsFromQueryResults(qr);
		
		SourceQueryResults eRes = resHash.get(SourceResult.ENRICHMENT_SERVICE);
		assertEquals(100, eRes.getProgress());
		assertEquals(QueryResults.COMPLETE_STATUS, eRes.getStatus());
		assertEquals(1, eRes.getNumberOfHits());
		
		SourceQueryResults pRes = resHash.get(SourceResult.INTERACTOME_PPI_SERVICE);
		assertEquals(100, pRes.getProgress());
		assertEquals(QueryResults.COMPLETE_STATUS, pRes.getStatus());
		
		SourceQueryResults gRes = resHash.get(SourceResult.INTERACTOME_GENEASSOCIATION_SERVICE);
		assertEquals(100, gRes.getProgress());
		assertEquals(QueryResults.COMPLETE_STATUS, gRes.getStatus());
	}
	
	@Test
	public void testcheckAndUpdateQueryResults_OneSucceedsTwoAreFailed() throws SearchException {
		Map<String,SourceEngine> sourceEngines = new HashMap<>();
		
		SourceEngine emockSrcEngine = mock(SourceEngine.class);
		SourceEngine pmockSrcEngine = mock(SourceEngine.class);
		SourceEngine gmockSrcEngine = mock(SourceEngine.class);
		
		SourceQueryResults esqres = new SourceQueryResults();
		esqres.setSourceName(SourceResult.ENRICHMENT_SERVICE);
		esqres.setStatus(QueryResults.PROCESSING_STATUS);
		esqres.setProgress(20);
		
		doAnswer(invocation -> {
			SourceQueryResults theSQR = (SourceQueryResults)invocation.getArgument(0);
			theSQR.setNumberOfHits(1);
			theSQR.setProgress(100);
			theSQR.setWallTime(10);
			theSQR.setMessage("some error");
			theSQR.setStatus(QueryResults.FAILED_STATUS);
			return null;
		}).when(emockSrcEngine).updateSourceQueryResults(any(SourceQueryResults.class));
		
		sourceEngines.put(SourceResult.ENRICHMENT_SERVICE, emockSrcEngine);
		sourceEngines.put(SourceResult.INTERACTOME_PPI_SERVICE, pmockSrcEngine);
		sourceEngines.put(SourceResult.INTERACTOME_GENEASSOCIATION_SERVICE, gmockSrcEngine);

		SourceConfigurations sc = new SourceConfigurations();
		BasicSearchEngineImpl engine = new BasicSearchEngineImpl("/dbdir", 
					"/taskdir", sc, 0, sourceEngines,geneSymbolFile);

		QueryResults qr = new QueryResults();
		qr.setStatus(QueryResults.PROCESSING_STATUS);

		SourceQueryResults psqres = new SourceQueryResults();
		psqres.setSourceName(SourceResult.INTERACTOME_PPI_SERVICE);
		psqres.setStatus(QueryResults.FAILED_STATUS);
		psqres.setNumberOfHits(10);
		psqres.setWallTime(100);
		psqres.setProgress(100);
		
		SourceQueryResults gsqres = new SourceQueryResults();
		gsqres.setSourceName(SourceResult.INTERACTOME_GENEASSOCIATION_SERVICE);
		gsqres.setStatus(QueryResults.COMPLETE_STATUS);
		gsqres.setNumberOfHits(100);
		gsqres.setWallTime(1000);
		gsqres.setProgress(100);
		
		qr.setSources(Arrays.asList(esqres, psqres, gsqres));
		qr.setProgress(0);
		engine.checkAndUpdateQueryResults("uuid", qr);
		
		verify(emockSrcEngine, times(1)).updateSourceQueryResults(any(SourceQueryResults.class));
		
		assertEquals(100, qr.getProgress());
		assertEquals(111, qr.getNumberOfHits());
		assertEquals(QueryResults.FAILED_STATUS, qr.getStatus());
		assertEquals(1000, qr.getWallTime());
		assertTrue(qr.getMessage().contains("] source(s) failed"));
		assertTrue(qr.getMessage().contains(SourceResult.ENRICHMENT_SERVICE));
		assertTrue(qr.getMessage().contains(SourceResult.INTERACTOME_PPI_SERVICE));
		
		
		assertEquals("[enrichment, interactome-ppi] source(s) failed", qr.getMessage());
		assertEquals(3, qr.getSources().size());
		
		Map<String,SourceQueryResults> resHash = getSourceQueryResultsFromQueryResults(qr);
		
		SourceQueryResults eRes = resHash.get(SourceResult.ENRICHMENT_SERVICE);
		assertEquals(100, eRes.getProgress());
		assertEquals(QueryResults.FAILED_STATUS, eRes.getStatus());
		assertEquals(1, eRes.getNumberOfHits());
		
		SourceQueryResults pRes = resHash.get(SourceResult.INTERACTOME_PPI_SERVICE);
		assertEquals(100, pRes.getProgress());
		assertEquals(QueryResults.FAILED_STATUS, pRes.getStatus());
		
		SourceQueryResults gRes = resHash.get(SourceResult.INTERACTOME_GENEASSOCIATION_SERVICE);
		assertEquals(100, gRes.getProgress());
		assertEquals(QueryResults.COMPLETE_STATUS, gRes.getStatus());
	}
	
	@Test
	public void testfilterQueryResultsBySourceListNoSource() throws SearchException {
		Map<String,SourceEngine> sourceEngines = new HashMap<>();
		SourceConfigurations sc = new SourceConfigurations();
		BasicSearchEngineImpl engine = new BasicSearchEngineImpl("/dbdir", 
					"/taskdir", sc, 0, sourceEngines,geneSymbolFile);
		
		QueryResults qr = new QueryResults();
		
		SourceQueryResults sqr = new SourceQueryResults();
		sqr.setSourceName("foosource");
		qr.setSources(Arrays.asList(sqr));
		engine.filterQueryResultsBySourceList(qr, null);
		engine.filterQueryResultsBySourceList(qr, "");
		engine.filterQueryResultsBySourceList(qr, "  ");
		
		assertEquals("foosource", qr.getSources().get(0).getSourceName());
	}
	
	@Test
	public void testfilterQueryResultsBySourceListNoMatchingSingleSource() throws SearchException {
		Map<String,SourceEngine> sourceEngines = new HashMap<>();
		SourceConfigurations sc = new SourceConfigurations();
		BasicSearchEngineImpl engine = new BasicSearchEngineImpl("/dbdir", 
					"/taskdir", sc, 0, sourceEngines,geneSymbolFile);
		
		QueryResults qr = new QueryResults();
		qr.setNumberOfHits(10);
		SourceQueryResults sqr = new SourceQueryResults();
		sqr.setSourceName("foosource");
		qr.setSources(Arrays.asList(sqr));
		engine.filterQueryResultsBySourceList(qr, "blah");
		assertEquals(0, qr.getSources().size());
	}
	
	@Test
	public void testfilterQueryResultsBySourceListMatchedSingleSource() throws SearchException {
		Map<String,SourceEngine> sourceEngines = new HashMap<>();
		SourceConfigurations sc = new SourceConfigurations();
		BasicSearchEngineImpl engine = new BasicSearchEngineImpl("/dbdir", 
					"/taskdir", sc, 0, sourceEngines,geneSymbolFile);
		
		QueryResults qr = new QueryResults();
		qr.setNumberOfHits(10);
		SourceQueryResults sqr = new SourceQueryResults();
		sqr.setSourceName("foosource");
		qr.setSources(Arrays.asList(sqr));
		engine.filterQueryResultsBySourceList(qr, "foosource");
		assertEquals(1, qr.getSources().size());
		assertEquals("foosource", qr.getSources().get(0).getSourceName());
	}
	
	@Test
	public void testfilterQueryResultsBySourceListMatchedMultipleCommaDelimSource() throws SearchException {
		Map<String,SourceEngine> sourceEngines = new HashMap<>();
		SourceConfigurations sc = new SourceConfigurations();
		BasicSearchEngineImpl engine = new BasicSearchEngineImpl("/dbdir", 
					"/taskdir", sc, 0, sourceEngines,geneSymbolFile);
		
		QueryResults qr = new QueryResults();
		qr.setNumberOfHits(10);
		SourceQueryResults sqr = new SourceQueryResults();
		sqr.setSourceName("foosource");
		SourceQueryResults sqr2 = new SourceQueryResults();
		sqr2.setSourceName("othersource");
		SourceQueryResults sqr3 = new SourceQueryResults();
		sqr3.setSourceName("blahsource");
		
		qr.setSources(Arrays.asList(sqr, sqr2, sqr3));
		engine.filterQueryResultsBySourceList(qr, "foosource,blahsource");
		assertEquals(2, qr.getSources().size());
		Set<String> srcNames = new HashSet<>();
		for (SourceQueryResults ss : qr.getSources()){;
			srcNames.add(ss.getSourceName());
		}
		assertEquals(2, srcNames.size());
		assertTrue(srcNames.contains("foosource"));
		assertTrue(srcNames.contains("blahsource"));
		
	}
	
	@Test
	public void testfilterQueryResultsBySourceListMatchedMultipleCommaDelimPlusSpaceSource() throws SearchException {
		Map<String,SourceEngine> sourceEngines = new HashMap<>();
		SourceConfigurations sc = new SourceConfigurations();
		BasicSearchEngineImpl engine = new BasicSearchEngineImpl("/dbdir", 
					"/taskdir", sc, 0, sourceEngines,geneSymbolFile);
		
		QueryResults qr = new QueryResults();
		qr.setNumberOfHits(10);
		SourceQueryResults sqr = new SourceQueryResults();
		sqr.setSourceName("foosource");
		SourceQueryResults sqr2 = new SourceQueryResults();
		sqr2.setSourceName("othersource");
		SourceQueryResults sqr3 = new SourceQueryResults();
		sqr3.setSourceName("blahsource");
		
		qr.setSources(Arrays.asList(sqr, sqr2, sqr3));
		engine.filterQueryResultsBySourceList(qr, "foosource , blahsource");
		assertEquals(2, qr.getSources().size());
		Set<String> srcNames = new HashSet<>();
		for (SourceQueryResults ss : qr.getSources()){;
			srcNames.add(ss.getSourceName());
		}
		assertEquals(2, srcNames.size());
		assertTrue(srcNames.contains("foosource"));
		assertTrue(srcNames.contains("blahsource"));
	}
	
	@Test
	public void testfilterQueryResultsByStartAndSizeNoSources() throws SearchException {
		Map<String,SourceEngine> sourceEngines = new HashMap<>();
		SourceConfigurations sc = new SourceConfigurations();
		BasicSearchEngineImpl engine = new BasicSearchEngineImpl("/dbdir", 
					"/taskdir", sc, 0, sourceEngines,geneSymbolFile);
		
		QueryResults qr = new QueryResults();
		engine.filterQueryResultsByStartAndSize(qr, 0, 0);
		qr.setSources(new ArrayList<>());
		engine.filterQueryResultsByStartAndSize(qr, 0, 0);
	}
	
	@Test
	public void testfilterQueryResultsByStartAndSizeAllNoResultsToSort() throws SearchException {
		Map<String,SourceEngine> sourceEngines = new HashMap<>();
		SourceConfigurations sc = new SourceConfigurations();
		BasicSearchEngineImpl engine = new BasicSearchEngineImpl("/dbdir", 
					"/taskdir", sc, 0, sourceEngines,geneSymbolFile);
		
		QueryResults qr = new QueryResults();
		SourceQueryResults sqr = new SourceQueryResults();
		sqr.setSourceName("a_source");
		sqr.setSourceRank(2);
		SourceQueryResults sqr2 = new SourceQueryResults();
		sqr2.setSourceName("b_source");
		sqr2.setSourceRank(1);
		SourceQueryResults sqr3 = new SourceQueryResults();
		sqr3.setSourceName("c_source");
		sqr3.setSourceRank(3);
		
		qr.setSources(Arrays.asList(sqr, sqr2, sqr3));
		engine.filterQueryResultsByStartAndSize(qr, 0, 0);
		assertEquals(3, qr.getSources().size());
	}
	
	@Test
	public void testfilterQueryResultsByStartAndSizeKeepAllResults() throws SearchException {
		Map<String,SourceEngine> sourceEngines = new HashMap<>();
		SourceConfigurations sc = new SourceConfigurations();
		BasicSearchEngineImpl engine = new BasicSearchEngineImpl("/dbdir", 
					"/taskdir", sc, 0, sourceEngines,geneSymbolFile);
		
		QueryResults qr = new QueryResults();
		SourceQueryResults sqr = new SourceQueryResults();
		sqr.setSourceName("a_source");
		sqr.setSourceRank(2);
		SourceQueryResult res1 = new SourceQueryResult();
		res1.setRank(5);
		res1.setDescription("res1");
		SourceQueryResult res2 = new SourceQueryResult();
		res2.setRank(4);
		res2.setDescription("res2");
		sqr.setResults(Arrays.asList(res1, res2));
		
		SourceQueryResults sqr2 = new SourceQueryResults();
		sqr2.setSourceName("b_source");
		sqr2.setSourceRank(1);
		SourceQueryResult res3 = new SourceQueryResult();
		res3.setRank(0);
		res3.setDescription("res3");
		sqr2.setResults(Arrays.asList(res3));
		
		SourceQueryResults sqr3 = new SourceQueryResults();
		sqr3.setSourceName("c_source");
		sqr3.setSourceRank(3);
		SourceQueryResult res4 = new SourceQueryResult();
		res4.setRank(2);
		res4.setDescription("res4");
		sqr3.setResults(Arrays.asList(res4));
		
		qr.setSources(Arrays.asList(sqr, sqr2, sqr3));
		engine.filterQueryResultsByStartAndSize(qr, 0, 0);
		assertEquals(3, qr.getSources().size());
		
		assertEquals("b_source", qr.getSources().get(0).getSourceName());
		assertEquals("a_source", qr.getSources().get(1).getSourceName());
		assertEquals("c_source", qr.getSources().get(2).getSourceName());
		
		assertEquals(1, qr.getSources().get(0).getResults().size());
		assertEquals("res3", qr.getSources().get(0).getResults().get(0).getDescription());
		
		assertEquals(2, qr.getSources().get(1).getResults().size());
		assertEquals("res2", qr.getSources().get(1).getResults().get(0).getDescription());
		assertEquals("res1", qr.getSources().get(1).getResults().get(1).getDescription());
		
		assertEquals(1, qr.getSources().get(2).getResults().size());
		assertEquals("res4", qr.getSources().get(2).getResults().get(0).getDescription());
	}
	
	@Test
	public void testfilterQueryResultsByStartAndSizeWithFilter() throws SearchException {
		Map<String,SourceEngine> sourceEngines = new HashMap<>();
		SourceConfigurations sc = new SourceConfigurations();
		BasicSearchEngineImpl engine = new BasicSearchEngineImpl("/dbdir", 
					"/taskdir", sc, 0, sourceEngines,geneSymbolFile);
		
		QueryResults qr = new QueryResults();
		SourceQueryResults sqr = new SourceQueryResults();
		sqr.setSourceName("a_source");
		sqr.setSourceRank(2);
		SourceQueryResult res1 = new SourceQueryResult();
		res1.setRank(5);
		res1.setDescription("res1");
		SourceQueryResult res2 = new SourceQueryResult();
		res2.setRank(4);
		res2.setDescription("res2");
		sqr.setResults(Arrays.asList(res1, res2));
		
		SourceQueryResults sqr2 = new SourceQueryResults();
		sqr2.setSourceName("b_source");
		sqr2.setSourceRank(1);
		SourceQueryResult res3 = new SourceQueryResult();
		res3.setRank(0);
		res3.setDescription("res3");
		sqr2.setResults(Arrays.asList(res3));
		
		SourceQueryResults sqr3 = new SourceQueryResults();
		sqr3.setSourceName("c_source");
		sqr3.setSourceRank(3);
		SourceQueryResult res4 = new SourceQueryResult();
		res4.setRank(2);
		res4.setDescription("res4");
		
		sqr3.setResults(Arrays.asList(res4));
		
		qr.setSources(Arrays.asList(sqr, sqr2, sqr3));
		engine.filterQueryResultsByStartAndSize(qr, 1, 2);
		assertEquals(3, qr.getSources().size());
		assertEquals("a_source", qr.getSources().get(1).getSourceName());
		
		assertEquals(2, qr.getSources().get(1).getResults().size());
		assertEquals("res2", qr.getSources().get(1).getResults().get(0).getDescription());
		assertEquals("res1", qr.getSources().get(1).getResults().get(1).getDescription());
	}
	
	@Test
	public void testfilterQueryResultsByStartAndSizeNonZeroStartSizeOne() throws SearchException {
		Map<String,SourceEngine> sourceEngines = new HashMap<>();
		SourceConfigurations sc = new SourceConfigurations();
		BasicSearchEngineImpl engine = new BasicSearchEngineImpl("/dbdir", 
					"/taskdir", sc, 0, sourceEngines,geneSymbolFile);
		
		QueryResults qr = new QueryResults();
		SourceQueryResults sqr = new SourceQueryResults();
		sqr.setSourceName("a_source");
		sqr.setSourceRank(2);
		SourceQueryResult res1 = new SourceQueryResult();
		res1.setRank(5);
		res1.setDescription("res1");
		SourceQueryResult res2 = new SourceQueryResult();
		res2.setRank(4);
		res2.setDescription("res2");
		sqr.setResults(Arrays.asList(res1, res2));
		
		SourceQueryResults sqr2 = new SourceQueryResults();
		sqr2.setSourceName("b_source");
		sqr2.setSourceRank(1);
		SourceQueryResult res3 = new SourceQueryResult();
		res3.setRank(0);
		res3.setDescription("res3");
		sqr2.setResults(Arrays.asList(res3));
		
		SourceQueryResults sqr3 = new SourceQueryResults();
		sqr3.setSourceName("c_source");
		sqr3.setSourceRank(3);
		SourceQueryResult res4 = new SourceQueryResult();
		res4.setRank(2);
		res4.setDescription("res4");
		
		sqr3.setResults(Arrays.asList(res4));
		
		qr.setSources(Arrays.asList(sqr, sqr2, sqr3));
		engine.filterQueryResultsByStartAndSize(qr, 1, 1);
		assertEquals(3, qr.getSources().size());
		
		assertEquals("a_source", qr.getSources().get(1).getSourceName());
		
		assertEquals(1, qr.getSources().get(1).getResults().size());
		assertEquals("res2", qr.getSources().get(1).getResults().get(0).getDescription());
	}
	
	@Test
	public void testfilterQueryResultsByStartAndSizeWithZeroStart() throws SearchException {
		Map<String,SourceEngine> sourceEngines = new HashMap<>();
		SourceConfigurations sc = new SourceConfigurations();
		BasicSearchEngineImpl engine = new BasicSearchEngineImpl("/dbdir", 
					"/taskdir", sc, 0, sourceEngines,geneSymbolFile);
		
		QueryResults qr = new QueryResults();
		SourceQueryResults sqr = new SourceQueryResults();
		sqr.setSourceName("a_source");
		sqr.setSourceRank(2);
		SourceQueryResult res1 = new SourceQueryResult();
		res1.setRank(5);
		res1.setDescription("res1");
		SourceQueryResult res2 = new SourceQueryResult();
		res2.setRank(4);
		res2.setDescription("res2");
		sqr.setResults(Arrays.asList(res1, res2));
		
		SourceQueryResults sqr2 = new SourceQueryResults();
		sqr2.setSourceName("b_source");
		sqr2.setSourceRank(1);
		SourceQueryResult res3 = new SourceQueryResult();
		res3.setRank(0);
		res3.setDescription("res3");
		sqr2.setResults(Arrays.asList(res3));
		
		SourceQueryResults sqr3 = new SourceQueryResults();
		sqr3.setSourceName("c_source");
		sqr3.setSourceRank(3);
		SourceQueryResult res4 = new SourceQueryResult();
		res4.setRank(2);
		res4.setDescription("res4");
		
		sqr3.setResults(Arrays.asList(res4));
		
		qr.setSources(Arrays.asList(sqr, sqr2, sqr3));
		engine.filterQueryResultsByStartAndSize(qr, 0, 2);
		assertEquals(3, qr.getSources().size());
		
		assertEquals("b_source", qr.getSources().get(0).getSourceName());
		assertEquals("a_source", qr.getSources().get(1).getSourceName());

		assertEquals(1, qr.getSources().get(0).getResults().size());
		assertEquals("res3", qr.getSources().get(0).getResults().get(0).getDescription());
		
		assertEquals(1, qr.getSources().get(1).getResults().size());
		assertEquals("res2", qr.getSources().get(1).getResults().get(0).getDescription());
	}
	
	@Test
	public void testGetQueryResultsNotFound() throws SearchException, IOException {
		File tempDir = _folder.newFolder();
		try {
			Map<String,SourceEngine> sourceEngines = new HashMap<>();
			SourceConfigurations sc = new SourceConfigurations();
			BasicSearchEngineImpl engine = new BasicSearchEngineImpl(tempDir.getAbsolutePath(), 
						tempDir.getAbsolutePath(), sc, 0, sourceEngines,geneSymbolFile);
			assertNull(engine.getQueryResults("nonexistantid", "foosource", 0, 0));
		} finally {
			_folder.delete();
		}
	}
	
	@Test
	public void testGetQueryResultsNegativeStart() throws SearchException, IOException {
		File tempDir = _folder.newFolder();
		try {
			Map<String,SourceEngine> sourceEngines = new HashMap<>();
			SourceConfigurations sc = new SourceConfigurations();
			BasicSearchEngineImpl engine = new BasicSearchEngineImpl(tempDir.getAbsolutePath(), 
						tempDir.getAbsolutePath(), sc, 0, sourceEngines,geneSymbolFile);
			Query thequery = new Query();
			thequery.setSourceList(Arrays.asList("foosource"));
			thequery.setGeneList(Arrays.asList("gene1", "gene2"));
			String id = engine.query(thequery);
			try {
				engine.getQueryResults(id, "foosource", -1, 0);
				fail("Expected SearchException");
			} catch(SearchException se) {
				assertTrue(se.getMessage().contains("start parameter must be value of 0 or"));
			}
		} finally {
			_folder.delete();
		}
	}
	
	@Test
	public void testGetQueryResultsNegativeSize() throws SearchException, IOException {
		File tempDir = _folder.newFolder();
		try {
			Map<String,SourceEngine> sourceEngines = new HashMap<>();
			SourceConfigurations sc = new SourceConfigurations();
			BasicSearchEngineImpl engine = new BasicSearchEngineImpl(tempDir.getAbsolutePath(), 
						tempDir.getAbsolutePath(), sc, 0, sourceEngines, geneSymbolFile);
			Query thequery = new Query();
			thequery.setSourceList(Arrays.asList("foosource"));
			thequery.setGeneList(Arrays.asList("gene1", "gene2"));
			String id = engine.query(thequery);
			try {
				engine.getQueryResults(id, "foosource", 0, -1);
				fail("Expected SearchException");
			} catch(SearchException se) {
				assertTrue(se.getMessage().contains("size parameter must be value of 0 or"));
			}
		} finally {
			_folder.delete();
		}
	}
	
	@Test
	public void testGetQueryResultsSuccess() throws SearchException, IOException {
		File tempDir = _folder.newFolder();
		try {
			Map<String,SourceEngine> sourceEngines = new HashMap<>();
			SourceConfigurations sc = new SourceConfigurations();
			BasicSearchEngineImpl engine = new BasicSearchEngineImpl(tempDir.getAbsolutePath(), 
						tempDir.getAbsolutePath(), sc, 0, sourceEngines, geneSymbolFile);
			Query thequery = new Query();
			thequery.setSourceList(Arrays.asList("foosource"));
			thequery.setGeneList(Arrays.asList("gene1", "gene2"));
			String id = engine.query(thequery);
			QueryResults qr = engine.getQueryResultsFromDb(id);
			qr.setStatus(QueryResults.COMPLETE_STATUS);
			qr.setMessage("myquery");
			qr.setProgress(100);
			
			QueryResults res = engine.getQueryResults(id, null, 0, 0);
			assertEquals("myquery", res.getMessage());
		} finally {
			_folder.delete();
		}
	}
	
	@Test
	public void testGetQueryStatusNotFound() throws SearchException, IOException {
		File tempDir = _folder.newFolder();
		try {
			Map<String,SourceEngine> sourceEngines = new HashMap<>();
			SourceConfigurations sc = new SourceConfigurations();
			BasicSearchEngineImpl engine = new BasicSearchEngineImpl(tempDir.getAbsolutePath(), 
						tempDir.getAbsolutePath(), sc, 0, sourceEngines,geneSymbolFile);
			assertNull(engine.getQueryStatus("nonexistantid"));
		} finally {
			_folder.delete();
		}
	}
	
	@Test
	public void testGetQueryStatusSuccessNullForSources() throws SearchException, IOException {
		File tempDir = _folder.newFolder();
		try {
			Map<String,SourceEngine> sourceEngines = new HashMap<>();
			SourceConfigurations sc = new SourceConfigurations();
			BasicSearchEngineImpl engine = new BasicSearchEngineImpl(tempDir.getAbsolutePath(), 
						tempDir.getAbsolutePath(), sc, 0, sourceEngines,geneSymbolFile);
			Query thequery = new Query();
			thequery.setSourceList(Arrays.asList("foosource"));
			thequery.setGeneList(Arrays.asList("gene1", "gene2"));
			String id = engine.query(thequery);
			QueryResults qr = engine.getQueryResultsFromDb(id);
			qr.setStatus(QueryResults.COMPLETE_STATUS);
			qr.setMessage("myquery");
			qr.setProgress(100);
		
			QueryStatus res = engine.getQueryStatus(id);
			assertEquals("myquery", res.getMessage());
		} finally {
			_folder.delete();
		}
	}
	
	@Test
	public void testGetQueryStatusSuccessWithSources() throws SearchException, IOException {
		File tempDir = _folder.newFolder();
		try {
			Map<String,SourceEngine> sourceEngines = new HashMap<>();
			SourceConfigurations sc = new SourceConfigurations();
			BasicSearchEngineImpl engine = new BasicSearchEngineImpl(tempDir.getAbsolutePath(), 
						tempDir.getAbsolutePath(), sc, 0, sourceEngines,geneSymbolFile);
			Query thequery = new Query();
			thequery.setSourceList(Arrays.asList("foosource"));
			thequery.setGeneList(Arrays.asList("gene1", "gene2"));
			String id = engine.query(thequery);
			QueryResults qr = engine.getQueryResultsFromDb(id);
			qr.setStatus(QueryResults.COMPLETE_STATUS);
			SourceQueryResults sqr1 = new SourceQueryResults();
			SourceQueryResults sqr2 = new SourceQueryResults();
			
			qr.setSources(Arrays.asList(sqr1, sqr2));
			qr.setMessage("myquery");
			qr.setProgress(100);
			QueryStatus res = engine.getQueryStatus(id);
			assertEquals("myquery", res.getMessage());
		} finally {
			_folder.delete();
		}
	}
	
	@Test
	public void testcombineSearchExceptionsAndThrow() throws SearchException {
		Map<String,SourceEngine> sourceEngines = new HashMap<>();
		SourceConfigurations sc = new SourceConfigurations();
		BasicSearchEngineImpl engine = new BasicSearchEngineImpl("/dbdir", 
					"/taskdir", sc, 0, sourceEngines,geneSymbolFile);
		
		// try passing null
		engine.combineSearchExceptionsAndThrow("someid", null);
		
		// pass in single search exception
		try {
			engine.combineSearchExceptionsAndThrow("someid",
					Arrays.asList(new SearchException("some error")));
		} catch(SearchException se){
			assertEquals("some error", se.getMessage());
		}
		
		// pass in two search exceptions
		try {
			engine.combineSearchExceptionsAndThrow("someid",
					Arrays.asList(new SearchException("some error"),
							new SearchException("another error")));
		} catch(SearchException se){
			assertEquals("2 exceptions raised while attempting to delete task someid : (1) some error : (2) another error", se.getMessage());
		}
	}
	
	@Test
	public void testDeleteNotFound() throws SearchException, IOException {
		File tempDir = _folder.newFolder();
		try {
			Map<String,SourceEngine> sourceEngines = new HashMap<>();
			SourceConfigurations sc = new SourceConfigurations();
			BasicSearchEngineImpl engine = new BasicSearchEngineImpl(tempDir.getAbsolutePath(), 
						tempDir.getAbsolutePath(), sc, 0, sourceEngines,geneSymbolFile);
			engine.delete("someid");
		} finally {
			_folder.delete();
		}
		
	}
	
	@Test
	public void testDeleteNoSourcesTaskNotOnFileSystem() throws SearchException, IOException {
		File tempDir = _folder.newFolder();
		try {
			Map<String,SourceEngine> sourceEngines = new HashMap<>();
			SourceConfigurations sc = new SourceConfigurations();
			BasicSearchEngineImpl engine = new BasicSearchEngineImpl(tempDir.getAbsolutePath(), 
						tempDir.getAbsolutePath(), sc, 0, sourceEngines,geneSymbolFile);
			Query thequery = new Query();
			thequery.setSourceList(Arrays.asList("foosource"));
			thequery.setGeneList(Arrays.asList("gene1", "gene2"));
			String id = engine.query(thequery);
			engine.delete(id);
		} finally {
			_folder.delete();
		}
	}
	
	@Test
	public void testDeleteNoSourcesOnFileSystem() throws SearchException, IOException {
		File tempDir = _folder.newFolder();
		try {
			Map<String,SourceEngine> sourceEngines = new HashMap<>();
			SourceConfigurations sc = new SourceConfigurations();
			BasicSearchEngineImpl engine = new BasicSearchEngineImpl(tempDir.getAbsolutePath(), 
						tempDir.getAbsolutePath(), sc, 0, sourceEngines,geneSymbolFile);
			Query thequery = new Query();
			thequery.setSourceList(Arrays.asList("foosource"));
			thequery.setGeneList(Arrays.asList("gene1", "gene2"));
			String id = engine.query(thequery);
			File taskDir = new File(tempDir.getAbsolutePath() + File.separator + id);
			assertTrue(taskDir.mkdirs());
			engine.saveQueryResultsToFilesystem(id);
			engine.delete(id);
		} finally {
			_folder.delete();
		}
	}
	
	@Test
	public void testDeleteMultipleSources() throws SearchException, IOException {
		File tempDir = _folder.newFolder();
		try {
			Map<String,SourceEngine> sourceEngines = new HashMap<>();
			SourceEngine mockEnrichEngine = mock(SourceEngine.class);
			doThrow(new SearchException("enrich")).when(mockEnrichEngine).delete(any(String.class));
			
			SourceEngine mockPPIEngine = mock(SourceEngine.class);
			SourceEngine mockGeneEngine = mock(SourceEngine.class);
			doThrow(new SearchException("gene")).when(mockGeneEngine).delete(any(String.class));
			
			sourceEngines.put(SourceResult.ENRICHMENT_SERVICE, mockEnrichEngine);
			sourceEngines.put(SourceResult.INTERACTOME_PPI_SERVICE, mockPPIEngine);
			sourceEngines.put(SourceResult.INTERACTOME_GENEASSOCIATION_SERVICE, mockGeneEngine);

			SourceConfigurations sc = new SourceConfigurations();
			BasicSearchEngineImpl engine = new BasicSearchEngineImpl(tempDir.getAbsolutePath(), 
						tempDir.getAbsolutePath(), sc, 0, sourceEngines,geneSymbolFile);
			Query thequery = new Query();
			thequery.setSourceList(Arrays.asList("foosource"));
			thequery.setGeneList(Arrays.asList("gene1", "gene2"));
			
			
			String id = engine.query(thequery);
			QueryResults qr = engine.getQueryResultsFromDb(id);
			SourceQueryResults sqr1 = new SourceQueryResults();
			sqr1.setSourceName("invalidsource");
			
			SourceQueryResults sqr2 = new SourceQueryResults();
			sqr2.setSourceName(SourceResult.ENRICHMENT_SERVICE);
			
			SourceQueryResults sqr3 = new SourceQueryResults();
			sqr3.setSourceName(SourceResult.INTERACTOME_GENEASSOCIATION_SERVICE);

			SourceQueryResults sqr4 = new SourceQueryResults();
			sqr4.setSourceName(SourceResult.INTERACTOME_PPI_SERVICE);

			qr.setSources(Arrays.asList(sqr1, sqr2, sqr3, sqr4));
			engine.updateQueryResultsInDb(id, qr);
			
			try {
				engine.delete(id);
				fail("Expected SearchException");
			} catch(SearchException se){
				assertTrue(se.getMessage().contains("2 exceptions raised while "
						+ "attempting to delete task " + id + " : (1) "));
				assertTrue(se.getMessage().contains("enrich"));
				assertTrue(se.getMessage().contains("gene"));				
			}
		} finally {
			_folder.delete();
		}
	}
	
	@Test
	public void testgetNetworkOverlayAsCXNullSourceNetwork() throws SearchException,
			NdexException, IOException {
		
			Map<String,SourceEngine> sourceEngines = new HashMap<>();
			SourceConfigurations sc = new SourceConfigurations();
			BasicSearchEngineImpl engine = new BasicSearchEngineImpl("/db", "/task",
						sc, 0, sourceEngines,geneSymbolFile);
			try {
				engine.getNetworkOverlayAsCX("nonexistantid", null, null);
			} catch(SearchException se){
				assertEquals("sourceUUID cannot be null", se.getMessage());
			}
			
			try {
				engine.getNetworkOverlayAsCX("nonexistantid", "sourceuuid", null);
			} catch(SearchException se){
				assertEquals("networkUUID cannot be null", se.getMessage());
			}
	}
	
	@Test
	public void testgetNetworkOverlayAsCXNoTaskFound() throws SearchException,
			NdexException, IOException {
		File tempDir = _folder.newFolder();
		try {
			Map<String,SourceEngine> sourceEngines = new HashMap<>();
			SourceConfigurations sc = new SourceConfigurations();
			BasicSearchEngineImpl engine = new BasicSearchEngineImpl(tempDir.getAbsolutePath(), 
						tempDir.getAbsolutePath(), sc, 0, sourceEngines,geneSymbolFile);
			assertNull(engine.getNetworkOverlayAsCX("nonexistantid", "srcuuid", "netuuid"));
		} finally {
			_folder.delete();
		}
	}
	
	@Test
	public void testgetNetworkOverlayAsCXNoMatchingSourceUUID() throws SearchException,
			NdexException, IOException {
		File tempDir = _folder.newFolder();
		try {
			Map<String,SourceEngine> sourceEngines = new HashMap<>();
			SourceConfigurations sc = new SourceConfigurations();
			BasicSearchEngineImpl engine = new BasicSearchEngineImpl(tempDir.getAbsolutePath(), 
						tempDir.getAbsolutePath(), sc, 0, sourceEngines,geneSymbolFile);
			
			Query thequery = new Query();
			thequery.setSourceList(Arrays.asList("foosource"));
			thequery.setGeneList(Arrays.asList("gene1", "gene2"));
			
			String id = engine.query(thequery);
			QueryResults qr = engine.getQueryResultsFromDb(id);
			
			SourceQueryResults sqr2 = new SourceQueryResults();
			UUID uuid2 = UUID.randomUUID();
			sqr2.setSourceUUID(uuid2);
			sqr2.setSourceName(SourceResult.ENRICHMENT_SERVICE);
			
			
			SourceQueryResults sqr3 = new SourceQueryResults();
			UUID uuid3 = UUID.randomUUID();
			sqr3.setSourceUUID(uuid3);
			sqr3.setSourceName(SourceResult.INTERACTOME_GENEASSOCIATION_SERVICE);

			SourceQueryResults sqr4 = new SourceQueryResults();
			UUID uuid4 = UUID.randomUUID();
			sqr4.setSourceUUID(uuid4);
			sqr4.setSourceName(SourceResult.INTERACTOME_PPI_SERVICE);

			qr.setSources(Arrays.asList(sqr2, sqr3, sqr4));
			engine.updateQueryResultsInDb(id, qr);
			assertNull(engine.getNetworkOverlayAsCX(id, uuid3.toString(), "netuuid"));
		} finally {
			_folder.delete();
		}
	}
	
	@Test
	public void testgetNetworkOverlayAsCXSuccess() throws SearchException,
			NdexException, IOException {
		File tempDir = _folder.newFolder();
		try {
			Map<String,SourceEngine> sourceEngines = new HashMap<>();
			SourceEngine mockSrcEngine = mock(SourceEngine.class);
			UUID uuid3 = UUID.randomUUID();
			InputStream mockStream = mock(InputStream.class);

			when(mockSrcEngine.getOverlaidNetworkAsCXStream("srctaskid", "netuuid")).thenReturn(mockStream);
			sourceEngines.put(SourceResult.INTERACTOME_GENEASSOCIATION_SERVICE, mockSrcEngine);
			SourceConfigurations sc = new SourceConfigurations();
			BasicSearchEngineImpl engine = new BasicSearchEngineImpl(tempDir.getAbsolutePath(), 
						tempDir.getAbsolutePath(), sc, 0, sourceEngines,geneSymbolFile);
			
			Query thequery = new Query();
			thequery.setSourceList(Arrays.asList("foosource"));
			thequery.setGeneList(Arrays.asList("gene1", "gene2"));
			
			String id = engine.query(thequery);
			QueryResults qr = engine.getQueryResultsFromDb(id);
			
			SourceQueryResults sqr3 = new SourceQueryResults();
			sqr3.setSourceUUID(uuid3);
			sqr3.setSourceTaskId("srctaskid");
			sqr3.setSourceName(SourceResult.INTERACTOME_GENEASSOCIATION_SERVICE);

			qr.setSources(Arrays.asList(sqr3));
			engine.updateQueryResultsInDb(id, qr);
			InputStream stream = engine.getNetworkOverlayAsCX(id, uuid3.toString(), "netuuid");
			assertEquals(mockStream, stream);
		} finally {
			_folder.delete();
		}
	}
	
}
