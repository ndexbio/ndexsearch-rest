/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ndexbio.ndexsearch.rest.model;

import java.util.LinkedList;
import org.junit.Test;
import static org.junit.Assert.*;
import org.ndexbio.enrichment.rest.model.DatabaseResult;
/**
 *
 * @author churas
 */
public class TestSourceConfiguration {
    
    @Test
    public void testGettersAndSetters(){
        SourceConfiguration sc = new SourceConfiguration();
       
        assertNull(sc.getDescription());
        assertNull(sc.getEndPoint());
        assertNull(sc.getName());
     
        sc.setDescription("description");
        sc.setEndPoint("endpoint");
        sc.setName("name");
        
        assertEquals("description", sc.getDescription());
        assertEquals("endpoint", sc.getEndPoint());
        assertEquals("name", sc.getName());
        
    }
}
