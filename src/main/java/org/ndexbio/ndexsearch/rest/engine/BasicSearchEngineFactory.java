/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ndexbio.ndexsearch.rest.engine;

import org.ndexbio.enrichment.rest.client.EnrichmentRestClient;
import org.ndexbio.enrichment.rest.client.EnrichmentRestClientImpl;
import org.ndexbio.interactomesearch.client.InteractomeRestClient;
import org.ndexbio.ndexsearch.rest.model.SourceResult;
import org.ndexbio.ndexsearch.rest.model.InternalSourceResults;
import org.ndexbio.ndexsearch.rest.services.Configuration;
import org.ndexbio.rest.client.NdexRestClientModelAccessLayer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author churas
 */
public class BasicSearchEngineFactory {
    
    static Logger _logger = LoggerFactory.getLogger(BasicSearchEngineFactory.class);

    private String _dbDir;
    private String _taskDir;
    private NdexRestClientModelAccessLayer _keywordclient;
    private InternalSourceResults _sourceResults;
    
    /**
     * Temp directory where query results will temporarily be stored.
     * @param tmpDir 
     */
    public BasicSearchEngineFactory(Configuration config){
        _keywordclient = config.getNDExClient();
        _dbDir = config.getSearchDatabaseDirectory();
        _taskDir = config.getSearchTaskDirectory();
        _sourceResults = config.getSourceResults();
    }
    
    
    /**
     * Creates SearchEngine
     * @return 
     */
    public SearchEngine getSearchEngine() throws Exception {
        EnrichmentRestClient enrichClient = null;
        InteractomeRestClient interactomeClient = null;
        for (SourceResult sr : _sourceResults.getResults()){
            if (sr.getName().equals(SourceResult.ENRICHMENT_SERVICE)){
                enrichClient = new EnrichmentRestClientImpl(sr.getEndPoint(), "");
            }
            if (sr.getName().equals(SourceResult.INTERACTOME_SERVER)){
            	interactomeClient = new InteractomeRestClient(sr.getEndPoint(), "");
            }
            
        }
        BasicSearchEngineImpl searcher = new BasicSearchEngineImpl(_dbDir,
                _taskDir, _sourceResults,_keywordclient, enrichClient, interactomeClient);
        return searcher;
    }
       
}
