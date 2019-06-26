/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ndexbio.ndexsearch.rest.engine;

import java.util.stream.Collectors;

import org.ndexbio.enrichment.rest.client.EnrichmentRestClient;
import org.ndexbio.enrichment.rest.client.EnrichmentRestClientImpl;
import org.ndexbio.interactomesearch.client.InteractomeRestClient;
import org.ndexbio.ndexsearch.rest.model.SourceResult;
import org.ndexbio.ndexsearch.rest.model.InternalSourceResults;
import org.ndexbio.ndexsearch.rest.model.SourceConfiguration;
import org.ndexbio.ndexsearch.rest.model.SourceConfigurations;
import org.ndexbio.ndexsearch.rest.services.Configuration;
import org.ndexbio.rest.client.NdexRestClientModelAccessLayer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

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
    
    /**
     * Temp directory where query results will temporarily be stored.
     * @param tmpDir 
     */
    public BasicSearchEngineFactory(Configuration config){
        _keywordclient = config.getNDExClient();
        _dbDir = config.getSearchDatabaseDirectory();
        _taskDir = config.getSearchTaskDirectory();
        _sourceConfigurations = config.getSourceConfigurations();
    }
    
    
    /**
     * Creates SearchEngine
     * @return 
     */
    public SearchEngine getSearchEngine() throws Exception {
        EnrichmentRestClient enrichClient = null;
        InteractomeRestClient interactomeClient = null;
        
        for (SourceConfiguration sc : _sourceConfigurations.getSources()) {
        	
        	if (sc.getName().equals(SourceResult.ENRICHMENT_SERVICE)){
                enrichClient = new EnrichmentRestClientImpl(sc.getEndPoint(), "");
            }
            if (sc.getName().equals(SourceResult.INTERACTOME_SERVICE)){
            	interactomeClient = new InteractomeRestClient(sc.getEndPoint(), "");
            }
            
        }
        BasicSearchEngineImpl searcher = new BasicSearchEngineImpl(_dbDir,
                _taskDir, _sourceConfigurations, _keywordclient, enrichClient, interactomeClient);
        return searcher;
    }
       
}
