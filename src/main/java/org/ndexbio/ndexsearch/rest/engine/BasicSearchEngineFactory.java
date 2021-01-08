package org.ndexbio.ndexsearch.rest.engine;

import java.util.HashMap;
import java.util.Map;
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
	private String _unsetImageURL;
    private SourceConfigurations _sourceConfigurations;
    private long _sourcePollingInterval;
    
    /**
     * Temp directory where query results will temporarily be stored.
     * @param config Contains needed configuration 
     */
    public BasicSearchEngineFactory(Configuration config){
        _keywordclient = config.getNDExClient();
		_unsetImageURL = config.getUnsetImageURL();
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
        Map<String,SourceEngine> sources = new HashMap<>();
        Map<String, Integer> rankMap = new HashMap<>();
		rankMap.put(SourceResult.INTERACTOME_PPI_SERVICE, 2);
		rankMap.put(SourceResult.INTERACTOME_GENEASSOCIATION_SERVICE, 3);
		rankMap.put(SourceResult.KEYWORD_SERVICE, 1);
		
		for (SourceConfiguration sc : _sourceConfigurations.getSources()) {
			_logger.info("Found {} service with endpoint {}. Attempting to add",
					sc.getName(), sc.getEndPoint());
			if (sc.getName().equals(SourceResult.ENRICHMENT_SERVICE)){
				sources.put(SourceResult.ENRICHMENT_SERVICE,
						new EnrichmentSourceEngine(new EnrichmentRestClientImpl(sc.getEndPoint(),
								"")));
             } else if (sc.getName().equals(SourceResult.INTERACTOME_PPI_SERVICE) || 
					 sc.getName().equals(SourceResult.INTERACTOME_GENEASSOCIATION_SERVICE)){
				sources.put(sc.getName(), new InteractomeSourceEngine(sc.getName(),
						new InteractomeRestClient(sc.getEndPoint(), ""),
						rankMap.get(sc.getName())));
					
             } else if (sc.getName().equals(SourceResult.KEYWORD_SERVICE)) {
				 sources.put(sc.getName(),
						 new KeywordSourceEngine(_keywordclient,
								 sc.getUuid().toString(), _unsetImageURL,
						 rankMap.get(sc.getName())));
			}
			else {
				 _logger.error("Unknown source {} skipping", sc.getName());
             } 
        }
        BasicSearchEngineImpl searcher = new BasicSearchEngineImpl(_dbDir,
                _taskDir, _sourceConfigurations,
				_sourcePollingInterval, sources);
        return searcher;
    }
       
}
