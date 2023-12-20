/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ndexbio.ndexsearch.rest.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import org.jboss.resteasy.core.Dispatcher;
import org.jboss.resteasy.mock.MockDispatcherFactory;
import org.jboss.resteasy.mock.MockHttpRequest;
import org.jboss.resteasy.mock.MockHttpResponse;
import static org.junit.Assert.*;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.ndexbio.enrichment.rest.model.DatabaseResults;
import org.ndexbio.enrichment.rest.model.ErrorResponse;
import org.ndexbio.ndexsearch.App;
import org.ndexbio.ndexsearch.rest.engine.SearchEngine;
import org.ndexbio.ndexsearch.rest.exceptions.SearchException;
import org.ndexbio.ndexsearch.rest.model.SourceResult;
import org.ndexbio.ndexsearch.rest.model.SourceResults;

/**
 *
 * @author churas
 */
public class TestSearchSource {
    public Dispatcher dispatcher = MockDispatcherFactory.createDispatcher();
    
    @Rule
    public TemporaryFolder _folder= new TemporaryFolder();
    
    /**
     * Little helper method to write out a configuration file and set it as
     * config file for Configuration object
     * @param tempDir
     * @throws Exception 
     */
    protected void reloadConfiguration(File tempDir) throws Exception {
        File confFile = new File(tempDir.getAbsolutePath() + File.separator + "my.conf");
        FileWriter fw = new FileWriter(confFile);
        fw.write(App.generateExampleConfiguration());
        fw.flush();
        fw.close();
        Configuration.setAlternateConfigurationFile(confFile.getAbsolutePath());
    }
    
    @Test
    public void testGetSourceResultsNoSearchEngine() throws Exception {
        File tempDir = _folder.newFolder();
        try {
            reloadConfiguration(tempDir);
            Configuration.getInstance().setSearchEngine(null);

            Dispatcher dispatcher = MockDispatcherFactory.createDispatcher();
            dispatcher.getRegistry().addSingletonResource(new SearchSource());

            MockHttpRequest request = MockHttpRequest.get(URIHelper.removeDuplicateSlashes(Configuration.V_ONE_PATH + "/" +
                                                          SearchSource.SOURCE_PATH));
            MockHttpResponse response = new MockHttpResponse();
            dispatcher.invoke(request, response);
            assertEquals(500, response.getStatus());
            ObjectMapper mapper = new ObjectMapper();
            ErrorResponse er = mapper.readValue(response.getOutput(),
                    ErrorResponse.class);
            assertEquals("Configuration error", er.getMessage());
            assertTrue(er.getDescription().contains("SearchEngine is null, which is"));
        } finally {
            _folder.delete();
        }
    }
    
    @Test
    public void testGetSourceResultsReturnsNull() throws Exception {
        File tempDir = _folder.newFolder();
        try {
            reloadConfiguration(tempDir);
            
            // create mock search engine that returns null
            SearchEngine mockEngine = createMock(SearchEngine.class);
            expect(mockEngine.getSourceResults()).andReturn(null);
            replay(mockEngine);
            Configuration.getInstance().setSearchEngine(mockEngine);
            Dispatcher dispatcher = MockDispatcherFactory.createDispatcher();
            dispatcher.getRegistry().addSingletonResource(new SearchSource());

            MockHttpRequest request = MockHttpRequest.get(URIHelper.removeDuplicateSlashes(Configuration.V_ONE_PATH + "/" +
                                                          SearchSource.SOURCE_PATH));
            MockHttpResponse response = new MockHttpResponse();
            dispatcher.invoke(request, response);
            assertEquals(500, response.getStatus());
            ObjectMapper mapper = new ObjectMapper();
            ErrorResponse er = mapper.readValue(response.getOutput(),
                    ErrorResponse.class);
            assertEquals("No information found on sources", er.getMessage());
            assertTrue(er.getDescription().contains("SourceResults is null, which is most likely due to configuration error"));
            verify(mockEngine);
        } finally {
            _folder.delete();
            Configuration.getInstance().setSearchEngine(null);
        }
    }
    
    @Test
    public void testGetSourceResultsThrowsException() throws Exception {
        File tempDir = _folder.newFolder();
        try {
            reloadConfiguration(tempDir);
            
            // create mock search engine that throws exception
            SearchEngine mockEngine = createMock(SearchEngine.class);
            expect(mockEngine.getSourceResults()).andThrow(new SearchException("someerror"));
            replay(mockEngine);
            Configuration.getInstance().setSearchEngine(mockEngine);
            Dispatcher dispatcher = MockDispatcherFactory.createDispatcher();
            dispatcher.getRegistry().addSingletonResource(new SearchSource());

            MockHttpRequest request = MockHttpRequest.get(URIHelper.removeDuplicateSlashes(Configuration.V_ONE_PATH + "/" +
                                                          SearchSource.SOURCE_PATH));
            MockHttpResponse response = new MockHttpResponse();
            dispatcher.invoke(request, response);
            assertEquals(500, response.getStatus());
            ObjectMapper mapper = new ObjectMapper();
            ErrorResponse er = mapper.readValue(response.getOutput(),
                    ErrorResponse.class);
            assertEquals("Error querying for source information", er.getMessage());
            assertEquals("someerror", er.getDescription());
            verify(mockEngine);
        } finally {
            _folder.delete();
            Configuration.getInstance().setSearchEngine(null);
        }
    }
    
    @Test
    public void testGetSourceResultsSuccess() throws Exception {
        File tempDir = _folder.newFolder();
        try {
            reloadConfiguration(tempDir);
       
            // create mock search engine that returns null
            SearchEngine mockEngine = createMock(SearchEngine.class);
            SourceResults sr = new SourceResults();
           
            expect(mockEngine.getSourceResults()).andReturn(sr);
            replay(mockEngine);
            Configuration.getInstance().setSearchEngine(mockEngine);
            Dispatcher dispatcher = MockDispatcherFactory.createDispatcher();
            dispatcher.getRegistry().addSingletonResource(new SearchSource());

            MockHttpRequest request = MockHttpRequest.get(URIHelper.removeDuplicateSlashes(Configuration.V_ONE_PATH + "/" +
                                                          SearchSource.SOURCE_PATH));
            MockHttpResponse response = new MockHttpResponse();
            dispatcher.invoke(request, response);
            assertEquals(200, response.getStatus());
            ObjectMapper mapper = new ObjectMapper();
            SourceResults res = mapper.readValue(response.getOutput(),
                    SourceResults.class);
            assertTrue(res != null);
            verify(mockEngine);
        } finally {
            _folder.delete();
            Configuration.getInstance().setSearchEngine(null);
        }
    }
	
	@Test
    public void testGetSourceObjectsNoSearchEngine() throws Exception {
        File tempDir = _folder.newFolder();
        try {
            reloadConfiguration(tempDir);
            Configuration.getInstance().setSearchEngine(null);

            Dispatcher dispatcher = MockDispatcherFactory.createDispatcher();
            dispatcher.getRegistry().addSingletonResource(new SearchSource());
            String uuid = UUID.randomUUID().toString();
            MockHttpRequest request = MockHttpRequest.get(URIHelper.removeDuplicateSlashes(Configuration.V_ONE_PATH + "/" +
                                                          SearchSource.SOURCE_PATH + "/" + uuid));
            MockHttpResponse response = new MockHttpResponse();
            dispatcher.invoke(request, response);
            assertEquals(500, response.getStatus());
            ObjectMapper mapper = new ObjectMapper();
            ErrorResponse er = mapper.readValue(response.getOutput(),
                    ErrorResponse.class);
            assertEquals("Configuration error", er.getMessage());
            assertTrue(er.getDescription().contains("SearchEngine is null, which is"));
        } finally {
            _folder.delete();
        }
    }
    
    @Test
    public void testGetSourceObjectsNoSourceResults() throws Exception {
        File tempDir = _folder.newFolder();
        try {
            reloadConfiguration(tempDir);
            
             // create mock search engine that returns null
            SearchEngine mockEngine = createMock(SearchEngine.class);
            SourceResults sr = new SourceResults();
           
            expect(mockEngine.getSourceResults()).andReturn(sr);
            replay(mockEngine);
            Configuration.getInstance().setSearchEngine(mockEngine);

            Dispatcher dispatcher = MockDispatcherFactory.createDispatcher();
            dispatcher.getRegistry().addSingletonResource(new SearchSource());
            String uuid = UUID.randomUUID().toString();
            MockHttpRequest request = MockHttpRequest.get(URIHelper.removeDuplicateSlashes(Configuration.V_ONE_PATH + "/" +
                                                          SearchSource.SOURCE_PATH + "/" + uuid));
            MockHttpResponse response = new MockHttpResponse();
            dispatcher.invoke(request, response);
            assertEquals(500, response.getStatus());
            ObjectMapper mapper = new ObjectMapper();
            ErrorResponse er = mapper.readValue(response.getOutput(),
                    ErrorResponse.class);
            assertEquals("Error querying for source information", er.getMessage());
            assertNull(er.getDescription());
        } finally {
            _folder.delete();
        }
    }
    
    @Test
    public void testGetSourceObjectsNoMatchingService() throws Exception {
        File tempDir = _folder.newFolder();
        try {
            reloadConfiguration(tempDir);
            
             // create mock search engine that returns null
            SearchEngine mockEngine = createMock(SearchEngine.class);
            SourceResults sr = new SourceResults();
            List<SourceResult> sRes = new ArrayList<>();
            SourceResult srOne = new SourceResult();
            srOne.setName("notmatching");
            sRes.add(srOne);
            sr.setResults(sRes);
           
            expect(mockEngine.getSourceResults()).andReturn(sr);
            replay(mockEngine);
            Configuration.getInstance().setSearchEngine(mockEngine);

            Dispatcher dispatcher = MockDispatcherFactory.createDispatcher();
            dispatcher.getRegistry().addSingletonResource(new SearchSource());
            String uuid = UUID.randomUUID().toString();
            MockHttpRequest request = MockHttpRequest.get(URIHelper.removeDuplicateSlashes(Configuration.V_ONE_PATH + "/" +
                                                          SearchSource.SOURCE_PATH + "/" + uuid));
            MockHttpResponse response = new MockHttpResponse();
            dispatcher.invoke(request, response);
            assertEquals(500, response.getStatus());
            ObjectMapper mapper = new ObjectMapper();
            ErrorResponse er = mapper.readValue(response.getOutput(),
                    ErrorResponse.class);
            assertEquals("Error querying for source information", er.getMessage());
            assertTrue(er.getDescription().contains("UUID did not match a service"));
        } finally {
            _folder.delete();
        }
    }
    
    @Test
    public void testGetSourceObjectsServiceNotFound() throws Exception {
        File tempDir = _folder.newFolder();
        try {
            reloadConfiguration(tempDir);
            
            String uuid = UUID.randomUUID().toString();
             // create mock search engine that returns null
            SearchEngine mockEngine = createMock(SearchEngine.class);
            SourceResults sr = new SourceResults();
            List<SourceResult> sRes = new ArrayList<>();
            SourceResult srOne = new SourceResult();
            srOne.setName("theservice");
            srOne.setUuid(uuid);
            sRes.add(srOne);
            sr.setResults(sRes);
           
            expect(mockEngine.getSourceResults()).andReturn(sr);
            replay(mockEngine);
            Configuration.getInstance().setSearchEngine(mockEngine);

            Dispatcher dispatcher = MockDispatcherFactory.createDispatcher();
            dispatcher.getRegistry().addSingletonResource(new SearchSource());
            
            MockHttpRequest request = MockHttpRequest.get(URIHelper.removeDuplicateSlashes(Configuration.V_ONE_PATH + "/" +
                                                          SearchSource.SOURCE_PATH + "/" + uuid));
            MockHttpResponse response = new MockHttpResponse();
            dispatcher.invoke(request, response);
            assertEquals(500, response.getStatus());
            ObjectMapper mapper = new ObjectMapper();
            ErrorResponse er = mapper.readValue(response.getOutput(),
                    ErrorResponse.class);
            assertEquals("Error querying for source information", er.getMessage());
            assertTrue(er.getDescription().contains("Service not found"));
        } finally {
            _folder.delete();
        }
    }
    
    @Test
    public void testGetSourceObjectsGetEnrichment() throws Exception {
        File tempDir = _folder.newFolder();
        try {
            reloadConfiguration(tempDir);
            
            String uuid = UUID.randomUUID().toString();
             // create mock search engine that returns null
            SearchEngine mockEngine = createMock(SearchEngine.class);
            SourceResults sr = new SourceResults();
            List<SourceResult> sRes = new ArrayList<>();
            SourceResult srOne = new SourceResult();
            srOne.setName(SourceResult.ENRICHMENT_SERVICE);
            srOne.setUuid(uuid);
            sRes.add(srOne);
            sr.setResults(sRes);
           
            expect(mockEngine.getSourceResults()).andReturn(sr);
            
            DatabaseResults dr = new DatabaseResults();
            expect(mockEngine.getEnrichmentDatabases()).andReturn(dr);
            replay(mockEngine);
            Configuration.getInstance().setSearchEngine(mockEngine);

            Dispatcher dispatcher = MockDispatcherFactory.createDispatcher();
            dispatcher.getRegistry().addSingletonResource(new SearchSource());
            
            MockHttpRequest request = MockHttpRequest.get(URIHelper.removeDuplicateSlashes(Configuration.V_ONE_PATH + "/" +
                                                          SearchSource.SOURCE_PATH + "/" + uuid));
            MockHttpResponse response = new MockHttpResponse();
            dispatcher.invoke(request, response);
            assertEquals(200, response.getStatus());
            ObjectMapper mapper = new ObjectMapper();
            DatabaseResults res = mapper.readValue(response.getOutput(),
                    DatabaseResults.class);
            assertNotNull(res);
            
        } finally {
            _folder.delete();
        }
    }
}
