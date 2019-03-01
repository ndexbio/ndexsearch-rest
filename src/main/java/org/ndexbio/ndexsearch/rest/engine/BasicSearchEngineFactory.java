/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ndexbio.ndexsearch.rest.engine;

import java.util.HashMap;
import java.util.HashSet;
import org.ndexbio.ndexsearch.rest.searchmodel.SourceResult;
import org.ndexbio.ndexsearch.rest.searchmodel.InternalSourceResults;
import org.ndexbio.ndexsearch.rest.searchmodel.InternalGeneMap;
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
    private NdexRestClientModelAccessLayer _client;
    private InternalSourceResults _databaseResults;
    
    /**
     * Temp directory where query results will temporarily be stored.
     * @param tmpDir 
     */
    public BasicSearchEngineFactory(Configuration config){
        
    }
    
    
    /**
     * Creates SearchEngine
     * @return 
     */
    public SearchEngine getEnrichmentEngine() throws Exception {
        BasicSearchEngineImpl searcher = new BasicSearchEngineImpl(_dbDir,
                _taskDir,_client);
        searcher.setDatabaseResults(_databaseResults);
        for (SourceResult dr : _databaseResults.getResults()){
            _logger.debug("Loading: " + dr.getName());
            _logger.debug("Done with loading");
        }
        return searcher;
    }
       
}
