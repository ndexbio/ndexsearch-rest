/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ndexbio.ndexsearch.rest.engine;

import java.io.InputStream;
import org.ndexbio.ndexsearch.rest.exceptions.EnrichmentException;
import org.ndexbio.ndexsearch.rest.model.DatabaseResults;
import org.ndexbio.ndexsearch.rest.model.EnrichmentQuery;
import org.ndexbio.ndexsearch.rest.model.EnrichmentQueryResults;
import org.ndexbio.ndexsearch.rest.model.EnrichmentQueryStatus;

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
    public String query(EnrichmentQuery query) throws EnrichmentException;
    
    /**
     * Gets a summary of databases in engine
     * @return DatabaseResults object
     * @throws EnrichmentException if there is an error
     */
    public DatabaseResults getDatabaseResults() throws EnrichmentException;
    
    /**
     * Gets query results
     * @param id
     * @param start
     * @param size
     * @return
     * @throws EnrichmentException  if there is an error
     */
    public EnrichmentQueryResults getQueryResults(final String id, int start, int size) throws EnrichmentException;
    
    
    /**
     * Gets query status
     * @param id
     * @return
     * @throws EnrichmentException if there is an error
     */
    public EnrichmentQueryStatus getQueryStatus(final String id) throws EnrichmentException;
    
    /**
     * Deletes query
     * @param id
     * @throws EnrichmentException if there is an error
     */
    public void delete(final String id) throws EnrichmentException;
    
    /**
     * Gets a network as CX
     * @param id
     * @param databaseUUID
     * @param networkUUID
     * @return
     * @throws EnrichmentException 
     */
    public InputStream getNetworkOverlayAsCX(final String id, final String databaseUUID, final String networkUUID) throws EnrichmentException;

    
    /**
     * Tells implementing objects to shutdown
     */
    public void shutdown();
    
}
