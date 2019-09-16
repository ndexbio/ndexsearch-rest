/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ndexbio.ndexsearch.rest.model.comparators;

import static org.junit.Assert.*;
import org.junit.Test;
import org.ndexbio.ndexsearch.rest.model.SourceQueryResult;
import org.ndexbio.ndexsearch.rest.model.SourceQueryResults;
/**
 *
 * @author churas
 */
public class TestSourceQueryResultsBySourceRank {
    
   // commenting out these tests because these errors can be detected in compile time now. 
/*	@Test
    public void testSortingWithNullParameters(){
        SourceQueryResultsBySourceRank sorter = new SourceQueryResultsBySourceRank();
        assertEquals(0, sorter.compare(null, null));
        assertEquals(-1, sorter.compare(new SourceQueryResult(), null));
        assertEquals(1, sorter.compare(null, new SourceQueryResult()));
    }

    @Test
    public void testClassCastException(){
        SourceQueryResultsBySourceRank sorter = new SourceQueryResultsBySourceRank();
        try {
            sorter.compare(new Integer(4), new SourceQueryResults());
            fail("Expected ClassCastException");
        } catch(ClassCastException cce){
            assertEquals("o1 is not of type SourceQueryResults", cce.getMessage());
        }
        try {
            sorter.compare(new SourceQueryResults(), new Integer(5));
            fail("Expected ClassCastException");
        } catch(ClassCastException cce){
            assertEquals("o2 is not of type SourceQueryResults", cce.getMessage());
        }
    } */
    
    @Test
    public void testVariousRanks(){
        SourceQueryResultsBySourceRank sorter = new SourceQueryResultsBySourceRank();
        SourceQueryResults o1 = new SourceQueryResults();
        o1.setSourceRank(0);
        
        SourceQueryResults o2 = new SourceQueryResults();
        o1.setSourceRank(0);
        
        // equals
        assertEquals(0, sorter.compare(o1, o2));
        
        o1.setSourceRank(1);
        o2.setSourceRank(2);
        //o1 less then o2
        assertEquals(-1, sorter.compare(o1, o2));
        //flip o1 is still less then o2, but put as 2nd argument
        assertEquals(1, sorter.compare(o2, o1));
        
    }

    
}
