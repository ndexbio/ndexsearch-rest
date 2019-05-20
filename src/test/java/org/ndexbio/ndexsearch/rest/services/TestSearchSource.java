/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ndexbio.ndexsearch.rest.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.FileWriter;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import org.jboss.resteasy.core.Dispatcher;
import org.jboss.resteasy.mock.MockDispatcherFactory;
import org.jboss.resteasy.mock.MockHttpRequest;
import org.jboss.resteasy.mock.MockHttpResponse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.ndexbio.enrichment.rest.model.ErrorResponse;
import org.ndexbio.ndexsearch.App;
import org.ndexbio.ndexsearch.rest.engine.SearchEngine;
import org.ndexbio.ndexsearch.rest.exceptions.SearchException;
import org.ndexbio.ndexsearch.rest.model.SourceResults;

/**
 *
 * @author churas
 */
public class TestSearchSource {
    public Dispatcher dispatcher = MockDispatcherFactory.createDispatcher();
    
    @Rule
    public TemporaryFolder _folder= new TemporaryFolder();
    
    @Test
    public void testGetSourceResultsNoSearchEngine() throws Exception {
        File tempDir = _folder.newFolder();
        try {
            File confFile = new File(tempDir.getAbsolutePath() + File.separator + "my.conf");
            FileWriter fw = new FileWriter(confFile);
            fw.write(App.generateExampleConfiguration());
            fw.flush();
            fw.close();
            Configuration.setAlternateConfigurationFile(confFile.getAbsolutePath());
            Configuration.reloadConfiguration();
            Configuration.getInstance().setSearchEngine(null);

            Dispatcher dispatcher = MockDispatcherFactory.createDispatcher();
            dispatcher.getRegistry().addSingletonResource(new SearchSource());

            MockHttpRequest request = MockHttpRequest.get(URIHelper.removeDuplicateSlashes(Configuration.REST_PATH + "/" +
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
            File confFile = new File(tempDir.getAbsolutePath() + File.separator + "my.conf");
            FileWriter fw = new FileWriter(confFile);
            fw.write(App.generateExampleConfiguration());
            fw.flush();
            fw.close();
            Configuration.setAlternateConfigurationFile(confFile.getAbsolutePath());
            Configuration.reloadConfiguration();
            
            // create mock search engine that returns null
            SearchEngine mockEngine = createMock(SearchEngine.class);
            expect(mockEngine.getSourceResults()).andReturn(null);
            replay(mockEngine);
            Configuration.getInstance().setSearchEngine(mockEngine);
            Dispatcher dispatcher = MockDispatcherFactory.createDispatcher();
            dispatcher.getRegistry().addSingletonResource(new SearchSource());

            MockHttpRequest request = MockHttpRequest.get(URIHelper.removeDuplicateSlashes(Configuration.REST_PATH + "/" +
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
            File confFile = new File(tempDir.getAbsolutePath() + File.separator + "my.conf");
            FileWriter fw = new FileWriter(confFile);
            fw.write(App.generateExampleConfiguration());
            fw.flush();
            fw.close();
            Configuration.setAlternateConfigurationFile(confFile.getAbsolutePath());
            Configuration.reloadConfiguration();
            
            // create mock search engine that throws exception
            SearchEngine mockEngine = createMock(SearchEngine.class);
            expect(mockEngine.getSourceResults()).andThrow(new SearchException("someerror"));
            replay(mockEngine);
            Configuration.getInstance().setSearchEngine(mockEngine);
            Dispatcher dispatcher = MockDispatcherFactory.createDispatcher();
            dispatcher.getRegistry().addSingletonResource(new SearchSource());

            MockHttpRequest request = MockHttpRequest.get(URIHelper.removeDuplicateSlashes(Configuration.REST_PATH + "/" +
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
            File confFile = new File(tempDir.getAbsolutePath() + File.separator + "my.conf");
            FileWriter fw = new FileWriter(confFile);
            fw.write(App.generateExampleConfiguration());
            fw.flush();
            fw.close();
            Configuration.setAlternateConfigurationFile(confFile.getAbsolutePath());
            Configuration.reloadConfiguration();
       
            // create mock search engine that returns null
            SearchEngine mockEngine = createMock(SearchEngine.class);
            SourceResults sr = new SourceResults();
           
            expect(mockEngine.getSourceResults()).andReturn(sr);
            replay(mockEngine);
            Configuration.getInstance().setSearchEngine(mockEngine);
            Dispatcher dispatcher = MockDispatcherFactory.createDispatcher();
            dispatcher.getRegistry().addSingletonResource(new SearchSource());

            MockHttpRequest request = MockHttpRequest.get(URIHelper.removeDuplicateSlashes(Configuration.REST_PATH + "/" +
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
}
