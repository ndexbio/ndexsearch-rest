/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ndexbio.ndexsearch.rest;

import java.util.Set;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author churas
 */
public class TestSearchApplication {
    
    @Test
    public void testCreation(){
        SearchApplication sa = new SearchApplication();
        Set<Object> objSet = sa.getSingletons();
        assertEquals(0, objSet.size());
        
        Set<Class<?>> classSet = sa.getClasses();
        assertEquals(5, classSet.size());
    }
}
