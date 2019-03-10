/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ndexbio.ndexsearch.rest.model;

import java.util.Arrays;
import java.util.HashSet;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author churas
 */
public class TestSourceQueryResult {
    
    @Test
    public void testGettersAndSetters(){
        SourceQueryResult sqr = new SourceQueryResult();
        assertNull(sqr.getDescription());
        assertEquals(0, sqr.getEdges());
        assertNull(sqr.getHitGenes());
        assertNull(sqr.getNetworkUUID());
        assertEquals(0, sqr.getNodes());
        assertEquals(0, sqr.getPercentOverlap());
        assertEquals(0, sqr.getRank());
        
        sqr.setDescription("description");
        sqr.setEdges(1);
        sqr.setHitGenes(new HashSet<String>(Arrays.asList("gene")));
        sqr.setNetworkUUID("uuid");
        sqr.setNodes(2);
        sqr.setPercentOverlap(3);
        sqr.setRank(4);
        assertEquals("description", sqr.getDescription());
        assertEquals(1, sqr.getEdges());
        assertTrue(sqr.getHitGenes().contains("gene"));
        assertEquals("uuid", sqr.getNetworkUUID());
        assertEquals(2, sqr.getNodes());
        assertEquals(3, sqr.getPercentOverlap());
        assertEquals(4, sqr.getRank());
        
    }
}
