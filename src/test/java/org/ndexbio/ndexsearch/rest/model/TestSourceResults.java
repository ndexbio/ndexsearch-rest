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
public class TestSourceResults {
    
    @Test
    public void testShallowCopyConstructor(){
        SourceResults sr = new SourceResults(null);
        assertNull(sr.getResults());
        InternalSourceResults isr = new InternalSourceResults();
        sr = new SourceResults(isr);
        assertNull(sr.getResults());
        SourceResult sRes = new SourceResult();
        sRes.setName("hi");
        isr.setResults(Arrays.asList(sRes));
        sr = new SourceResults(isr);
        assertEquals(1, sr.getResults().size());
        assertEquals("hi", sr.getResults().get(0).getName());
        
        
    }
}
