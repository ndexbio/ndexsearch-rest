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
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.commons.io.FileUtils;
import org.ndexbio.enrichment.rest.client.EnrichmentRestClient;
import org.ndexbio.enrichment.rest.model.DatabaseResult;
import org.ndexbio.enrichment.rest.model.EnrichmentQuery;
import org.ndexbio.enrichment.rest.model.EnrichmentQueryResult;
import org.ndexbio.enrichment.rest.model.EnrichmentQueryResults;
import org.ndexbio.enrichment.rest.model.exceptions.EnrichmentException;
import org.ndexbio.model.exceptions.NdexException;
import org.ndexbio.model.object.NetworkSearchResult;
import org.ndexbio.model.object.network.NetworkSummary;
import org.ndexbio.ndexsearch.rest.exceptions.SearchException;
import org.ndexbio.ndexsearch.rest.model.InternalSourceResults;
import org.ndexbio.ndexsearch.rest.model.SourceResults;
import org.ndexbio.ndexsearch.rest.model.Query;
import org.ndexbio.ndexsearch.rest.model.QueryResults;
import org.ndexbio.ndexsearch.rest.model.QueryStatus;
import org.ndexbio.ndexsearch.rest.model.SourceQueryResult;
import org.ndexbio.ndexsearch.rest.model.SourceQueryResults;
import org.ndexbio.ndexsearch.rest.model.SourceResult;
import org.ndexbio.ndexsearch.rest.model.comparators.SourceQueryResultByRank;
import org.ndexbio.ndexsearch.rest.model.comparators.SourceQueryResultsBySourceRank;

import org.ndexbio.rest.client.NdexRestClientModelAccessLayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ndexbio.interactomesearch.client.InteractomeRestClient;
import org.ndexbio.interactomesearch.object.InteractomeSearchResult;
import org.ndexbio.interactomesearch.object.SearchStatus;
import org.ndexbio.ndexsearch.rest.services.Configuration;
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
    private InteractomeRestClient _interactomeClient;
    
    private long _threadSleep = 10;
    private SourceQueryResultsBySourceRank _sourceRankSorter;
    private SourceQueryResultByRank _rankSorter;
    
    public BasicSearchEngineImpl(final String dbDir,
            final String taskDir,
            InternalSourceResults sourceResults,
            NdexRestClientModelAccessLayer keywordclient,
            EnrichmentRestClient enrichClient, InteractomeRestClient interactomeClient){
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
        _interactomeClient = interactomeClient;
        _sourceRankSorter = new SourceQueryResultsBySourceRank();
        _rankSorter = new SourceQueryResultByRank();
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
    
    protected QueryResults getQueryResultsFromDbOrFilesystem(final String id){
        QueryResults qr = _queryResults.get(id);
        if (qr != null){
            return qr;
        }
        ObjectMapper mappy = new ObjectMapper();
        File qrFile = new File(getQueryResultsFilePath(id));
        if (qrFile.isFile() == false){
            _logger.error(qrFile.getAbsolutePath() + " is not a file");
            return null;
        }
        try {
            return mappy.readValue(qrFile, QueryResults.class);
        }catch(IOException io){
            _logger.error("Caught exception trying to load " +qrFile.getAbsolutePath(), io);
        }
        return null;
    }

    protected void updateQueryResultsInDb(final String id,
            QueryResults updatedQueryResults){
        _queryResults.merge(id, updatedQueryResults, (oldval, newval) -> newval.updateStartTime(oldval));        
    }
    
    protected SourceQueryResults processEnrichment(final String sourceName, Query query) {
        EnrichmentQuery equery = new EnrichmentQuery();
        equery.setDatabaseList(getEnrichmentDatabaseList(sourceName));
        equery.setGeneList(query.getGeneList());
        try {
            SourceQueryResults sqr = new SourceQueryResults();
            sqr.setSourceName(sourceName);
            String enrichTaskId = _enrichClient.query(equery);
            if (enrichTaskId == null){
                _logger.error("Query failed");
                sqr.setMessage("Enrichment failed for unknown reason");
                sqr.setStatus(QueryResults.FAILED_STATUS);
                sqr.setProgress(100);
                return sqr;
            }
            sqr.setStatus(QueryResults.SUBMITTED_STATUS);
            sqr.setSourceUUID(enrichTaskId);
            return sqr;
        } catch(EnrichmentException ee){
            _logger.error("Caught exception running enrichment", ee);
        }
        return null;
    }

    protected SourceQueryResults processInteractome(final String sourceName, Query query)  {
       
        SourceQueryResults sqr = new SourceQueryResults();
        sqr.setSourceName(sourceName);
        try {
            String interactomeTaskId = this._interactomeClient.search(query.getGeneList()).toString();
            if (interactomeTaskId == null){
                _logger.error("Query failed");
                sqr.setMessage("Interactome failed for unknown reason");
                sqr.setStatus(QueryResults.FAILED_STATUS);
                sqr.setProgress(100);
                return sqr;
            }
            sqr.setStatus(QueryResults.SUBMITTED_STATUS);
            sqr.setSourceUUID(interactomeTaskId);
            return sqr;
        } catch(NdexException ee){
            _logger.error("Caught ndexexception running interactome", ee);
            sqr.setMessage("Interactome failed: " + ee.getMessage());
        } catch(Exception ex){
            _logger.error("Caught exception running interactome", ex);
            sqr.setMessage("Interactome failed: " + ex.getMessage());
        }
        sqr.setStatus(QueryResults.FAILED_STATUS);
        sqr.setProgress(100);
        return sqr;
    }
    
    protected String getUUIDOfSourceByName(final String sourceName){
        if (sourceName == null){
            return null;
        }
        InternalSourceResults isr = _sourceResults.get();
        for (SourceResult sr : isr.getResults()){
            if (sr.getName() == null){
                continue;
            }
            if (sr.getName().equals(sourceName)){
                return sr.getUuid();
            }
        }
        return null;
    }
    
    protected List<String> getEnrichmentDatabaseList(final String sourceName){
        if (sourceName == null){
            return null;
        }
        List<String> dbList = new LinkedList<>();
        InternalSourceResults isr = _sourceResults.get();
        for (SourceResult sr : isr.getResults()){
            if (sr.getName()== null){
                continue;
            }
            if (sr.getName().equals(sourceName)){
                for(DatabaseResult dr : sr.getDatabases()){
                    dbList.add(dr.getName());
                }
            }
        }
        return dbList;
    }
    
    protected SourceQueryResults processKeyword(final String sourceName, Query query) {
        
        try {
            SourceQueryResults sqr = new SourceQueryResults();
            sqr.setSourceName(sourceName);
            StringBuilder sb = new StringBuilder();
            for (String gene : query.getGeneList()){
                if (gene.isEmpty() == false){
                    sb.append(" ");
                }
                sb.append(gene);
            }
            _logger.info("Query being sent: " + sb.toString());
            NetworkSearchResult nrs = _keywordclient.findNetworks(sb.toString(), null, 0, 100);
            if (nrs == null){
                _logger.error("Query failed");
                sqr.setMessage(sourceName + " failed for unknown reason");
                sqr.setStatus(QueryResults.FAILED_STATUS);
                sqr.setProgress(100);
                return sqr;
            }
            int rankCounter = 0;
            List<SourceQueryResult> sqrList = new LinkedList<>();
            for(NetworkSummary ns : nrs.getNetworks()){
                SourceQueryResult sr = new SourceQueryResult();
                sr.setDescription(ns.getName());
                sr.setEdges(ns.getEdgeCount());
                sr.setNodes(ns.getNodeCount());
                sr.setNetworkUUID(ns.getExternalId().toString());
                sr.setPercentOverlap(0);
                sr.setRank(rankCounter++);
                sr.setImageURL(Configuration.getInstance().getUnsetImageURL());
                sqrList.add(sr);
            }
            sqr.setNumberOfHits(sqrList.size());
            sqr.setProgress(100);
            sqr.setStatus(QueryResults.COMPLETE_STATUS);
            sqr.setSourceUUID(getUUIDOfSourceByName(sourceName));
            sqr.setResults(sqrList);
            _logger.info("Returning sqr");
            return sqr;
        } catch(IOException io){
            _logger.error("caught ioexceptin ", io);
        } catch(NdexException ne){
            _logger.error("caught", ne);
        }
        return null;
    }
    
    protected void processQuery(final String id, Query query){
        
        QueryResults qr = getQueryResultsFromDb(id);
        qr.setQuery(query.getGeneList());
        qr.setInputSourceList(query.getSourceList());
        qr.setStatus(QueryResults.PROCESSING_STATUS);
        File taskDir = new File(this._taskDir + File.separator + id);
        _logger.info("Creating new task directory:" + taskDir.getAbsolutePath());
        
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
            _logger.info("Querying service: " + source);
            
            if (source.equals(SourceResult.ENRICHMENT_SERVICE)){
                sqr = processEnrichment(source, query);
                sqr.setSourceRank(0);
            } else if (source.equals(SourceResult.KEYWORD_SERVICE)){
                sqr = processKeyword(source, query);
                sqr.setSourceRank(1);
            } else if ( source.equals(SourceResult.INTERACTOME_SERVER)) {
            	sqr = processInteractome(source, query);
            	sqr.setSourceRank(2);
            }
            
            if (sqr != null){
                _logger.info("Adding SourceQueryResult for " + source);
                sqrList.add(sqr);
                qr.setSources(sqrList);
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
     * 
     * @param sqRes
     * @return number of hits for this source
     */
    protected int updateEnrichmentSourceQueryResults(SourceQueryResults sqRes){
        try {
            EnrichmentQueryResults qr = this._enrichClient.getQueryResults(sqRes.getSourceUUID(), 0, 0);
            sqRes.setMessage(qr.getMessage());
            sqRes.setProgress(qr.getProgress());
            sqRes.setStatus(qr.getStatus());
            
            sqRes.setWallTime(qr.getWallTime());
            List<SourceQueryResult> sqResults = new LinkedList<>();
            if (qr.getResults() != null){
                for (EnrichmentQueryResult qRes : qr.getResults()){
                    SourceQueryResult sqr = new SourceQueryResult();
                    sqr.setDescription(qRes.getDatabaseName() + ": " + qRes.getDescription());
                    sqr.setEdges(qRes.getEdges());
                    sqr.setHitGenes(qRes.getHitGenes());
                    sqr.setNetworkUUID(qRes.getNetworkUUID());
                    sqr.setNodes(qRes.getNodes());
                    sqr.setPercentOverlap(qRes.getPercentOverlap());
                    sqr.setRank(qRes.getRank());
                    sqr.setImageURL(qRes.getImageURL());
                    sqResults.add(sqr);
                }
            }
            sqRes.setResults(sqResults);
            sqRes.setNumberOfHits(sqResults.size());
            return sqRes.getNumberOfHits();
        }catch(EnrichmentException ee){
            _logger.error("caught exception", ee);
        }
        return 0;
    }
    
    protected int updateInteractomeSourceQueryResults(SourceQueryResults sqRes){
        try {
            List<InteractomeSearchResult> qr = this._interactomeClient.getSearchResult(UUID.fromString(sqRes.getSourceUUID()));
            SearchStatus status = this._interactomeClient.getSearchStatus(UUID.fromString(sqRes.getSourceUUID())); 
            sqRes.setMessage(status.getMessage());
            sqRes.setProgress(status.getProgress());
            sqRes.setStatus(status.getStatus());
            
            sqRes.setWallTime(status.getWallTime());
            List<SourceQueryResult> sqResults = new LinkedList<>();
            if (qr != null){
                for (InteractomeSearchResult qRes : qr){
                    SourceQueryResult sqr = new SourceQueryResult();
                    sqr.setDescription(qRes.getDescription());
                    sqr.setEdges(qRes.getEdgeCount());
                    sqr.setHitGenes(qRes.getHitGenes());
                    sqr.setNetworkUUID(qRes.getNetworkUUID());
                    sqr.setNodes(qRes.getNodeCount());
                    sqr.setPercentOverlap(qRes.getPercentOverlap());
                    sqr.setImageURL(qRes.getImageURL());
                    sqr.setRank(qRes.getRank());
                    sqResults.add(sqr);
                }
            }
            sqRes.setResults(sqResults);
            sqRes.setNumberOfHits(sqResults.size());
            return sqRes.getNumberOfHits();
        }catch(NdexException ee){
            _logger.error("caught exception", ee);
        }
        return 0;
    }
        
    protected void checkAndUpdateQueryResults(QueryResults qr){
        // if its complete just return
        if (qr.getStatus().equals(QueryResults.COMPLETE_STATUS)){
            return;
        }
        if (qr.getStatus().equals(QueryResults.FAILED_STATUS)){
            return;
        }
        int hitCount = 0;
        int numComplete = 0;
        if (qr.getSources() != null){
            for (SourceQueryResults sqRes : qr.getSources()){
                _logger.info("Examining status of " + sqRes.getSourceName());
                if (sqRes.getProgress() == 100){
                    hitCount += sqRes.getNumberOfHits();
                    numComplete++;
                    continue;
                }
                if (sqRes.getSourceName().equals(SourceResult.ENRICHMENT_SERVICE)){
                    _logger.info("adding hits to hit count");
                    hitCount += updateEnrichmentSourceQueryResults(sqRes);
                    if (sqRes.getProgress() == 100){
                        numComplete++;
                    }
                } else if (sqRes.getSourceName().equals(SourceResult.INTERACTOME_SERVER)){
                    _logger.info("adding hits to hit count");
                    hitCount += updateInteractomeSourceQueryResults(sqRes);
                    if (sqRes.getProgress() == 100){
                        numComplete++;
                    }
                }
            }
            if (numComplete >= qr.getSources().size()){
                qr.setProgress(100);
                qr.setStatus(QueryStatus.COMPLETE_STATUS);
            } else {
                qr.setProgress(Math.round(((float)numComplete/(float)qr.getSources().size())*100));
            }
            qr.setNumberOfHits(hitCount);
        }
    }
    
    protected void filterQueryResultsBySourceList(QueryResults qr, final String source){
        if (source == null || source.isEmpty() || source.trim().isEmpty()){
            _logger.debug("Source not defined leave all results in");
            return;
        }
        
        String[] splitStr = source.split("\\s+,\\s+");
        HashSet<String> sourceSet = new HashSet<>();
        for (String entry : splitStr){
            sourceSet.add(entry);
        }
        List<SourceQueryResults> newSqrList = new LinkedList<>();
        for (SourceQueryResults sqr : qr.getSources()){
            if (!sourceSet.contains(sqr.getSourceName())){
                _logger.debug(sqr.getSourceName() + " not in source list. Skipping.");
                continue;
            }
            newSqrList.add(sqr);
        }
        qr.setSources(newSqrList);
        return;
    }
    
    protected void filterQueryResultsByStartAndSize(QueryResults qr, int start, int size){
        if (start == 0 && size == 0){
            return;
        }
        if (qr.getSources() == null || qr.getSources().isEmpty()){
            return;
        }
        int counter = 0;
        List<SourceQueryResults> newSQRS = null;
        List<SourceQueryResult> srcQueryRes = null;
        Collections.sort(qr.getSources(), _sourceRankSorter);
        newSQRS = new LinkedList<>();
        for (SourceQueryResults sqr : qr.getSources()){
            Collections.sort(sqr.getResults(), _rankSorter);
            
            srcQueryRes = null;
            for(SourceQueryResult sq : sqr.getResults()){
                
                if (counter < start){
                    counter++;
                    continue;
                }
                if (counter >= size){
                    break;
                }
                if (srcQueryRes == null){
                    srcQueryRes = new LinkedList<>();
                }
                srcQueryRes.add(sq);
                counter++;
            }
            if (srcQueryRes != null){
                sqr.setResults(srcQueryRes);
                newSQRS.add(sqr);
            }
        }
        qr.setSources(newSQRS);
    }
    
    /**
     * Returns
     * @param id Id of the query. 
     * @param start starting index to return from. Starting index is 0.
     * @param size Number of results to return. If 0 means all from starting index so
     *             to get all set both {@code start} and {@code size} to 0.
     * @param source comma delimited list of sources to return. null or empty string means 
     *               all.
     * @return {@link org.ndexbio.ndexsearch.rest.model.QueryResults} object
     *         or null if no result could be found. 
     * @throws SearchException If there was an error getting the results
     */
    @Override
    public QueryResults getQueryResults(final String id, final String source, int start, int size) throws SearchException {
        _logger.info("Got queryresults request: " + id);
        QueryResults qr = this.getQueryResultsFromDbOrFilesystem(id);
        if (qr == null){
            return null;
        }
        if (start < 0){
            throw new SearchException("start parameter must be value of 0 or greater");
        }
        if (size < 0){
            throw new SearchException("size parameter must be value of 0 or greater");
        }
        checkAndUpdateQueryResults(qr);
        filterQueryResultsBySourceList(qr, source);
        filterQueryResultsByStartAndSize(qr, start, size);
        return qr;
    }

    @Override
    public QueryStatus getQueryStatus(String id) throws SearchException {
        QueryResults qr = this.getQueryResultsFromDbOrFilesystem(id);
        if (qr == null){
            return null;
        }
        checkAndUpdateQueryResults(qr);
        if (qr.getSources() != null){
            for (SourceQueryResults sqr : qr.getSources()){
                sqr.setResults(null);
            }
        }
        return qr;
    }

    @Override
    public void delete(String id) throws SearchException {
        _logger.debug("Deleting task " + id);
        QueryResults qr = this.getQueryResultsFromDbOrFilesystem(id);
        if (qr == null){
            _logger.error("cant find " + id + " to delete");
            return;
        }
        if (qr.getSources() == null){
            return;
        }
        for (SourceQueryResults sqr: qr.getSources()){
            if (sqr.getSourceName().equals(SourceResult.ENRICHMENT_SERVICE)){
                try {
                    _logger.debug("Calling enrichment DELETE on id: " + sqr.getSourceUUID());
                    _enrichClient.delete(sqr.getSourceUUID());
                } catch(EnrichmentException ee){
                    throw new SearchException("caught error trying to delete enrichment: " + ee.getMessage());
                }
            } else if (sqr.getSourceName().equals(SourceResult.INTERACTOME_SERVER)) {
            	//TODO handle this when a delete function is added to the interactome search.
            }
        }
        // TODO delete the local file system copy
        File thisTaskDir = new File(this._taskDir + File.separator + id);
        if (thisTaskDir.exists() == false){
            return;
        }
        _logger.debug("Attempting to delete task from filesystem: " + thisTaskDir.getAbsolutePath());
        if (FileUtils.deleteQuietly(thisTaskDir) == false){
            _logger.error("There was a problem deleting the directory: " + thisTaskDir.getAbsolutePath());
        }
    }

    @Override
    public InputStream getNetworkOverlayAsCX(final String id, final String sourceUUID, String networkUUID) throws SearchException, NdexException {
        QueryResults qr = this.getQueryResultsFromDbOrFilesystem(id);
        checkAndUpdateQueryResults(qr);
        for (SourceQueryResults sqRes : qr.getSources()){
            if (sqRes.getSourceUUID().equals(sourceUUID) == false){
                continue;
            }
            for(SourceQueryResult sqr: sqRes.getResults()){
                if (sqr.getNetworkUUID() == null || !sqr.getNetworkUUID().equals(networkUUID)){
                    continue;
                }
                if (sqRes.getSourceName().equals(SourceResult.ENRICHMENT_SERVICE)){
                    try {
                        return _enrichClient.getNetworkOverlayAsCX(sqRes.getSourceUUID(),"", sqr.getNetworkUUID());
                    } catch(EnrichmentException ee){
                        throw new SearchException("unable to get network: " + ee.getMessage());
                    }
                } else if (sqRes.getSourceName().equals(SourceResult.KEYWORD_SERVICE)){
                    try {
                        _logger.info("Returning network as stream: " + networkUUID);
                        return this._keywordclient.getNetworkAsCXStream(UUID.fromString(networkUUID));
                    } catch(IOException ee){
                        throw new SearchException("unable to get network: " + ee.getMessage());
                    } catch(NdexException ne){
                        throw new SearchException("unable to get network " + ne.getMessage());
                    }
                } else if ( sqRes.getSourceName().equals(SourceResult.INTERACTOME_SERVER)) {
                	return this._interactomeClient.getOverlayedNetworkStream(UUID.fromString(sqRes.getSourceUUID()),
                			UUID.fromString(sqr.getNetworkUUID()));
                }
            }
        }
        return null;
    }
    
}
