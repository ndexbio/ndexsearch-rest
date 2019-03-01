/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ndexbio.ndexsearch.rest.engine;

import java.io.InputStream;
import org.ndexbio.ndexsearch.rest.exceptions.SearchException;
import org.ndexbio.ndexsearch.rest.model.SourceResults;
import org.ndexbio.ndexsearch.rest.model.Query;
import org.ndexbio.ndexsearch.rest.model.QueryResults;
import org.ndexbio.ndexsearch.rest.model.QueryStatus;

/**
 *
 * @author churas
 */
public interface SearchEngine extends Runnable {
    
    /**
     * Submits query for processing
     * @param query query to process
     * @return UUID as a string that is an identifier for query
     */
    public String query(Query query) throws SearchException;
    
    /**
     * Gets a summary of databases in engine
     * @return SourceResults object
     * @throws SearchException if there is an error
     */
    public SourceResults getSourceResults() throws SearchException;
    
    /**
     * Gets query results
     * @param id
     * @param start
     * @param size
     * @return
     * @throws SearchException  if there is an error
     */
    public QueryResults getQueryResults(final String id, final String source, int start, int size) throws SearchException;
    
    
    /**
     * Gets query status
     * @param id
     * @return
     * @throws SearchException if there is an error
     */
    public QueryStatus getQueryStatus(final String id) throws SearchException;
    
    /**
     * Deletes query
     * @param id
     * @throws SearchException if there is an error
     */
    public void delete(final String id) throws SearchException;
    
    /**
     * Gets a network as CX
     * @param id
     * @param databaseUUID
     * @param networkUUID
     * @return
     * @throws SearchException 
     */
    public InputStream getNetworkOverlayAsCX(final String id, final String sourceUUID, final String networkUUID) throws SearchException;

    
    /**
     * Tells implementing objects to shutdown
     */
    public void shutdown();
    
}
