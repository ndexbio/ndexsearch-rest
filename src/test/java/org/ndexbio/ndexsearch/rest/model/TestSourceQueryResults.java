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
public class TestSourceQueryResults {
    
    @Test
    public void testGettersAndSetters(){
        SourceQueryResults sqrs = new SourceQueryResults();
        assertNull(sqrs.getMessage());
        assertEquals(0, sqrs.getNumberOfHits());
        assertEquals(0, sqrs.getProgress());
        assertNull(sqrs.getResults());
        assertNull(sqrs.getSourceName());
        assertEquals(0, sqrs.getSourceRank());
        assertEquals(0L, sqrs.getWallTime());
        assertNull(sqrs.getSourceUUID());
        assertNull(sqrs.getStatus());
        
        SourceQueryResult sq = new SourceQueryResult();
        sq.setDescription("desc");
        sqrs.setResults(Arrays.asList(sq));
        sqrs.setMessage("message");
        sqrs.setNumberOfHits(1);
        sqrs.setProgress(2);
        sqrs.setSourceName("source");
        sqrs.setSourceRank(3);
        sqrs.setWallTime(4L);
        sqrs.setSourceUUID("uuid");
        sqrs.setStatus("status");
        
        assertEquals("message", sqrs.getMessage());
        assertEquals(1, sqrs.getNumberOfHits());
        assertEquals(2, sqrs.getProgress());
        assertEquals("desc", sqrs.getResults().get(0).getDescription());
        assertEquals("source", sqrs.getSourceName());
        assertEquals(3, sqrs.getSourceRank());
        assertEquals(4L, sqrs.getWallTime());
        assertEquals("uuid", sqrs.getSourceUUID());
        assertEquals("status", sqrs.getStatus());
        
    }
}
