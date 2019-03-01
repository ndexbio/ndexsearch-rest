/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ndexbio.ndexsearch.rest.engine;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicReference;
import org.ndexbio.enrichment.rest.client.EnrichmentRestClient;
import org.ndexbio.enrichment.rest.model.EnrichmentQuery;
import org.ndexbio.enrichment.rest.model.exceptions.EnrichmentException;
import org.ndexbio.ndexsearch.rest.exceptions.SearchException;
import org.ndexbio.ndexsearch.rest.model.InternalSourceResults;
import org.ndexbio.ndexsearch.rest.model.SourceResults;
import org.ndexbio.ndexsearch.rest.model.Query;
import org.ndexbio.ndexsearch.rest.model.QueryResults;
import org.ndexbio.ndexsearch.rest.model.QueryStatus;
import org.ndexbio.ndexsearch.rest.model.SourceQueryResults;
import org.ndexbio.ndexsearch.rest.model.SourceResult;

import org.ndexbio.rest.client.NdexRestClientModelAccessLayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Runs enrichment 
 * @author churas
 */
public class BasicSearchEngineImpl implements SearchEngine {

    public static final String QR_JSON_FILE = "queryresults.json";
    
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
        _enrichClient = enrichClient;
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
           // try {
               // _enrichClient.shutdown();
          //  } catch(EnrichmentException ee){
              //  _logger.error("Caught exception shutting down enrichment client", ee);
          //  }
        }
    }

    @Override
    public void shutdown() {
        _shutdown = true;
    }
    
    
    public void setDatabaseResults(InternalSourceResults dr){
        _sourceResults.set(dr);
    }
    
    protected String getQueryResultsFilePath(final String id){
        return this._taskDir + File.separator + id + File.separator + BasicSearchEngineImpl.QR_JSON_FILE;
    }

    protected void saveQueryResultsToFilesystem(final String id){
        QueryResults eqr = getQueryResultsFromDb(id);
        if (eqr == null){
            return;
        }
        File destFile = new File(getQueryResultsFilePath(id));
        ObjectMapper mappy = new ObjectMapper();
        try (FileOutputStream out = new FileOutputStream(destFile)){
            mappy.writeValue(out, eqr);
        } catch(IOException io){
            _logger.error("Caught exception writing " + destFile.getAbsolutePath(), io);
        }
        _queryResults.remove(id);
    }
    
    /**
     * First tries to get EnrichmentQueryResults from _queryResults list
     * and if that fails method creates a new EnrichmentQueryResults setting
     * current time in constructor.
     * @param id
     * @return 
     */
    protected QueryResults getQueryResultsFromDb(final String id){
        QueryResults qr = _queryResults.get(id);
        if (qr == null){
            qr = new QueryResults(System.currentTimeMillis());
        }
        return qr;
    }

    protected void updateQueryResultsInDb(final String id,
            QueryResults updatedQueryResults){
        _queryResults.merge(id, updatedQueryResults, (oldval, newval) -> newval.updateStartTime(oldval));        
    }
    
    protected SourceQueryResults processEnrichment(final String sourceName, Query query){
        EnrichmentQuery equery = new EnrichmentQuery();
        equery.setDatabaseList(Arrays.asList("ncipid"));
        equery.setGeneList(query.getGeneList());
        try {
            SourceQueryResults sqr = new SourceQueryResults();
            sqr.setSourceName(sourceName);
            String enrichTaskId = _enrichClient.query(equery);
            if (enrichTaskId == null){
                _logger.error("Query failed");
                throw new EnrichmentException("query failed");
            }
            sqr.setStatus(QueryResults.SUBMITTED_STATUS);
            sqr.setSourceUUID(enrichTaskId);
            return sqr;
        } catch(EnrichmentException ee){
            _logger.error("Caught exception running enrichment", ee);
        }
        return null;
    }
    
    protected void processQuery(final String id, Query query){
        
        QueryResults qr = getQueryResultsFromDb(id);
        qr.setQuery(query.getGeneList());
        qr.setInputSourceList(query.getSourceList());
            
        File taskDir = new File(this._taskDir + File.separator + id);
        _logger.debug("Creating new task directory:" + taskDir.getAbsolutePath());
        
        if (taskDir.mkdirs() == false){
            _logger.error("Unable to create task directory: " + taskDir.getAbsolutePath());
            qr.setStatus(QueryResults.FAILED_STATUS);
            qr.setMessage("Internal error unable to create directory on filesystem");
            qr.setProgress(100);
            updateQueryResultsInDb(id, qr);
            return;
        }
        
        List<SourceQueryResults> sqrList = new LinkedList<>();
        qr.setSources(sqrList);
        SourceQueryResults sqr = null;
        for (String source : query.getSourceList()){
            _logger.debug("Querying service: " + source);
            
            if (source.equals(SourceResult.ENRICHMENT_SERVICE)){
                sqr = processEnrichment(source, query);
            }
            if (sqr != null){
                sqrList.add(sqr);
                updateQueryResultsInDb(id, qr);
            }
            sqr = null;
        }
        saveQueryResultsToFilesystem(id);
    }
    
    @Override
    public String query(Query thequery) throws SearchException {
        if (thequery.getSourceList() == null || thequery.getSourceList().isEmpty()){
            throw new SearchException("No databases selected");
        }
        _logger.debug("Received query request");
        // @TODO get Jing's uuid generator code that can be a poormans cache
        String id = UUID.randomUUID().toString();
        _queryTasks.put(id, thequery);
        _queryTaskIds.add(id);
        QueryResults qr = new QueryResults(System.currentTimeMillis());
        qr.setInputSourceList(thequery.getSourceList());
        qr.setQuery(thequery.getGeneList());
        qr.setStatus(QueryResults.SUBMITTED_STATUS);
        _queryResults.merge(id, qr, (oldval, newval) -> newval.updateStartTime(oldval));        
        return id;
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
     * @return {@link org.ndexbio.ndexsearch.rest.model.QueryResults} object
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
