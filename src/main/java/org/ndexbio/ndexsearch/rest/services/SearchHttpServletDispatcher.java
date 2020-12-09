package org.ndexbio.ndexsearch.rest.services;

import java.io.IOException;
import java.io.InputStream;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import org.jboss.resteasy.plugins.server.servlet.HttpServletDispatcher;
import org.ndexbio.ndexsearch.rest.engine.BasicSearchEngineFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ndexbio.ndexsearch.rest.engine.SearchEngine;
/**
 *
 * @author churas
 */
public class SearchHttpServletDispatcher extends HttpServletDispatcher {
    
    static Logger _logger = LoggerFactory.getLogger(SearchHttpServletDispatcher.class.getSimpleName());

    private static String _version = "";
    private static String _buildNumber = "";
    private SearchEngine _searchEngine;
    private Thread _searchEngineThread;
    
    
    public SearchHttpServletDispatcher(){
        super();
        _logger.info("In constructor");
        createAndStartSearchEngine();
    }
    
    private void createAndStartSearchEngine() {
        BasicSearchEngineFactory fac = new BasicSearchEngineFactory(Configuration.getInstance());
        try {
            _logger.debug("Creating Search Engine from factory");
            _searchEngine = fac.getSearchEngine();
            _logger.debug("Starting Search Engine thread");
            _searchEngineThread = new Thread(_searchEngine);
            _searchEngineThread.start();
            _logger.debug("Search Engine thread running id => {}",
					Long.toString(_searchEngineThread.getId()));
            Configuration.getInstance().setSearchEngine(_searchEngine);
        }
        catch(Exception ex){
            _logger.error("Unable to start enrichment engine", ex);
        }
    }

    @Override
    public void init(javax.servlet.ServletConfig servletConfig) throws ServletException {
        super.init(servletConfig);
        _logger.info("Entering init()");
        updateVersion();
        
        _logger.info("Exiting init()");
    }
    
    @Override
    public void destroy() {
        super.destroy();
        _logger.info("In destroy()");
        if (_searchEngine != null){
            _searchEngine.shutdown();
            _logger.info("Waiting for search engine to shutdown");
            try {
                if (_searchEngineThread != null){
                    _searchEngineThread.join(10000);
                }
            }
            catch(InterruptedException ie){
                _logger.error("Caught exception waiting for search engine to exit", ie);
            }
        } else {
            _logger.error("No search engine found to destroy");
        
        }
    }
    
    /**
     * Reads /META-INFO/MANIFEST.MF for version and build information
     * setting _version and _buildNumber to those values if found.
     */
    private void updateVersion(){
        ServletContext application = getServletConfig().getServletContext();
        try(InputStream inputStream = application.getResourceAsStream("/META-INF/MANIFEST.MF")) {
            if ( inputStream !=null) {
                try {
                    Manifest manifest = new Manifest(inputStream);

                    Attributes aa = manifest.getMainAttributes();	

                    String ver = aa.getValue("NDExSearch-Version");
                    String bui = aa.getValue("NDExSearch-Build"); 
                    _logger.info("NDEx-Version: " + ver + ",Build:" + bui);
                    _buildNumber= bui.substring(0, 5);
                    _version = ver;
                } catch (IOException e) {
                    _logger.error("failed to read MANIFEST.MF", e);
                }     
            }
            else {
                _logger.error("Unable to get /META-INF/MANIFEST.MF");
            }
        } catch (IOException e1) {
            _logger.error("Failed to close InputStream from MANIFEST.MF", e1);
        }
    }
    
    public static String getVersion(){
        return _version;
    }
    
    public static String getBuildNumber(){
        return _buildNumber;
    }
}
