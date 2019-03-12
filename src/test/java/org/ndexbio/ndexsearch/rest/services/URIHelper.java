/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ndexbio.ndexsearch.rest.services;

/**
 *
 * @author churas
 */
public class URIHelper {
    
    public static String removeDuplicateSlashes(final String path){
        return path.replaceAll("/+", "/");
    }
 
}
