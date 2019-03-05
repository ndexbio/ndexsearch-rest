/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ndexbio.ndexsearch.rest.model.comparators;

import static org.junit.Assert.*;
import org.junit.Test;
import org.ndexbio.ndexsearch.rest.model.SourceQueryResult;
/**
 *
 * @author churas
 */
public class TestSourceQueryResultByRank {
    
    @Test
    public void testSortingWithNullParameters(){
        SourceQueryResultByRank sorter = new SourceQueryResultByRank();
        assertEquals(0, sorter.compare(null, null));
        assertEquals(-1, sorter.compare(new SourceQueryResult(), null));
        assertEquals(1, sorter.compare(null, new SourceQueryResult()));
    }

    @Test
    public void testClassCastException(){
        SourceQueryResultByRank sorter = new SourceQueryResultByRank();
        try {
            sorter.compare(new Integer(4), new SourceQueryResult());
            fail("Expected ClassCastException");
        } catch(ClassCastException cce){
            assertEquals("o1 is not of type SourceQueryResult", cce.getMessage());
        }
        try {
            sorter.compare(new SourceQueryResult(), new Integer(5));
            fail("Expected ClassCastException");
        } catch(ClassCastException cce){
            assertEquals("o2 is not of type SourceQueryResult", cce.getMessage());
        }
    }
    
    @Test
    public void testVariousRanks(){
        SourceQueryResultByRank sorter = new SourceQueryResultByRank();
        SourceQueryResult o1 = new SourceQueryResult();
        o1.setRank(0);
        
        SourceQueryResult o2 = new SourceQueryResult();
        o1.setRank(0);
        
        // equals
        assertEquals(0, sorter.compare(o1, o2));
        
        o1.setRank(1);
        o2.setRank(2);
        //o1 less then o2
        assertEquals(-1, sorter.compare(o1, o2));
        //flip o1 is still less then o2, but put as 2nd argument
        assertEquals(1, sorter.compare(o2, o1));
        
    }

    
}
