package org.ndexbio.ndexsearch.rest.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;
import java.net.URI;
import java.util.Arrays;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import org.easymock.EasyMock;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.notNull;
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
import org.ndexbio.enrichment.rest.model.Task;
import org.ndexbio.ndexsearch.App;
import org.ndexbio.ndexsearch.rest.engine.SearchEngine;
import org.ndexbio.ndexsearch.rest.exceptions.SearchException;
import org.ndexbio.ndexsearch.rest.model.Query;
import org.ndexbio.ndexsearch.rest.model.QueryResults;
import org.ndexbio.ndexsearch.rest.model.QueryStatus;

/**
 *
 * @author churas
 */
public class TestSearch {
    public Dispatcher dispatcher = MockDispatcherFactory.createDispatcher();
    
    @Rule
    public TemporaryFolder _folder= new TemporaryFolder();
    
    @Test
    public void testRequestQueryNoSearchEngine() throws Exception {
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
            dispatcher.getRegistry().addSingletonResource(new Search());

            MockHttpRequest request = MockHttpRequest.post(Configuration.V_ONE_PATH);
            
            Query query = new Query();
            query.setGeneList(Arrays.asList("hi"));
            request.contentType(MediaType.APPLICATION_JSON);
            ObjectMapper omappy = new ObjectMapper();
            request.content(omappy.writeValueAsBytes(query));
            
            MockHttpResponse response = new MockHttpResponse();
            dispatcher.invoke(request, response);
            assertEquals(500, response.getStatus());
            ObjectMapper mapper = new ObjectMapper();
            ErrorResponse er = mapper.readValue(response.getOutput(),
                    ErrorResponse.class);
            assertEquals("Configuration error", er.getMessage());
            assertTrue(er.getDescription().contains("SearchEngine is null, which is most likely due to configuration error"));
        } finally {
            _folder.delete();
        }
    }
    
    @Test
    public void testRequestQueryIdFromQueryIsNull() throws Exception {
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
            expect(mockEngine.query(notNull())).andReturn(null);
            replay(mockEngine);
            Configuration.getInstance().setSearchEngine(mockEngine);

            Dispatcher dispatcher = MockDispatcherFactory.createDispatcher();
            dispatcher.getRegistry().addSingletonResource(new Search());

            MockHttpRequest request = MockHttpRequest.post(Configuration.V_ONE_PATH);
            
            Query query = new Query();
            query.setGeneList(Arrays.asList("hi"));
            request.contentType(MediaType.APPLICATION_JSON);
            ObjectMapper omappy = new ObjectMapper();
            request.content(omappy.writeValueAsBytes(query));
            
            MockHttpResponse response = new MockHttpResponse();
            dispatcher.invoke(request, response);
            assertEquals(500, response.getStatus());
            ObjectMapper mapper = new ObjectMapper();
            ErrorResponse er = mapper.readValue(response.getOutput(),
                    ErrorResponse.class);
            assertEquals("Error requesting search", er.getMessage());
            assertEquals("No id returned from search engine", er.getDescription());
            verify(mockEngine);
        } finally {
            _folder.delete();
        }
    }
    @Test
    public void testRequestQueryThrowsException() throws Exception {
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
            expect(mockEngine.query(notNull())).andThrow(new SearchException("yoyo"));
            replay(mockEngine);
            Configuration.getInstance().setSearchEngine(mockEngine);

            Dispatcher dispatcher = MockDispatcherFactory.createDispatcher();
            dispatcher.getRegistry().addSingletonResource(new Search());

            MockHttpRequest request = MockHttpRequest.post(Configuration.V_ONE_PATH);
            
            Query query = new Query();
            query.setGeneList(Arrays.asList("hi"));
            request.contentType(MediaType.APPLICATION_JSON);
            ObjectMapper omappy = new ObjectMapper();
            request.content(omappy.writeValueAsBytes(query));
            
            MockHttpResponse response = new MockHttpResponse();
            dispatcher.invoke(request, response);
            assertEquals(500, response.getStatus());
            ObjectMapper mapper = new ObjectMapper();
            ErrorResponse er = mapper.readValue(response.getOutput(),
                    ErrorResponse.class);
            assertEquals("Error requesting search", er.getMessage());
            assertEquals("yoyo", er.getDescription());
            
            verify(mockEngine);
        } finally {
            _folder.delete();
        }
    }
    
    @Test
    public void testRequestQuerySuccessWebURLUnset() throws Exception {
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
            expect(mockEngine.query(notNull())).andReturn("12345");
            replay(mockEngine);
            Configuration.getInstance().setSearchEngine(mockEngine);

            Dispatcher dispatcher = MockDispatcherFactory.createDispatcher();
            dispatcher.getRegistry().addSingletonResource(new Search());

            MockHttpRequest request = MockHttpRequest.post(Configuration.V_ONE_PATH);
            
            Query query = new Query();
            query.setGeneList(Arrays.asList("hi"));
            request.contentType(MediaType.APPLICATION_JSON);
            ObjectMapper omappy = new ObjectMapper();
            request.content(omappy.writeValueAsBytes(query));
            
            MockHttpResponse response = new MockHttpResponse();
            dispatcher.invoke(request, response);
            assertEquals(202, response.getStatus());
            ObjectMapper mapper = new ObjectMapper();
            Task t = mapper.readValue(response.getOutput(),
                    Task.class);
            assertEquals("12345", t.getId());
			assertEquals(null, t.getWebUrl());
            MultivaluedMap<String, Object> resmap = response.getOutputHeaders();
            assertEquals(new URI(Configuration.V_ONE_PATH + "/12345"), resmap.getFirst("Location"));
            
            verify(mockEngine);
        } finally {
            _folder.delete();
           Configuration.getInstance().setSearchEngine(null);

        }
    }
    
    @Test
    public void testgetQueryResultsNoSearchEngine() throws Exception {
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
            dispatcher.getRegistry().addSingletonResource(new Search());

            MockHttpRequest request = MockHttpRequest.get(Configuration.V_ONE_PATH + "/12345");
            
            MockHttpResponse response = new MockHttpResponse();
            dispatcher.invoke(request, response);
            assertEquals(500, response.getStatus());
            ObjectMapper mapper = new ObjectMapper();
            ErrorResponse er = mapper.readValue(response.getOutput(),
                    ErrorResponse.class);
            assertEquals("Configuration error", er.getMessage());
            assertTrue(er.getDescription().contains("SearchEngine is null, which is most likely due to configuration error"));
        } finally {
            _folder.delete();
        }
    }
    
    @Test
    public void testgetQueryResultsReturnsNull() throws Exception {
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
            expect(mockEngine.getQueryResults("12345", "source", 1, 2)).andReturn(null);
            replay(mockEngine);
            Configuration.getInstance().setSearchEngine(mockEngine);

            Dispatcher dispatcher = MockDispatcherFactory.createDispatcher();
            dispatcher.getRegistry().addSingletonResource(new Search());

            MockHttpRequest request = MockHttpRequest.get(Configuration.V_ONE_PATH + "/12345?source=source&start=1&size=2");
            
            MockHttpResponse response = new MockHttpResponse();
            dispatcher.invoke(request, response);
            assertEquals(410, response.getStatus());
            verify(mockEngine);
        } finally {
            _folder.delete();
            Configuration.getInstance().setSearchEngine(null);

        }
    }
    
    @Test
    public void testgetQueryResultsThrowsException() throws Exception {
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
            expect(mockEngine.getQueryResults("12345", null, 0, 0)).andThrow(new SearchException("hehe"));
            replay(mockEngine);
            Configuration.getInstance().setSearchEngine(mockEngine);

            Dispatcher dispatcher = MockDispatcherFactory.createDispatcher();
            dispatcher.getRegistry().addSingletonResource(new Search());

            MockHttpRequest request = MockHttpRequest.get(Configuration.V_ONE_PATH + "/12345");
            
            MockHttpResponse response = new MockHttpResponse();
            dispatcher.invoke(request, response);
            assertEquals(500, response.getStatus());
            ObjectMapper mapper = new ObjectMapper();
            ErrorResponse er = mapper.readValue(response.getOutput(),
                    ErrorResponse.class);
            assertEquals("Error querying for results", er.getMessage());
            assertEquals("hehe", er.getDescription());
        
            verify(mockEngine);
        } finally {
            _folder.delete();
            Configuration.getInstance().setSearchEngine(null);

        }
    }
    
    @Test
    public void testgetQueryResultsSuccess() throws Exception {
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
            QueryResults qr = new QueryResults();
            qr.setNumberOfHits(55);
            expect(mockEngine.getQueryResults("12345", null, 0, 0)).andReturn(qr);
            replay(mockEngine);
            Configuration.getInstance().setSearchEngine(mockEngine);

            Dispatcher dispatcher = MockDispatcherFactory.createDispatcher();
            dispatcher.getRegistry().addSingletonResource(new Search());

            MockHttpRequest request = MockHttpRequest.get(Configuration.V_ONE_PATH + "/12345");
            
            MockHttpResponse response = new MockHttpResponse();
            dispatcher.invoke(request, response);
            assertEquals(200, response.getStatus());
            ObjectMapper mapper = new ObjectMapper();
            QueryResults res = mapper.readValue(response.getOutput(),
                    QueryResults.class);
            assertEquals(55, res.getNumberOfHits());
        
            verify(mockEngine);
        } finally {
            _folder.delete();
            Configuration.getInstance().setSearchEngine(null);

        }
    }
   
    @Test
    public void testgetQueryStatusNoSearchEngine() throws Exception {
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
            dispatcher.getRegistry().addSingletonResource(new Search());

            MockHttpRequest request = MockHttpRequest.get(Configuration.V_ONE_PATH + "/12345/status");
            
            MockHttpResponse response = new MockHttpResponse();
            dispatcher.invoke(request, response);
            assertEquals(500, response.getStatus());
            ObjectMapper mapper = new ObjectMapper();
            ErrorResponse er = mapper.readValue(response.getOutput(),
                    ErrorResponse.class);
            assertEquals("Configuration error", er.getMessage());
            assertTrue(er.getDescription().contains("SearchEngine is null, which is most likely due to configuration error"));
        } finally {
            _folder.delete();
        }
    }
    
    @Test
    public void testgetQueryStatusReturnsNull() throws Exception {
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
            expect(mockEngine.getQueryStatus("12345")).andReturn(null);
            replay(mockEngine);
            Configuration.getInstance().setSearchEngine(mockEngine);

            Dispatcher dispatcher = MockDispatcherFactory.createDispatcher();
            dispatcher.getRegistry().addSingletonResource(new Search());

            MockHttpRequest request = MockHttpRequest.get(Configuration.V_ONE_PATH + "/12345/status");
            
            MockHttpResponse response = new MockHttpResponse();
            dispatcher.invoke(request, response);
            assertEquals(410, response.getStatus());
            verify(mockEngine);
        } finally {
            _folder.delete();
            Configuration.getInstance().setSearchEngine(null);

        }
    }
    
    @Test
    public void testgetQueryStatusThrowsException() throws Exception {
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
            expect(mockEngine.getQueryStatus("12345")).andThrow(new SearchException("hehe"));
            replay(mockEngine);
            Configuration.getInstance().setSearchEngine(mockEngine);

            Dispatcher dispatcher = MockDispatcherFactory.createDispatcher();
            dispatcher.getRegistry().addSingletonResource(new Search());

            MockHttpRequest request = MockHttpRequest.get(Configuration.V_ONE_PATH + "/12345/status");
            
            MockHttpResponse response = new MockHttpResponse();
            dispatcher.invoke(request, response);
            assertEquals(500, response.getStatus());
            ObjectMapper mapper = new ObjectMapper();
            ErrorResponse er = mapper.readValue(response.getOutput(),
                    ErrorResponse.class);
            assertEquals("Error querying for results", er.getMessage());
            assertEquals("hehe", er.getDescription());
        
            verify(mockEngine);
        } finally {
            _folder.delete();
            Configuration.getInstance().setSearchEngine(null);

        }
    }
    
    @Test
    public void testgetQueryStatusSuccess() throws Exception {
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
            QueryStatus qs = new QueryStatus();
            qs.setNumberOfHits(56);
            expect(mockEngine.getQueryStatus("12345")).andReturn(qs);
            replay(mockEngine);
            Configuration.getInstance().setSearchEngine(mockEngine);

            Dispatcher dispatcher = MockDispatcherFactory.createDispatcher();
            dispatcher.getRegistry().addSingletonResource(new Search());

            MockHttpRequest request = MockHttpRequest.get(Configuration.V_ONE_PATH + "/12345/status");
            
            MockHttpResponse response = new MockHttpResponse();
            dispatcher.invoke(request, response);
            assertEquals(200, response.getStatus());
            ObjectMapper mapper = new ObjectMapper();
            QueryStatus res = mapper.readValue(response.getOutput(),
                    QueryStatus.class);
            assertEquals(56, res.getNumberOfHits());
        
            verify(mockEngine);
        } finally {
            _folder.delete();
            Configuration.getInstance().setSearchEngine(null);

        }
    }
    
    @Test
    public void testDeleteNoSearchEngine() throws Exception {
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
            dispatcher.getRegistry().addSingletonResource(new Search());

            MockHttpRequest request = MockHttpRequest.delete(Configuration.V_ONE_PATH + "/12345");
            
            MockHttpResponse response = new MockHttpResponse();
            dispatcher.invoke(request, response);
            assertEquals(500, response.getStatus());
            ObjectMapper mapper = new ObjectMapper();
            ErrorResponse er = mapper.readValue(response.getOutput(),
                    ErrorResponse.class);
            assertEquals("Configuration error", er.getMessage());
            assertTrue(er.getDescription().contains("SearchEngine is null, which is most likely due to configuration error"));
        } finally {
            _folder.delete();
        }
    }
    
    @Test
    public void testDeleteThrowsException() throws Exception {
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
            mockEngine.delete("12345");
            EasyMock.expectLastCall().andThrow(new SearchException("hehe"));
            replay(mockEngine);
            Configuration.getInstance().setSearchEngine(mockEngine);

            Dispatcher dispatcher = MockDispatcherFactory.createDispatcher();
            dispatcher.getRegistry().addSingletonResource(new Search());

            MockHttpRequest request = MockHttpRequest.delete(Configuration.V_ONE_PATH + "/12345");
            
            MockHttpResponse response = new MockHttpResponse();
            dispatcher.invoke(request, response);
            assertEquals(500, response.getStatus());
            ObjectMapper mapper = new ObjectMapper();
            ErrorResponse er = mapper.readValue(response.getOutput(),
                    ErrorResponse.class);
            assertEquals("Error deleting search result", er.getMessage());
            assertEquals("hehe", er.getDescription());
        
            verify(mockEngine);
        } finally {
            _folder.delete();
            Configuration.getInstance().setSearchEngine(null);

        }
    }
    
    @Test
    public void testDeleteSuccess() throws Exception {
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
            mockEngine.delete("12345");
            replay(mockEngine);
            Configuration.getInstance().setSearchEngine(mockEngine);

            Dispatcher dispatcher = MockDispatcherFactory.createDispatcher();
            dispatcher.getRegistry().addSingletonResource(new Search());

            MockHttpRequest request = MockHttpRequest.delete(Configuration.V_ONE_PATH + "/12345");
            
            MockHttpResponse response = new MockHttpResponse();
            dispatcher.invoke(request, response);
            assertEquals(200, response.getStatus());
        
            verify(mockEngine);
        } finally {
            _folder.delete();
            Configuration.getInstance().setSearchEngine(null);

        }
    }

    @Test
    public void testGetOverlayNetworkNoSearchEngine() throws Exception {
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
            dispatcher.getRegistry().addSingletonResource(new Search());

            MockHttpRequest request = MockHttpRequest.get(Configuration.V_ONE_PATH + "/12345/overlaynetwork");
            
            MockHttpResponse response = new MockHttpResponse();
            dispatcher.invoke(request, response);
            assertEquals(500, response.getStatus());
            ObjectMapper mapper = new ObjectMapper();
            ErrorResponse er = mapper.readValue(response.getOutput(),
                    ErrorResponse.class);
            assertEquals("Configuration error", er.getMessage());
            assertTrue(er.getDescription().contains("SearchEngine is null, which is most likely due to configuration error"));
        } finally {
            _folder.delete();
        }
    }
    
    @Test
    public void testGetOverlayNetworkReturnsNull() throws Exception {
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
            expect(mockEngine.getNetworkOverlayAsCX("12345", "srcid", "netid")).andReturn(null);
            replay(mockEngine);
            Configuration.getInstance().setSearchEngine(mockEngine);

            Dispatcher dispatcher = MockDispatcherFactory.createDispatcher();
            dispatcher.getRegistry().addSingletonResource(new Search());

            MockHttpRequest request = MockHttpRequest.get(Configuration.V_ONE_PATH + "/12345/overlaynetwork?sourceUUID=srcid&networkUUID=netid");
            
            MockHttpResponse response = new MockHttpResponse();
            dispatcher.invoke(request, response);
            assertEquals(410, response.getStatus());
            verify(mockEngine);
        } finally {
            _folder.delete();
            Configuration.getInstance().setSearchEngine(null);

        }
    }
    
    @Test
    public void testGetOverlayNetworkThrowsException() throws Exception {
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
            expect(mockEngine.getNetworkOverlayAsCX("12345", "srcid", "netid")).andThrow(new SearchException("hehe"));
            replay(mockEngine);
            Configuration.getInstance().setSearchEngine(mockEngine);

            Dispatcher dispatcher = MockDispatcherFactory.createDispatcher();
            dispatcher.getRegistry().addSingletonResource(new Search());

            MockHttpRequest request = MockHttpRequest.get(Configuration.V_ONE_PATH + "/12345/overlaynetwork?sourceUUID=srcid&networkUUID=netid");
            
            MockHttpResponse response = new MockHttpResponse();
            dispatcher.invoke(request, response);
            assertEquals(500, response.getStatus());
            ObjectMapper mapper = new ObjectMapper();
            ErrorResponse er = mapper.readValue(response.getOutput(),
                    ErrorResponse.class);
            assertEquals("Error querying for overlay network", er.getMessage());
            assertEquals("hehe", er.getDescription());
        
            verify(mockEngine);
        } finally {
            _folder.delete();
            Configuration.getInstance().setSearchEngine(null);

        }
    }
    
    @Test
    public void testGetOverlayNetworkSuccess() throws Exception {
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
            String jsonStr = "{\"a\": \"b\"}";
            byte[] strAsByte = jsonStr.getBytes();
            ByteArrayInputStream iStream = new ByteArrayInputStream(strAsByte);
            expect(mockEngine.getNetworkOverlayAsCX("12345", "srcid", "netid")).andReturn(iStream);
            replay(mockEngine);
            Configuration.getInstance().setSearchEngine(mockEngine);

            Dispatcher dispatcher = MockDispatcherFactory.createDispatcher();
            dispatcher.getRegistry().addSingletonResource(new Search());

            MockHttpRequest request = MockHttpRequest.get(Configuration.V_ONE_PATH + "/12345/overlaynetwork?sourceUUID=srcid&networkUUID=netid");
            
            MockHttpResponse response = new MockHttpResponse();
            dispatcher.invoke(request, response);
            assertEquals(200, response.getStatus());
            assertEquals(jsonStr, response.getContentAsString());
            verify(mockEngine);
        } finally {
            _folder.delete();
            Configuration.getInstance().setSearchEngine(null);

        }
    }

}
