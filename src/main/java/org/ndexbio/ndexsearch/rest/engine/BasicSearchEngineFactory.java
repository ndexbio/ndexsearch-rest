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
import org.ndexbio.ndexsearch.rest.model.SourceConfiguration;
import org.ndexbio.ndexsearch.rest.model.SourceConfigurations;
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
    private SourceConfigurations _sourceConfigurations;
    private long _sourcePollingInterval;
    
    /**
     * Temp directory where query results will temporarily be stored.
     * @param tmpDir 
     */
    public BasicSearchEngineFactory(Configuration config){
        _keywordclient = config.getNDExClient();
        _dbDir = config.getSearchDatabaseDirectory();
        _taskDir = config.getSearchTaskDirectory();
        _sourceConfigurations = config.getSourceConfigurations();
        _sourcePollingInterval = config.getSourcePollingInterval();
    }
    
    
    /**
     * Creates SearchEngine
     * @return 
     */
    public SearchEngine getSearchEngine() throws Exception {
        EnrichmentRestClient enrichClient = null;
        InteractomeRestClient interactomeClient_i = null;
        InteractomeRestClient interactomeClient_a = null;
        
        for (SourceConfiguration sc : _sourceConfigurations.getSources()) {
        	
        	if (sc.getName().equals(SourceResult.ENRICHMENT_SERVICE)){
                enrichClient = new EnrichmentRestClientImpl(sc.getEndPoint(), "");
            } else if (sc.getName().equals(SourceResult.INTERACTOME_PPI_SERVICE)){
            	interactomeClient_i = new InteractomeRestClient(sc.getEndPoint(), "");
            } else if (sc.getName().equals(SourceResult.INTERACTOME_GENEASSOCIATION_SERVICE)){
            	interactomeClient_a = new InteractomeRestClient(sc.getEndPoint(), "");
            } 
            
        }
        BasicSearchEngineImpl searcher = new BasicSearchEngineImpl(_dbDir,
                _taskDir, _sourceConfigurations, _sourcePollingInterval, _keywordclient,
                enrichClient, interactomeClient_i, interactomeClient_a);
        return searcher;
    }
       
}
