/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ndexbio.ndexsearch.rest.engine;

import java.io.InputStream;
import java.util.HashSet;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicReference;
import org.ndexbio.ndexsearch.rest.exceptions.SearchException;
import org.ndexbio.ndexsearch.rest.model.SourceResults;
import org.ndexbio.ndexsearch.rest.model.EnrichmentQuery;
import org.ndexbio.ndexsearch.rest.model.EnrichmentQueryResults;
import org.ndexbio.ndexsearch.rest.model.EnrichmentQueryStatus;

import org.ndexbio.rest.client.NdexRestClientModelAccessLayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Runs enrichment 
 * @author churas
 */
public class BasicSearchEngineImpl implements SearchEngine {

    public static final String EQR_JSON_FILE = "enrichmentqueryresults.json";
    
    static Logger _logger = LoggerFactory.getLogger(BasicSearchEngineImpl.class);

    private String _dbDir;
    private String _taskDir;
    private boolean _shutdown;
    
    /**
     * This should be a map of <query UUID> => EnrichmentQuery object
     */
    private ConcurrentHashMap<String, EnrichmentQuery> _queryTasks;
    
    private ConcurrentLinkedQueue<String> _queryTaskIds;
    
    /**
     * This should be a map of <query UUID> => EnrichmentQueryResults object
     */
    private ConcurrentHashMap<String, EnrichmentQueryResults> _queryResults;
        
    /**
     * This should be a map of <database UUID> => Map<Gene => Set of network UUIDs>
     */
    private ConcurrentHashMap<String, ConcurrentHashMap<String, HashSet<String>>> _databases;
    
    private AtomicReference<SourceResults> _databaseResults;
    private NdexRestClientModelAccessLayer _client;
    
    private long _threadSleep = 10;
    
    public BasicSearchEngineImpl(final String dbDir,
            final String taskDir,
            NdexRestClientModelAccessLayer client){
        _shutdown = false;
        _dbDir = dbDir;
        _taskDir = taskDir;
        _client = client;
        _queryTasks = new ConcurrentHashMap<>();
        _queryResults = new ConcurrentHashMap<>();
        _databaseResults = new AtomicReference<>();
        _queryTaskIds = new ConcurrentLinkedQueue<>();
    }
    
    /**
     * Sets milliseconds thread should sleep if no work needs to be done.
     * @param sleepTime 
     */
    public void updateThreadSleepTime(long sleepTime){
        _threadSleep = sleepTime;
    }

    protected void threadSleep(){
        try {
            Thread.sleep(_threadSleep);
        }
        catch(InterruptedException ie){

        }
    }
    
    /**
     * Processes any query tasks, looping until {@link #shutdown()} is invoked
     */
    @Override
    public void run() {
        while(_shutdown == false){
            String id = _queryTaskIds.poll();
            if (id == null){
                threadSleep();
                continue;
            }
            //processQuery(id,_queryTasks.remove(id));            
        }
        _logger.debug("Shutdown was invoked");
    }

    @Override
    public void shutdown() {
        _shutdown = true;
    }
    
    
    public void setDatabaseResults(SourceResults dr){
        _databaseResults.set(dr);
    }

    
    @Override
    public String query(EnrichmentQuery thequery) throws SearchException {
        
        if (thequery.getDatabaseList() == null || thequery.getDatabaseList().isEmpty()){
            throw new SearchException("No databases selected");
        }
        // @TODO get Jing's uuid generator code that can be a poormans cache
        String id = UUID.randomUUID().toString();
        _queryTasks.put(id, thequery);
        _queryTaskIds.add(id);
        EnrichmentQueryResults eqr = new EnrichmentQueryResults(System.currentTimeMillis());
        eqr.setStatus(EnrichmentQueryResults.SUBMITTED_STATUS);
        _queryResults.merge(id, eqr, (oldval, newval) -> newval.updateStartTime(oldval));        
        return id;
    }

    @Override
    public SourceResults getDatabaseResults() throws SearchException {
        return _databaseResults.get();
    }
    
    /**
     * Returns
     * @param id Id of the query. 
     * @param start starting index to return from. Starting index is 0.
     * @param size Number of results to return. If 0 means all from starting index so
     *             to get all set both {@code start} and {@code size} to 0.
     * @return {@link org.ndexbio.ndexsearch.rest.model.EnrichmentQueryResults} object
     *         or null if no result could be found. 
     * @throws SearchException If there was an error getting the results
     */
    @Override
    public EnrichmentQueryResults getQueryResults(String id, int start, int size) throws SearchException {
        return null;
    }

    @Override
    public EnrichmentQueryStatus getQueryStatus(String id) throws SearchException {
        return null;
    }

    @Override
    public void delete(String id) throws SearchException {
        _logger.debug("Deleting task " + id);
    }

    @Override
    public InputStream getNetworkOverlayAsCX(String id, String databaseUUID, String networkUUID) throws SearchException {
        
        return null;
    }
    
}
