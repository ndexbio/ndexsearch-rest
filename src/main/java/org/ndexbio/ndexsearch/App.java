/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ndexbio.ndexsearch;


import ch.qos.logback.classic.Level;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Properties;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.util.RolloverFileOutputStream;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.ndexbio.enrichment.rest.model.DatabaseResult;
import org.ndexbio.enrichment.rest.model.DatabaseResults;
import org.ndexbio.enrichment.rest.model.EnrichmentQuery;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.ndexbio.ndexsearch.rest.model.SourceResult;
import org.ndexbio.ndexsearch.rest.model.InternalSourceResults;
import org.ndexbio.ndexsearch.rest.services.Configuration;
import org.ndexbio.ndexsearch.rest.services.SearchHttpServletDispatcher;

/**
 *
 * @author churas
 */
public class App {
    
    static Logger _logger = LoggerFactory.getLogger(App.class);

    
    
    /**
     * Sets log directory for embedded Jetty
     */
    public static final String RUNSERVER_LOGDIR = "runserver.log.dir";
    
    /**
     * Sets port for embedded Jetty
     */
    public static final String RUNSERVER_PORT = "runserver.port";
        
    /**
     * Sets context path for embedded Jetty
     */
    public static final String RUNSERVER_CONTEXTPATH = "runserver.contextpath";
    
    
    public static final String MODE = "mode";
    public static final String CONF = "conf";    
    public static final String EXAMPLE_CONF_MODE = "exampleconf";
    public static final String EXAMPLE_SOURCERES_MODE = "examplesourceresults";
    public static final String RUNSERVER_MODE = "runserver";
    public static final String TESTQUERY_MODE = "testquery";
    
    public static final String SUPPORTED_MODES = ", " + EXAMPLE_CONF_MODE +
                                                    ", " + EXAMPLE_SOURCERES_MODE +
                                                    ", " + RUNSERVER_MODE + 
                                                    ", " + TESTQUERY_MODE;
    
    public static void main(String[] args){

        final List<String> helpArgs = Arrays.asList("h", "help", "?");
        try {
            OptionParser parser = new OptionParser() {

                {
                    accepts(MODE, "Mode to run. Supported modes: " + SUPPORTED_MODES).withRequiredArg().ofType(String.class).required();
                    accepts(CONF, "Configuration file")
                            .withRequiredArg().ofType(String.class);
                    acceptsAll(helpArgs, "Show Help").forHelp();
                }
            };
            
            OptionSet optionSet = null;
            try {
                optionSet = parser.parse(args);
            } catch (OptionException oe) {
                System.err.println("\nThere was an error parsing arguments: "
                        + oe.getMessage() + "\n\n");
                parser.printHelpOn(System.err);
                System.exit(1);
            }

            //help check
            for (String helpArgName : helpArgs) {
                if (optionSet.has(helpArgName)) {
                    System.out.println("\n\nHelp\n\n");
                    parser.printHelpOn(System.out);
                    System.exit(2);
                }
            }
            
            String mode = optionSet.valueOf(MODE).toString();

            if (mode.equals(EXAMPLE_CONF_MODE)){
                System.out.println(generateExampleConfiguration());
                System.out.flush();
                return;
            }
            if (mode.equals(EXAMPLE_SOURCERES_MODE)){
                System.out.println(generateExampleSourceResults());
                System.out.flush();
                return;
            }
                        
            if (mode.equals(RUNSERVER_MODE)){
                Configuration.setAlternateConfigurationFile(optionSet.valueOf(CONF).toString());
                Properties props = getPropertiesFromConf(optionSet.valueOf(CONF).toString());
                ch.qos.logback.classic.Logger rootLog = 
        		(ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
		rootLog.setLevel(Level.INFO);
                String logDir = props.getProperty(App.RUNSERVER_LOGDIR, ".");
                RolloverFileOutputStream os = new RolloverFileOutputStream(logDir + File.separator + "ndexsearch_yyyy_mm_dd.log", true);
		
		
                final int port = Integer.valueOf(props.getProperty(App.RUNSERVER_PORT, "8080"));
                System.out.println("\nSpinning up server for status invoke: http://localhost:" + Integer.toString(port) + "/status\n\n");
                System.out.flush();
                
                //We are creating a print stream based on our RolloverFileOutputStream
		PrintStream logStream = new PrintStream(os);

                //We are redirecting system out and system error to our print stream.
		System.setOut(logStream);
		System.setErr(logStream);

                final Server server = new Server(port);

                final ServletContextHandler webappContext = new ServletContextHandler(server, props.getProperty(App.RUNSERVER_CONTEXTPATH, "/"));
                
                HashMap<String, String> initMap = new HashMap<>();
                initMap.put("resteasy.servlet.mapping.prefix", "/");
                initMap.put("javax.ws.rs.Application", "org.ndexbio.ndexsearch.rest.SearchApplication");
                final ServletHolder restEasyServlet = new ServletHolder(
                     new SearchHttpServletDispatcher());
                
                restEasyServlet.setInitOrder(1);
                restEasyServlet.setInitParameters(initMap);
                webappContext.addServlet(restEasyServlet, "/*");
                
                ContextHandlerCollection contexts = new ContextHandlerCollection();
                contexts.setHandlers(new Handler[] { webappContext });
 
                server.setHandler(contexts);
                
                server.start();
                Log.getRootLogger().info("Embedded Jetty logging started.", new Object[]{});
	    
                System.out.println("Server started on port " + port);
                server.join();
                return;
            }
            
        }
        catch(Exception ex){
            ex.printStackTrace();
        }
    }
    
    public static Properties getPropertiesFromConf(final String path) throws IOException, FileNotFoundException {
        Properties props = new Properties();
        props.load(new FileInputStream(path));
        return props;
    }
    
    /**
     * Generates an example {@link org.ndexbio.ndexsearch.rest.services.Configuration#SOURCE_RESULTS_JSON_FILE}
     * file as String
     * @return String 
     * @throws Exception If there was a problem generating output
     */
    public static String generateExampleSourceResults() throws Exception {
        SourceResult sr = new SourceResult();
        sr.setDescription("This is a description of enrichment source");
        sr.setName("enrichment");
        sr.setNumberOfNetworks("350");
        String sruuid = "eeb4af50-83c4-4e33-ac21-87142403589b";
        sr.setUuid(sruuid);
        sr.setEndPoint("http://localhost:8085/enrichment");
        sr.setVersion("0.1.0");
        sr.setStatus("ok");
        DatabaseResult dr = new DatabaseResult();
        dr.setDescription("This is a description of a signor database");
        dr.setName("signor");
        dr.setNumberOfNetworks("50");
        String druuid = "89a90a24-2fa8-4a57-ae4b-7c30a180e8e6";
        dr.setUuid(druuid);
        
        DatabaseResult drtwo = new DatabaseResult();
        drtwo.setDescription("This is a description of a ncipid database");
        drtwo.setName("ncipid");
        drtwo.setNumberOfNetworks("200");
        String drtwouuid = "e508cf31-79af-463e-b8b6-ff34c87e1734";
        drtwo.setUuid(drtwouuid);
        sr.setDatabases(Arrays.asList(dr, drtwo));
        
        SourceResult srtwo = new SourceResult();
        srtwo.setDescription("This is a description of interactome service");
        srtwo.setName("interactome");
        srtwo.setNumberOfNetworks("2009");
        String srtwouuid = "0857a397-3453-4ae4-8208-e33a283c85ec";
        srtwo.setUuid(srtwouuid);
        srtwo.setEndPoint("http://localhost:8086/interactome");
        srtwo.setVersion("0.1.1a1");
        srtwo.setStatus("ok");
        
        SourceResult srthree = new SourceResult();
        srthree.setDescription("This is a description of keyword service");
        srthree.setName("keyword");
        srthree.setNumberOfNetworks("2009");
        String srthreeuuid = "33b9c3ca-13e5-48b9-bcd2-09070203350a";
        srthree.setUuid(srthreeuuid);
        srthree.setEndPoint("http://localhost:8086/keyword");
        srthree.setVersion("0.2.0");
        srthree.setStatus("ok");
        
        
        InternalSourceResults idr = new InternalSourceResults();
        idr.setResults(Arrays.asList(sr, srtwo, srthree));
        ObjectMapper mappy = new ObjectMapper();
        
        return mappy.writerWithDefaultPrettyPrinter().writeValueAsString(idr);
    }
    /**
     * Generates example Configuration file writing to standard out
     * @return Example configuration as String
     */
    public static String generateExampleConfiguration() {
        StringBuilder sb = new StringBuilder();
        sb.append("# Example configuration file for Search service\n\n");
        
        sb.append("\n# Sets Search database directory\n");
        sb.append(Configuration.DATABASE_DIR + " = /tmp\n");
        
        sb.append("\n# Sets Search task directory where results from queries are stored\n");
        sb.append(Configuration.TASK_DIR + " = /tmp/tasks\n");
        
        sb.append("\n# Run Service under embedded Jetty command line parameters\n");
        sb.append(App.RUNSERVER_CONTEXTPATH + " = /\n");
        sb.append(App.RUNSERVER_LOGDIR + " = /tmp/log\n");
        sb.append(App.RUNSERVER_PORT + " = 8080\n");
        
        sb.append("\n# Sets name of json file containing source results.\n# This file ");
        sb.append("expected to reside in " + Configuration.DATABASE_DIR + " directory\n");
        sb.append(Configuration.SOURCE_RESULTS_JSON_FILE+ " = " + Configuration.SOURCE_RESULTS_JSON_FILE + "\n");
        sb.append(Configuration.NDEX_USER+ " = bob\n");
        sb.append(Configuration.NDEX_PASS+ " = somepassword\n");
        sb.append(Configuration.NDEX_SERVER+ " = public.ndexbio.org\n");
        sb.append(Configuration.NDEX_USERAGENT+ " = NDExSearch/1.0\n");
        return sb.toString();
    }    
}
