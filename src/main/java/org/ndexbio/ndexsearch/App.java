/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ndexbio.ndexsearch;


import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.core.OutputStreamAppender;
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
import org.ndexbio.ndexsearch.rest.CorsFilter;
import org.ndexbio.ndexsearch.rest.RequestLoggingFilter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.ndexbio.ndexsearch.rest.model.SourceConfiguration;
import org.ndexbio.ndexsearch.rest.model.SourceConfigurations;
import org.ndexbio.ndexsearch.rest.services.Configuration;
import org.ndexbio.ndexsearch.rest.services.SearchHttpServletDispatcher;

/**
 *
 * @author churas
 */
public class App {
    
    static Logger _logger = LoggerFactory.getLogger(App.class);

    
    /**
     * Sets logging level valid values DEBUG INFO WARN ALL ERROR
     */
    public static final String RUNSERVER_LOGLEVEL = "runserver.log.level";
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
    public static final String EXAMPLE_SOURCE_CONF_MODE = "examplesourceconfig";
    public static final String RUNSERVER_MODE = "runserver";
    public static final String TESTQUERY_MODE = "testquery";
    
    public static final String SUPPORTED_MODES = ", " + EXAMPLE_CONF_MODE +
    						    ", " + EXAMPLE_SOURCE_CONF_MODE +
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
            if (mode.equals(EXAMPLE_SOURCE_CONF_MODE)){
                System.out.println(generateExampleSourceConfigurations());
                System.out.flush();
                return;
            }
                        
            if (mode.equals(RUNSERVER_MODE)){
                Configuration.setAlternateConfigurationFile(optionSet.valueOf(CONF).toString());
                Properties props = getPropertiesFromConf(optionSet.valueOf(CONF).toString());
                ch.qos.logback.classic.Logger rootLog = 
        		(ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
                rootLog.setLevel(Level.toLevel(props.getProperty(App.RUNSERVER_LOGLEVEL, "INFO")));
                String logDir = props.getProperty(App.RUNSERVER_LOGDIR, ".");
                RolloverFileOutputStream os = new RolloverFileOutputStream(logDir + File.separator + "ndexsearch_yyyy_mm_dd.log", true);
			  
			
				LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();

				PatternLayoutEncoder logEncoder = new PatternLayoutEncoder();
				logEncoder.setContext(lc);
				logEncoder.setPattern("[%date]\t%msg%n");
				logEncoder.start();
               
				
                RolloverFileOutputStream requestOS = new RolloverFileOutputStream(logDir + File.separator + "requests_yyyy_mm_dd.log", true);
				
				OutputStreamAppender osa = new OutputStreamAppender();
				osa.setOutputStream(requestOS);
				osa.setContext(lc);
				osa.setEncoder(logEncoder);
				osa.setName(RequestLoggingFilter.REQUEST_LOGGER_NAME + "appender");
				osa.start();
				ch.qos.logback.classic.Logger requestLog = 
					  (ch.qos.logback.classic.Logger) lc.getLogger(RequestLoggingFilter.REQUEST_LOGGER_NAME);
                requestLog.setLevel(Level.toLevel("INFO"));
				requestLog.setAdditive(false);
				requestLog.addAppender(osa);
		
                final int port = Integer.valueOf(props.getProperty(App.RUNSERVER_PORT, "8080"));
                System.out.println("\nSpinning up server for status invoke: http://localhost:" + Integer.toString(port) + Configuration.V_ONE_PATH + "/status\n\n");
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
                webappContext.addFilter(CorsFilter.class, "/*", null);
				webappContext.addFilter(RequestLoggingFilter.class, "/*", null);
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
     * Generates an example source configurations file
     * file as String
     * @return String 
     * @throws Exception If there was a problem generating output
     */
    public static String generateExampleSourceConfigurations() throws Exception {
        SourceConfiguration scA = new SourceConfiguration();
        scA.setDescription("This is a description of enrichment source");
        scA.setName("enrichment");
        scA.setEndPoint("http://localhost:8095/enrichment/v1/");
     
        
        SourceConfiguration scB = new SourceConfiguration();
        scB.setDescription("This is a description of interactome-ppi service");
        scB.setName("interactome-ppi");
        scB.setEndPoint("http://localhost:8096/interactome/ppi/v1/");
   
        
        SourceConfiguration scC = new SourceConfiguration();
        scC.setDescription("This is a description of interactome-association service");
        scC.setName("interactome-association");
        scC.setEndPoint("http://localhost:8096/interactome/geneassociation/v1/");  
        
        SourceConfigurations scs = new  SourceConfigurations();
        scs.setSources(Arrays.asList(scA, scB, scC));
        ObjectMapper mappy = new ObjectMapper();
        
        return mappy.writerWithDefaultPrettyPrinter().writeValueAsString(scs);
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
        
        sb.append("\n# Sets imageURL to use for results that lack imageURL\n");
        sb.append(Configuration.UNSET_IMAGE_URL);
        sb.append(" = http://ndexbio.org/images/new_landing_page_logo.06974471.png\n");
        
        sb.append("\n# Sets Search task directory where results from queries are stored\n");
        sb.append(Configuration.TASK_DIR + " = /tmp/tasks\n");
        
        sb.append("# Sets HOST URL prefix (value is prefixed to Location header when query is invoked. Can be commented out)\n");
        sb.append("# " + Configuration.HOST_URL + " = http://ndexbio.org\n");
        
        sb.append("\n# Run Service under embedded Jetty command line parameters\n");
        sb.append(App.RUNSERVER_CONTEXTPATH + " = /\n");
        sb.append(App.RUNSERVER_LOGDIR + " = /tmp/log\n");
        sb.append(App.RUNSERVER_PORT + " = 8080\n");
        sb.append("# Valid log levels DEBUG INFO WARN ERROR ALL\n");
        sb.append(App.RUNSERVER_LOGLEVEL + " = INFO\n");
        
        sb.append("\n# Sets name of json file containing source results.\n# This file ");
        sb.append("expected to reside in " + Configuration.DATABASE_DIR + " directory\n");
        sb.append(Configuration.SOURCE_CONFIGURATIONS_JSON_FILE+ " = " + Configuration.SOURCE_CONFIGURATIONS_JSON_FILE + ".json\n");
        sb.append(Configuration.NDEX_USER+ " = bob\n");
        sb.append(Configuration.NDEX_PASS+ " = somepassword\n");
        sb.append(Configuration.NDEX_SERVER+ " = public.ndexbio.org\n");
        sb.append(Configuration.NDEX_USERAGENT+ " = NDExSearch/1.0\n");
        return sb.toString();
    }    
}
