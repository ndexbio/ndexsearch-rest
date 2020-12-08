package org.ndexbio.ndexsearch.rest.engine;


import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.Mockito.*;

import static org.junit.Assert.*;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.ndexbio.ndexsearch.rest.exceptions.SearchException;
import org.ndexbio.ndexsearch.rest.model.Query;
import org.ndexbio.ndexsearch.rest.model.QueryResults;
import org.ndexbio.ndexsearch.rest.model.SourceConfiguration;
import org.ndexbio.ndexsearch.rest.model.SourceConfigurations;
import org.ndexbio.ndexsearch.rest.model.SourceQueryResults;
import org.ndexbio.ndexsearch.rest.model.SourceResult;


/**
 *
 * @author churas
 */
public class TestBasicSearchEngineImpl {

	@Rule
	public TemporaryFolder _folder = new TemporaryFolder();

	public Map<String,SourceQueryResults> getSourceQueryResultsFromQueryResults(final QueryResults qr){
		
		Map<String,SourceQueryResults> resHash = new HashMap<>();
		for (SourceQueryResults sqr : qr.getSources()){
			resHash.put(sqr.getSourceName(), sqr);
		}
		return resHash;
	}
	
	@Test
	public void testConstructor() {
		try {
			BasicSearchEngineImpl engine = new BasicSearchEngineImpl("/dbdir", "/task",
				null, 0, null);
			fail("Expected SearchException");
		} catch(SearchException se){
			assertEquals("Sources cannot be null", se.getMessage());
		}
	}
	
	@Test
	public void testGetQueryResultsFilePath() throws SearchException {
		BasicSearchEngineImpl engine = new BasicSearchEngineImpl("/dbdir", "/task",
				null, 0, new HashMap<String,SourceEngine>());

		assertEquals("/task/someid/" + BasicSearchEngineImpl.QR_JSON_FILE,
				engine.getQueryResultsFilePath("someid"));
	}
	
	@Test
	public void testThreadSleepAndUpdateThreadSleepTime() throws SearchException {
		BasicSearchEngineImpl engine = new BasicSearchEngineImpl("/dbdir", "/task",
                                                                 null, 0,
				new HashMap<String,SourceEngine>());
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
				new HashMap<String,SourceEngine>());
		engine.shutdown();
		engine.run();
		
	}
	
	@Test
	public void testGetQueryResultsFromDb() throws SearchException {
		BasicSearchEngineImpl engine = new BasicSearchEngineImpl("/dbdir", "/task",
                                                                 null, 0, new HashMap<String,SourceEngine>());
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
					tempDir.getAbsolutePath(), null, 0, new HashMap<String,SourceEngine>());
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
					tempDir.getAbsolutePath(), null, 0, new HashMap<String,SourceEngine>());
			
			
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
	public void testQueryEmptyOrNullSourceList() throws SearchException {
		BasicSearchEngineImpl engine = new BasicSearchEngineImpl("/db", 
					"/task", null, 0, new HashMap<String,SourceEngine>());
		
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
					"/task", null, 0, new HashMap<String,SourceEngine>());
		Query q = new Query();
		q.setSourceList(Arrays.asList("db1"));
		q.setGeneList(Arrays.asList("gene1"));
		String id = engine.query(q);
		assertNotNull(id);
		QueryResults qr = engine.getQueryResultsFromDb(id);
		assertEquals(QueryResults.SUBMITTED_STATUS, qr.getStatus());
		assertTrue(qr.getStartTime() > 0);
		assertEquals("db1", qr.getInputSourceList().get(0));
		assertEquals("gene1", qr.getQuery().get(0));
	}
	
	@Test
	public void testProcessQueryErrorMakingTaskDirectory() throws Exception {
		File tempDir = _folder.newFolder();
		try {
			File taskFile = new File(tempDir.getAbsolutePath() + File.separator + "task");
			assertTrue(taskFile.createNewFile());
			
			BasicSearchEngineImpl engine = new BasicSearchEngineImpl(tempDir.getAbsolutePath(), 
					taskFile.getAbsolutePath(), null, 0, new HashMap<String,SourceEngine>());
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
					taskDir.getAbsolutePath(), sc, 0, new HashMap<String,SourceEngine>());
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
					taskDir.getAbsolutePath(), sc, 0, sourceEngines);
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
					taskDir.getAbsolutePath(), sc, 0, sourceEngines);
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
					"/taskdir", sc, 0, sourceEngines);
		QueryResults qr = new QueryResults();
		qr.setStatus(QueryResults.COMPLETE_STATUS);
		qr.setProgress(55);
		engine.checkAndUpdateQueryResults(qr);
		assertEquals(55, qr.getProgress());
		assertEquals(QueryResults.COMPLETE_STATUS, qr.getStatus());
	}
	
	@Test
	public void testcheckAndUpdateQueryResults_ResultIsFailed() throws SearchException {
		Map<String,SourceEngine> sourceEngines = new HashMap<>();
		SourceConfigurations sc = new SourceConfigurations();
		BasicSearchEngineImpl engine = new BasicSearchEngineImpl("/dbdir", 
					"/taskdir", sc, 0, sourceEngines);
		QueryResults qr = new QueryResults();
		qr.setStatus(QueryResults.FAILED_STATUS);
		qr.setProgress(55);
		engine.checkAndUpdateQueryResults(qr);
		assertEquals(55, qr.getProgress());
		assertEquals(QueryResults.FAILED_STATUS, qr.getStatus());
	}
	
	@Test
	public void testcheckAndUpdateQueryResults_SourcesIsNull() throws SearchException {
		Map<String,SourceEngine> sourceEngines = new HashMap<>();
		SourceConfigurations sc = new SourceConfigurations();
		BasicSearchEngineImpl engine = new BasicSearchEngineImpl("/dbdir", 
					"/taskdir", sc, 0, sourceEngines);
		QueryResults qr = new QueryResults();
		qr.setStatus(QueryResults.PROCESSING_STATUS);
		qr.setProgress(55);
		engine.checkAndUpdateQueryResults(qr);
		assertEquals(100, qr.getProgress());
		assertEquals(QueryResults.FAILED_STATUS, qr.getStatus());
		assertEquals("No sources in result", qr.getMessage());
	}
	
	@Test
	public void testcheckAndUpdateQueryResults_UnknownSource() throws SearchException {
		Map<String,SourceEngine> sourceEngines = new HashMap<>();
		SourceConfigurations sc = new SourceConfigurations();
		BasicSearchEngineImpl engine = new BasicSearchEngineImpl("/dbdir", 
					"/taskdir", sc, 0, sourceEngines);
		QueryResults qr = new QueryResults();
		qr.setStatus(QueryResults.PROCESSING_STATUS);
		SourceQueryResults sqres = new SourceQueryResults();
		sqres.setSourceName("foo");
		qr.setSources(Arrays.asList(sqres));
		qr.setProgress(55);
		engine.checkAndUpdateQueryResults(qr);
		
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
					"/taskdir", sc, 0, sourceEngines);

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
		engine.checkAndUpdateQueryResults(qr);
		
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
					"/taskdir", sc, 0, sourceEngines);

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
		engine.checkAndUpdateQueryResults(qr);
		
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
			theSQR.setMessage("some error");
			theSQR.setStatus(QueryResults.FAILED_STATUS);
			return null;
		}).when(emockSrcEngine).updateSourceQueryResults(any(SourceQueryResults.class));
		
		sourceEngines.put(SourceResult.ENRICHMENT_SERVICE, emockSrcEngine);
		sourceEngines.put(SourceResult.INTERACTOME_PPI_SERVICE, pmockSrcEngine);
		sourceEngines.put(SourceResult.INTERACTOME_GENEASSOCIATION_SERVICE, gmockSrcEngine);

		SourceConfigurations sc = new SourceConfigurations();
		BasicSearchEngineImpl engine = new BasicSearchEngineImpl("/dbdir", 
					"/taskdir", sc, 0, sourceEngines);

		QueryResults qr = new QueryResults();
		qr.setStatus(QueryResults.PROCESSING_STATUS);

		SourceQueryResults psqres = new SourceQueryResults();
		psqres.setSourceName(SourceResult.INTERACTOME_PPI_SERVICE);
		psqres.setStatus(QueryResults.FAILED_STATUS);
		psqres.setNumberOfHits(10);
		psqres.setProgress(100);
		
		SourceQueryResults gsqres = new SourceQueryResults();
		gsqres.setSourceName(SourceResult.INTERACTOME_GENEASSOCIATION_SERVICE);
		gsqres.setStatus(QueryResults.COMPLETE_STATUS);
		gsqres.setNumberOfHits(100);
		gsqres.setProgress(100);
		
		qr.setSources(Arrays.asList(esqres, psqres, gsqres));
		qr.setProgress(0);
		engine.checkAndUpdateQueryResults(qr);
		
		verify(emockSrcEngine, times(1)).updateSourceQueryResults(any(SourceQueryResults.class));
		
		assertEquals(100, qr.getProgress());
		assertEquals(111, qr.getNumberOfHits());
		assertEquals(QueryResults.FAILED_STATUS, qr.getStatus());
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
	
	
}