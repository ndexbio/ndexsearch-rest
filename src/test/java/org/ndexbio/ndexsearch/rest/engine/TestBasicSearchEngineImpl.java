package org.ndexbio.ndexsearch.rest.engine;


import java.io.File;
import java.io.FileWriter;
import java.util.HashMap;

import static org.junit.Assert.*;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.ndexbio.ndexsearch.rest.exceptions.SearchException;
import org.ndexbio.ndexsearch.rest.model.QueryResults;


/**
 *
 * @author churas
 */
public class TestBasicSearchEngineImpl {
    
    @Rule
    public TemporaryFolder _folder = new TemporaryFolder();
    
    @Test
    public void testGetQueryResultsFilePath() throws SearchException {
        BasicSearchEngineImpl engine = new BasicSearchEngineImpl("/dbdir", "/task",
                                                                 null, 0,
				new HashMap<String,SourceEngine>());
        
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
		BasicSearchEngineImpl engine = new BasicSearchEngineImpl("/dbdir", "/task",
                                                                 null, 600000,
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
}
