/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ndexbio.ndexsearch.rest.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.FileWriter;
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

/**
 *
 * @author churas
 */
public class TestSearchSource {
    public Dispatcher dispatcher = MockDispatcherFactory.createDispatcher();
    
    @Rule
    public TemporaryFolder _folder= new TemporaryFolder();
    
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
}
