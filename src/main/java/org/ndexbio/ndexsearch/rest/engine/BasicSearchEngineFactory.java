package org.ndexbio.ndexsearch.rest.engine;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import org.ndexbio.enrichment.rest.client.EnrichmentRestClientImpl;
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
    private String _geneSymbolFile;
    
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
        _geneSymbolFile = config.getGeneSymbolFile();
    }
    
    
    /**
     * Creates SearchEngine
     * @return 
     */
    public SearchEngine getSearchEngine() throws Exception {
        Map<String,SourceEngine> sources = new HashMap<>();
        Map<String, Integer> rankMap = new HashMap<>();
		int rank = 1;
		rankMap.put(SourceResult.KEYWORD_SERVICE, rank++);
		rankMap.put(SourceResult.INTERACTOME_PPI_SERVICE, rank++);
		rankMap.put(SourceResult.PATHWAYFIGURES_SERVICE, rank++);
		rankMap.put(SourceResult.INDRA_SERVICE, rank++);
		
		for (SourceConfiguration sc : _sourceConfigurations.getSources()) {
			_logger.info("Found {} service with endpoint {}. Attempting to add",
					sc.getName(), sc.getEndPoint());
			if (sc.getName().equals(SourceResult.ENRICHMENT_SERVICE)){
				sources.put(SourceResult.ENRICHMENT_SERVICE,
						new EnrichmentSourceEngine(new EnrichmentRestClientImpl(sc.getEndPoint(),
								"")));
             } else if (sc.getName().equals(SourceResult.KEYWORD_SERVICE)) {
				 sources.put(sc.getName(),
						 new KeywordSourceEngine(_keywordclient,
								 sc.getUuid().toString(), _unsetImageURL,
						 rankMap.get(sc.getName())));
			} else if (sc.getName().equals(SourceResult.PATHWAYFIGURES_SERVICE)){
				sources.put(sc.getName(),
						new EnrichmentSourceEngine(new EnrichmentRestClientImpl(sc.getEndPoint(),
								""), sc.getName()));
			}
			else if (sc.getName().equals(SourceResult.INDRA_SERVICE)){
				sources.put(sc.getName(),
						new EnrichmentSourceEngine(new EnrichmentRestClientImpl(sc.getEndPoint(),
								""), sc.getName()));
			}
			else {
				 _logger.warn("Unknown source {} assuming it is an enrichment", sc.getName());
				 if (sc.getEndPoint() != null){
					 sources.put(sc.getName(),
							new EnrichmentSourceEngine(new EnrichmentRestClientImpl(sc.getEndPoint(),
									""), sc.getName()));
				 } else {
					 _logger.error("Unknown source {} has null for endpoint. Skipping", sc.getEndPoint());
				 }
             } 
        }
        BasicSearchEngineImpl searcher = new BasicSearchEngineImpl(_dbDir,
                _taskDir, _sourceConfigurations,
				_sourcePollingInterval, sources, new File ( _dbDir + File.separator + _geneSymbolFile ));
        return searcher;
    }
       
}
