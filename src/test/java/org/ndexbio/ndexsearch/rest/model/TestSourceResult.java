/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ndexbio.ndexsearch.rest.model;

import java.util.LinkedList;
import org.junit.Test;
import static org.junit.Assert.*;
/**
 *
 * @author churas
 */
public class TestSourceResult {
    
    @Test
    public void testGettersAndSetters(){
        SourceResult sr = new SourceResult();
        assertNull(sr.getDatabases());
        assertNull(sr.getDescription());
        assertNull(sr.getEndPoint());
        assertNull(sr.getName());
        assertEquals(0, sr.getNumberOfNetworks());
        assertNull(sr.getStatus());
        assertNull(sr.getUuid());
        assertNull(sr.getVersion());
        
        sr.setDatabases(new LinkedList<DatabaseResult>());
        sr.setDescription("description");
        sr.setEndPoint("endpoint");
        sr.setName("name");
        sr.setNumberOfNetworks(1);
        sr.setStatus("status");
        sr.setUuid("uuid");
        sr.setVersion("version");
        
        assertEquals(0, sr.getDatabases().size());
        assertEquals("description", sr.getDescription());
        assertEquals("endpoint", sr.getEndPoint());
        assertEquals("name", sr.getName());
        assertEquals(1, sr.getNumberOfNetworks());
        assertEquals("status", sr.getStatus());
        assertEquals("uuid", sr.getUuid());
        assertEquals("version", sr.getVersion());
        
        
    }
}
