package org.ndexbio.ndexsearch.rest.services;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Properties;
import javax.naming.InitialContext;
import org.ndexbio.ndexsearch.rest.exceptions.SearchException;
import org.ndexbio.ndexsearch.rest.model.InternalSourceResults;
import org.ndexbio.ndexsearch.rest.model.SourceConfiguration;
import org.ndexbio.ndexsearch.rest.model.SourceConfigurations;
import org.ndexbio.ndexsearch.rest.model.SourceResult;
import org.ndexbio.model.exceptions.NdexException;
import org.ndexbio.rest.client.NdexRestClient;
import org.ndexbio.rest.client.NdexRestClientModelAccessLayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ndexbio.ndexsearch.rest.engine.SearchEngine;

/**
 * Contains configuration for Enrichment. The configuration
 is extracted by looking for a file under the environment
 variable NDEX_SEARCH_CONFIG and if that fails defaults are
 used
 * @author churas
 */
public class Configuration {
    
    /**
     * Sets the REST end point for {@link org.ndexbio.ndexsearch.rest.services.Status}, 
     * {@link org.ndexbio.ndexsearch.rest.services.Search} and 
     * {@link org.ndexbio.ndexsearch.rest.services.SearchSource} classes
     */
    public static final String REST_PATH = "/";
    public static final String V_ONE_PATH = "/v1";
    public static final String NDEX_SEARCH_CONFIG = "NDEX_SEARCH_CONFIG";
    
    public static final String DATABASE_DIR = "search.database.dir";
    public static final String TASK_DIR = "search.task.dir";
    public static final String UNSET_IMAGE_URL = "search.unset.image.url";
    
    public static final String HOST_URL = "search.host.url";
    
    public static final String NDEX_USER = "ndex.user";
    public static final String NDEX_PASS = "ndex.password";
    public static final String NDEX_SERVER = "ndex.server";
    public static final String NDEX_USERAGENT = "ndex.useragent";
    
    
    public static final String SOURCE_CONFIGURATIONS_JSON_FILE = "source.configurations";
    public static final String SOURCE_POLLING_INTERVAL = "source.polling.interval";
    private static final long DEFAULT_SOURCE_POLLING_INTERVAL = 300000;
    
    private static Configuration INSTANCE;
    private static final Logger _logger = LoggerFactory.getLogger(Configuration.class);
    private static String _alternateConfigurationFile;
    private static NdexRestClientModelAccessLayer _client;
    private static SearchEngine _searchEngine;

    private static String _searchDatabaseDir;
    private static String _searchTaskDir;
    private static String _unsetImageURL;
    
    private static String _searchHostURL;
    
    private  String _sourceConfiguration;
    private static String _sourcePollingInterval;
    
    
    /**
     * Constructor that attempts to get configuration from properties file
     * specified via configPath
     */
    private Configuration(final String configPath)
    {
        Properties props = new Properties();
        try (FileInputStream s = new FileInputStream(configPath) ) {
            props.load(s);
        }
        catch(FileNotFoundException fne){
            _logger.error("No configuration found at " + configPath, fne);
        }
        catch(IOException io){
            _logger.error("Unable to read configuration " + configPath, io);
        }
        
        _searchDatabaseDir = props.getProperty(Configuration.DATABASE_DIR, "/tmp");
        _searchTaskDir = props.getProperty(Configuration.TASK_DIR);
        _unsetImageURL = props.getProperty(Configuration.UNSET_IMAGE_URL,
                                           "http://ndexbio.org/images/new_landing_page_logo.06974471.png");
        
        _searchHostURL = props.getProperty(Configuration.HOST_URL, "");
        if (_searchHostURL.trim().isEmpty()){
            _searchHostURL = "";
        } else if (!_searchHostURL.endsWith("/")){
            _searchHostURL =_searchHostURL + "/";
        }
        
        _sourceConfiguration = props.getProperty(SOURCE_CONFIGURATIONS_JSON_FILE, "source.configurations.json");
        _sourcePollingInterval = props.getProperty(Configuration.SOURCE_POLLING_INTERVAL, Long.toString(DEFAULT_SOURCE_POLLING_INTERVAL));
        _client = getNDExClient(props);
        
    }
    
    public NdexRestClientModelAccessLayer getNDExClient(){
        return _client;
    }
    
    protected void setSearchEngine(SearchEngine ee){
        _searchEngine = ee;
    }
    public SearchEngine getSearchEngine(){
        return _searchEngine;
    }
    
    /**
     * Gets a URL to use for imageURL field when no value found
     * from child service
     * @return String containing URL to image file
     */
    public String getUnsetImageURL(){
        return _unsetImageURL;
    }
    
    /**
     * Gets alternate URL prefix for the host running this service.
     * @return String containing alternate URL ending with / or empty
     *         string if not is set
     */
    public String getHostURL(){
        return _searchHostURL;
    }
    
    public String getSearchDatabaseDirectory(){
        return _searchDatabaseDir;
    }
    
    public String getSearchTaskDirectory(){
        return _searchTaskDir;
    }

    public File getSourceConfigurationsFile(){
        return new File(this.getSearchDatabaseDirectory()+ File.separator + _sourceConfiguration);
    }
    
    public SourceConfigurations getSourceConfigurations(){
        ObjectMapper mapper = new ObjectMapper();
        File dbres = getSourceConfigurationsFile();
        try {
        	SourceConfigurations sc = mapper.readValue(dbres, SourceConfigurations.class);
        	if (sc != null) {
	        	SourceConfiguration enrichConfig = sc.getSourceConfigurationByName(SourceResult.ENRICHMENT_SERVICE);
	        	if (enrichConfig != null) {
		        	String enrichEndPoint = enrichConfig.getEndPoint();
		        	if (enrichEndPoint.substring(enrichEndPoint.length() - 1).equals("/")) {
		        		enrichConfig.setEndPoint(enrichEndPoint.substring(0, enrichEndPoint.length() - 1));
		        	}
	        	}
	        	
	        	SourceConfiguration interactomePpiConfig = sc.getSourceConfigurationByName(SourceResult.INTERACTOME_PPI_SERVICE);
	        	if (interactomePpiConfig != null) {
	        		String interactomePpiEndPoint = interactomePpiConfig.getEndPoint();
	        		if (!interactomePpiEndPoint.substring(interactomePpiEndPoint.length() - 1).equals("/")) {
	        			interactomePpiConfig.setEndPoint(interactomePpiEndPoint + "/");
	        		}
	        	}
	        	
	        	SourceConfiguration interactomeAssociationConfig = sc.getSourceConfigurationByName(SourceResult.INTERACTOME_GENEASSOCIATION_SERVICE);
	        	if (interactomeAssociationConfig != null) {
	        		String interactomeAssociationEndPoint = interactomeAssociationConfig.getEndPoint();
	        		if (!interactomeAssociationEndPoint.substring(interactomeAssociationEndPoint.length() - 1).equals("/")) {
	        			interactomeAssociationConfig.setEndPoint(interactomeAssociationEndPoint + "/");
	        		}
	        	}
        	}
        	
        	/*
        	List<SourceConfiguration> sources = sc.getSources();
        	/*
        	for (SourceConfiguration source : sources) {
        		//String sourceName = source.getName();
        		
        		if (source.getName().equals(SourceResult.ENRICHMENT_SERVICE)) {
        			//String endPoint = source.getEndPoint();
        			//if (endPoint.substring(endPoint.length() - 1).equals("/")) {
        				//source.setEndPoint(endPoint.substring(0, endPoint.length() - 1));
        			//}
        		} else if (source.getName().equals(SourceResult.INTERACTOME_PPI_SERVICE) || 
        				   source.getName().equals(SourceResult.INTERACTOME_GENEASSOCIATION_SERVICE)) {
        			//String endPoint = source.getEndPoint();
        			//if (!endPoint.substring(endPoint.length() - 1).equals("/")) {
        				//source.setEndPoint(endPoint + "/");
        			//}
        		}
        	}*/
            return sc;
        }
        catch(IOException io){
            _logger.error("caught io exception trying to load " + dbres.getAbsolutePath(), io);
        }
        return null;
    }
    
    public long getSourcePollingInterval() {
    	try {
    	return Long.valueOf(_sourcePollingInterval);
    	} catch (NumberFormatException e) {
    		_logger.error("caught exception parsing source.polling.interval value", e);
    		return DEFAULT_SOURCE_POLLING_INTERVAL;
    	}
    	}
    
    /**
     * Using configuration create 
     * @return ndex client
     */
    protected NdexRestClientModelAccessLayer getNDExClient(Properties props){
        
        try {
            String user = props.getProperty(Configuration.NDEX_USER);
            String pass = props.getProperty(Configuration.NDEX_PASS);
            String server = props.getProperty(Configuration.NDEX_SERVER);
            String useragent = props.getProperty(Configuration.NDEX_USERAGENT,"IntegratedSearch/0.1.0");
            NdexRestClient nrc = new NdexRestClient(user, pass, server, useragent);
            _client = new NdexRestClientModelAccessLayer(nrc);
            return _client;
        }
        catch(JsonProcessingException jpe){
            _logger.error("Caught JsonProcessingException ", jpe);
        }
        catch(IOException io){
            _logger.error("Caught IOException", io);
        }
        catch(NdexException ne){
            _logger.error("Caught NdexException", ne);
        }
        catch(Exception ex){
            _logger.error("Caught Exception", ex);
        }
        return null;
    }
    
    /**
     * Gets singleton instance of configuration
     * @return {@link org.ndexbio.ndexsearch.rest.services.Configuration} object with configuration loaded
     * @throws SearchException If there was an error reading configuration
     */
    public static Configuration getInstance()
    {
    	if ( INSTANCE == null)  { 
            
            try {
                String configPath = null;
                if (_alternateConfigurationFile != null){
                    configPath = _alternateConfigurationFile;
                    _logger.info("Alternate configuration path specified: " + configPath);
                } else {
                    try {
                        configPath = System.getenv(Configuration.NDEX_SEARCH_CONFIG);
                    } catch(SecurityException se){
                        _logger.error("Caught security exception ", se);
                    }
                }
                if (configPath == null){
                    InitialContext ic = new InitialContext();
                    configPath = (String) ic.lookup("java:comp/env/" + Configuration.NDEX_SEARCH_CONFIG); 

                }
                INSTANCE = new Configuration(configPath);
            } catch (Exception ex) {
                _logger.error("Error loading configuration", ex);
            }
    	} 
        return INSTANCE;
    }
    
    /**
     * Reloads configuration
     * @return {@link org.ndexbio.ndexsearch.rest.services.Configuration} object
     */
    public static Configuration reloadConfiguration()  {
        INSTANCE = null;
        return getInstance();
    }
    
    /**
     * Lets caller set an alternate path to configuration. Added so the command
     * line application can set path to configuration and it makes testing easier
     * @param configFilePath - Path to configuration file
     */
    public static void  setAlternateConfigurationFile(final String configFilePath) {
    	_alternateConfigurationFile = configFilePath;
    }
}
