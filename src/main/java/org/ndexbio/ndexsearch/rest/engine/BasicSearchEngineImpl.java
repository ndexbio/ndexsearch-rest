/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ndexbio.ndexsearch.rest.engine;

import java.io.InputStream;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicReference;
import org.ndexbio.enrichment.rest.client.EnrichmentRestClient;
import org.ndexbio.enrichment.rest.model.exceptions.EnrichmentException;
import org.ndexbio.ndexsearch.rest.exceptions.SearchException;
import org.ndexbio.ndexsearch.rest.searchmodel.InternalSourceResults;
import org.ndexbio.ndexsearch.rest.searchmodel.SourceResults;
import org.ndexbio.ndexsearch.rest.searchmodel.Query;
import org.ndexbio.ndexsearch.rest.searchmodel.QueryResults;
import org.ndexbio.ndexsearch.rest.searchmodel.QueryStatus;

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
     * This should be a map of <query UUID> => Query object
     */
    private ConcurrentHashMap<String, Query> _queryTasks;
    
    private ConcurrentLinkedQueue<String> _queryTaskIds;
    
    /**
     * This should be a map of <query UUID> => QueryResults object
     */
    private ConcurrentHashMap<String, QueryResults> _queryResults;
        
    /**
     * This should be a map of <database UUID> => Map<Gene => Set of network UUIDs>
     */
    private ConcurrentHashMap<String, ConcurrentHashMap<String, HashSet<String>>> _databases;
    
    private AtomicReference<InternalSourceResults> _sourceResults;
    private NdexRestClientModelAccessLayer _keywordclient;
    private EnrichmentRestClient _enrichClient;
    
    private long _threadSleep = 10;
    
    public BasicSearchEngineImpl(final String dbDir,
            final String taskDir,
            InternalSourceResults sourceResults,
            NdexRestClientModelAccessLayer keywordclient,
            EnrichmentRestClient enrichClient){
        _shutdown = false;
        _dbDir = dbDir;
        _taskDir = taskDir;
        _keywordclient = keywordclient;
        _queryTasks = new ConcurrentHashMap<>();
        _queryResults = new ConcurrentHashMap<>();
        _sourceResults = new AtomicReference<>();
        _queryTaskIds = new ConcurrentLinkedQueue<>();
        _sourceResults.set(sourceResults);
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
            processQuery(id,_queryTasks.remove(id));            
        }
        _logger.debug("Shutdown was invoked");
        if (this._enrichClient != null){
            try {
                _enrichClient.shutdown();
            } catch(EnrichmentException ee){
                _logger.error("Caught exception shutting down enrichment client", ee);
            }
        }
    }

    @Override
    public void shutdown() {
        _shutdown = true;
    }
    
    
    public void setDatabaseResults(InternalSourceResults dr){
        _sourceResults.set(dr);
    }

    protected void processQuery(final String id, Query query){
        
        
    }
    @Override
    public String query(Query thequery) throws SearchException {
        return null;
        //if (thequery.getDatabaseList() == null || thequery.getDatabaseList().isEmpty()){
        //    throw new SearchException("No databases selected");
        //}
        // @TODO get Jing's uuid generator code that can be a poormans cache
        //String id = UUID.randomUUID().toString();
        //_queryTasks.put(id, thequery);
        //_queryTaskIds.add(id);
        //EnrichmentQueryResults eqr = new QueryResults(System.currentTimeMillis());
        //eqr.setStatus(QueryResults.SUBMITTED_STATUS);
        //_queryResults.merge(id, eqr, (oldval, newval) -> newval.updateStartTime(oldval));        
        //return id;
    }

    @Override
    public SourceResults getSourceResults() throws SearchException {
        SourceResults sr = new SourceResults(_sourceResults.get());
        return sr;
    }
    
    /**
     * Returns
     * @param id Id of the query. 
     * @param start starting index to return from. Starting index is 0.
     * @param size Number of results to return. If 0 means all from starting index so
     *             to get all set both {@code start} and {@code size} to 0.
     * @return {@link org.ndexbio.ndexsearch.rest.searchmodel.QueryResults} object
     *         or null if no result could be found. 
     * @throws SearchException If there was an error getting the results
     */
    @Override
    public QueryResults getQueryResults(String id, int start, int size) throws SearchException {
        return null;
    }

    @Override
    public QueryStatus getQueryStatus(String id) throws SearchException {
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
