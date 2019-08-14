/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ndexbio.ndexsearch;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author churas
 */
public class TestApp {
    
    @Test
    public void testGenerateExampleConfiguration(){
        String res = App.generateExampleConfiguration();
        assertTrue(res.contains("# Example configuration file for Search service"));
        assertTrue(res.contains("NDExSearch/1.0"));
    }
    
    @Test
    public void testGenerateExampleSourceConfiguration() throws Exception {
        String res = App.generateExampleSourceConfigurations();
        assertTrue(res.contains("http://localhost:8095/enrichment"));  
    }
    
    @Test
    public void testExampleModes(){
        String[] args = {"--mode", App.EXAMPLE_CONF_MODE};
        App.main(args);
    }
}
