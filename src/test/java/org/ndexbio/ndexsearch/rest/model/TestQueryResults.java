/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ndexbio.ndexsearch.rest.model;

import java.util.Arrays;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author churas
 */
public class TestQueryResults {
    
    
    @Test
    public void testGettersAndSetters(){
        QueryResults qr = new QueryResults();
        qr.setInputSourceList(Arrays.asList("input1"));
        qr.setMessage("message");
        qr.setNumberOfHits(1);
        qr.setProgress(2);
        qr.setQuery(Arrays.asList("query1"));
        qr.setSize(3);
        qr.setSource("source");
        SourceQueryResults sqr = new SourceQueryResults();
        sqr.setMessage("hi");
        qr.setSources(Arrays.asList(sqr));
        qr.setStart(4);
        qr.setStartTime(5L);
        qr.setStatus("status");
        qr.setWallTime(6L);
        
        assertEquals(1, qr.getInputSourceList().size());
        assertEquals("input1", qr.getInputSourceList().get(0));
        assertEquals("message", qr.getMessage());
        assertEquals(1, qr.getNumberOfHits());
        assertEquals(2, qr.getProgress());
        assertEquals(1, qr.getQuery().size());
        assertEquals("query1", qr.getQuery().get(0));
        assertEquals(3, qr.getSize());
        assertEquals("source", qr.getSource());
        assertEquals(1, qr.getSources().size());
        assertEquals("hi", qr.getSources().get(0).getMessage());
        assertEquals(4, qr.getStart());
        assertEquals(5L, qr.getStartTime());
        assertEquals("status", qr.getStatus());
        assertEquals(6L, qr.getWallTime());
                
    }
    
    @Test
    public void testUpdateStartTime(){
        QueryResults qr = new QueryResults();
        qr.setStartTime(1L);
        assertEquals(1L, qr.getStartTime());
        QueryResults newQr = new QueryResults();
        assertEquals(0L, newQr.getStartTime());
        newQr.updateStartTime(qr);
        assertEquals(1L, newQr.getStartTime());
    }
    
    @Test
    public void testOverloadedConstructor(){
        QueryResults qr = new QueryResults(2L);
        assertEquals(2L, qr.getStartTime());
    }
}
