/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ndexbio.ndexsearch.rest.model;

import java.io.IOException;
import java.util.UUID;

import org.junit.Test;
import static org.junit.Assert.*;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
/**
 *
 * @author churas
 */
public class TestSourceConfiguration {
    
    @Test
    public void testGettersAndSetters() throws JsonParseException, JsonMappingException, IOException{
        SourceConfiguration sc = new SourceConfiguration();
       
        assertNull(sc.getDescription());
        assertNull(sc.getEndPoint());
        assertNull(sc.getName());
        assertNull(sc.getUuid());
     
        sc.setDescription("description");
        sc.setEndPoint("endpoint");
        sc.setName("name");
        sc.setUuid("1857a397-3453-4ae4-8208-e33a283c85ec");
        
        assertEquals("description", sc.getDescription());
        assertEquals("endpoint", sc.getEndPoint());
        assertEquals("name", sc.getName());
        assertEquals (UUID.fromString("1857a397-3453-4ae4-8208-e33a283c85ec"), sc.getUuid());
        
        ObjectMapper om = new ObjectMapper();
        sc = om.readValue("{\n" + 
        		"    \"description\" : \"description1\",\n" + 
        		"    \"endPoint\" : \"endpoint1\",\n" + 
        		"    \"name\" : \"name1\",\n" + 
        		"    \"uuid\" : \"1857a397-3453-4ae4-8208-e33a283c85ec\"\n" + 
        		"  }", SourceConfiguration.class);
        
        assertEquals("description1", sc.getDescription());
        assertEquals("endpoint1", sc.getEndPoint());
        assertEquals("name1", sc.getName());
        assertEquals (UUID.fromString("1857a397-3453-4ae4-8208-e33a283c85ec"), sc.getUuid());
        
        String s = om.writeValueAsString(sc);
        sc = om.readValue(s, SourceConfiguration.class);
        
        assertEquals("description1", sc.getDescription());
        assertEquals("endpoint1", sc.getEndPoint());
        assertEquals("name1", sc.getName());
        assertEquals (UUID.fromString("1857a397-3453-4ae4-8208-e33a283c85ec"), sc.getUuid());
        
        
    }
}
