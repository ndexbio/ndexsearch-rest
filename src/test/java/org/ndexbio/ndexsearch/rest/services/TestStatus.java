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
import org.jboss.resteasy.mock.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.ndexbio.ndexsearch.App;

import org.ndexbio.ndexsearch.rest.model.ServerStatus;

/**
 *
 * @author churas
 */
public class TestStatus {
    
    public Dispatcher dispatcher = MockDispatcherFactory.createDispatcher();
    
    @Rule
    public TemporaryFolder _folder= new TemporaryFolder();
    
    public TestStatus() {
    }
  
    @Test
    public void testGetSuccess() throws Exception {
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
            dispatcher.getRegistry().addSingletonResource(new Status());

            MockHttpRequest request = MockHttpRequest.get(URIHelper.removeDuplicateSlashes(Configuration.REST_PATH + "/" + Status.STATUS_PATH));

            MockHttpResponse response = new MockHttpResponse();
            dispatcher.invoke(request, response);
            assertEquals(200, response.getStatus());
            ObjectMapper mapper = new ObjectMapper();
            ServerStatus ss = mapper.readValue(response.getOutput(),
                    ServerStatus.class);
            assertTrue(ss.getRestVersion() != null);
        } finally {
            _folder.delete();
        }
    }
    
}
