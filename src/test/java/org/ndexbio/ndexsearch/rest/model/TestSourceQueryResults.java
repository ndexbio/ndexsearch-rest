/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ndexbio.ndexsearch.rest.model;

import java.util.Arrays;
import java.util.UUID;

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
        assertNull(sqrs.getSourceTaskId());
        
        SourceQueryResult sq = new SourceQueryResult();
        sq.setDescription("desc");
        sqrs.setResults(Arrays.asList(sq));
        sqrs.setMessage("message");
        sqrs.setNumberOfHits(1);
        sqrs.setProgress(2);
        sqrs.setSourceName("source");
        sqrs.setSourceRank(3);
        sqrs.setWallTime(4L);
        sqrs.setSourceUUID(UUID.fromString("1857a397-3453-4ae4-8208-e33a283c85ec"));
        sqrs.setStatus("status");
        sqrs.setSourceTaskId("abcde");
        
        assertEquals("message", sqrs.getMessage());
        assertEquals(1, sqrs.getNumberOfHits());
        assertEquals(2, sqrs.getProgress());
        assertEquals("desc", sqrs.getResults().get(0).getDescription());
        assertEquals("source", sqrs.getSourceName());
        assertEquals(3, sqrs.getSourceRank());
        assertEquals(4L, sqrs.getWallTime());
        assertEquals(UUID.fromString("1857a397-3453-4ae4-8208-e33a283c85ec"), sqrs.getSourceUUID());
        assertEquals("status", sqrs.getStatus());
        assertEquals("abcde",sqrs.getSourceTaskId());
        
    }
}
