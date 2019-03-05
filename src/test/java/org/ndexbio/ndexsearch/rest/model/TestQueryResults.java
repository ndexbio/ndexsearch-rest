/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ndexbio.ndexsearch.rest.model;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author churas
 */
public class TestQueryResults {
    
    
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
}
