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
public class TestQuery {
    
    @Test
    public void testGettersAndSetters(){
        Query q = new Query();
        assertNull(q.getGeneList());
        assertNull(q.getSourceList());
        q.setGeneList(Arrays.asList("1","2"));
        q.setSourceList(Arrays.asList("3"));
        assertEquals(2, q.getGeneList().size());
        assertEquals("1", q.getGeneList().get(0));
        assertEquals("2", q.getGeneList().get(1));
        assertEquals(1, q.getSourceList().size());
        assertEquals("3", q.getSourceList().get(0));
    }
}
