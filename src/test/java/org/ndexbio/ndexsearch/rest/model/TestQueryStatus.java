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
public class TestQueryStatus {
    
    @Test
    public void testUpdateStartTimeNullArgument(){
       QueryStatus qs = new QueryStatus();
       qs.setStartTime(3L);
       assertEquals(3L, qs.getStartTime());
       qs.updateStartTime(null);
       assertEquals(3L, qs.getStartTime());
    }
    
    @Test
    public void testUpdateStartTimeNewStartTimeSmaller(){
       QueryStatus qs = new QueryStatus();
       qs.setStartTime(3L);
       assertEquals(3L, qs.getStartTime());
       QueryStatus newQs = new QueryStatus();
       newQs.setStartTime(2L);
       qs.updateStartTime(newQs);
       assertEquals(3L, qs.getStartTime());
    }
    
    @Test
    public void testQueryResultsCopyConstructor(){
        
        // null test
        QueryStatus qs = new QueryStatus(null);
        assertEquals(0, qs.getNumberOfHits());
        
        QueryResults qr = new QueryResults();
        qr.setInputSourceList(Arrays.asList("input1"));
        qr.setMessage("message1");
        qr.setNumberOfHits(1);
        qr.setProgress(2);
        qr.setQuery(Arrays.asList("query1"));
        qr.setSize(3);
        qr.setSource("source1");
        qr.setStart(4);
        qr.setStartTime(5L);
        qr.setStatus("status1");
        qr.setWallTime(6L);
        QueryStatus newQs = new QueryStatus(qr);
        assertEquals(1, newQs.getInputSourceList().size());
        assertEquals("input1", newQs.getInputSourceList().get(0));
        assertEquals("message1", newQs.getMessage());
        assertEquals(1, newQs.getNumberOfHits());
        assertEquals(2, newQs.getProgress());
        assertEquals(1, newQs.getQuery().size());
        assertEquals("query1", newQs.getQuery().get(0));
        assertEquals(3, newQs.getSize());
        assertEquals(4, newQs.getStart());
        assertEquals(5L , newQs.getStartTime());
        assertEquals("status1", newQs.getStatus());
        assertEquals(6L, newQs.getWallTime());
        
        
    }
}
