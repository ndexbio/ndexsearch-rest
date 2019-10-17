package org.ndexbio.ndexsearch.rest.services; // Note your package will be {{ groupId }}.rest

import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import javax.ws.rs.GET;
import javax.ws.rs.core.Response;
import org.ndexbio.ndexsearch.rest.model.SourceResults;
import org.ndexbio.ndexsearch.rest.model.SourceResult;
import org.ndexbio.enrichment.rest.model.DatabaseResult;
import org.ndexbio.enrichment.rest.model.ErrorResponse;
import org.ndexbio.ndexsearch.rest.engine.SearchEngine;

/**
 * Returns Sources that can be queried
 * @author churas
 */
//@Path(Configuration.REST_PATH)
@Path(Configuration.V_ONE_PATH)
public class SearchSource {
    
    public static final String SOURCE_PATH = "/source";
    static Logger logger = LoggerFactory.getLogger(SearchSource.class);
    
    /**
     * Returns status of server 
     * @return {@link org.ndexbio.ndexsearch.rest.model.ServerStatus} as JSON
     */
    @GET 
    @Path(SOURCE_PATH)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Gets list of sources that can be queried",
               description="Result in JSON which is a list of objects with uuid and display\n" +
"name for database that can be queried.",
               responses = {
                   @ApiResponse(responseCode = "200", description = "Success",
                                 content = @Content(mediaType = MediaType.APPLICATION_JSON,
                                   schema = @Schema(implementation = SourceResults.class))),
                   @ApiResponse(responseCode = "500", description = "Server Error",
                                content = @Content(mediaType = MediaType.APPLICATION_JSON,
                                  schema = @Schema(implementation = ErrorResponse.class)))
               })
    public Response getSourceResults() {
        ObjectMapper omappy = new ObjectMapper();

        try {
            SearchEngine searcher = Configuration.getInstance().getSearchEngine();
            if (searcher == null){
                ErrorResponse er = new ErrorResponse();
                er.setMessage("Configuration error");
                er.setDescription("SearchEngine is null, which is most likely due to configuration error");
                er.setErrorCode("searchsource1");
                return Response.serverError().type(MediaType.APPLICATION_JSON).entity(er.asJson()).build();
            }
            SourceResults sr = searcher.getSourceResults();
            if (sr == null){
                ErrorResponse er = new ErrorResponse();
                er.setMessage("No information found on sources");
                er.setDescription("SourceResults is null, which is most likely due to configuration error");
                er.setErrorCode("searchsource2");
                return Response.serverError().type(MediaType.APPLICATION_JSON).entity(er.asJson()).build();
           }
           return Response.ok(omappy.writeValueAsString(sr), MediaType.APPLICATION_JSON).build();
        }
        catch(Exception ex){
            ErrorResponse er = new ErrorResponse("Error querying for source information", ex);
            return Response.serverError().type(MediaType.APPLICATION_JSON).entity(er.asJson()).build();
        }
    }
    
    /**
     * Returns list of objects in the source specified by the given UUID
     */
    @GET
    @Path(SOURCE_PATH + "/{uuid}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Gets list of objects in source specified by uuid",
               description = "Result in JSON which is a list of databases and their associated networks",
               responses = {
            		   @ApiResponse(responseCode = "200", description = "Success",
            				        content = @Content(mediaType = MediaType.APPLICATION_JSON,
            				        schema = @Schema())),
            		   @ApiResponse(responseCode = "500", description = "Server Error",
                                    content = @Content(mediaType = MediaType.APPLICATION_JSON,
                                    schema = @Schema(implementation = ErrorResponse.class)))
               })
    public Response getSourceObjects(@PathParam("uuid") final String uuid) {
    	ObjectMapper omappy = new ObjectMapper();
    	try {
    		SearchEngine searcher = Configuration.getInstance().getSearchEngine();
    		if (searcher == null){
                ErrorResponse er = new ErrorResponse();
                er.setMessage("Configuration error");
                er.setDescription("SearchEngine is null, which is most likely due to configuration error");
                er.setErrorCode("searchsource1");
                return Response.serverError().type(MediaType.APPLICATION_JSON).entity(er.asJson()).build();
            }
    		String service = null;
    		for (SourceResult sr : searcher.getSourceResults().getResults()) {
    			if (uuid.equals(sr.getUuid())) {
    				service = sr.getName();
    			}
    		}
    		if (service == null) {
    			ErrorResponse er = new ErrorResponse();
    			er.setMessage("Error querying for source information");
    			er.setDescription("Uuid did not match a service");
                return Response.serverError().type(MediaType.APPLICATION_JSON).entity(er.asJson()).build();
    		}    		
    		if (service.equals(SourceResult.ENRICHMENT_SERVICE)) {
    			return Response.ok(omappy.writeValueAsString(searcher.getEnrichmentDatabases()), MediaType.APPLICATION_JSON).build();
    		} else if (service.equals(SourceResult.INTERACTOME_PPI_SERVICE)) {
    			return Response.ok(omappy.writeValueAsString(searcher.getInteractomePpiDatabases()), MediaType.APPLICATION_JSON).build();
    		} else if (service.equals(SourceResult.INTERACTOME_GENEASSOCIATION_SERVICE)) {
    			return Response.ok(omappy.writeValueAsString(searcher.getInteractomeGeneAssociationDatabases()), MediaType.APPLICATION_JSON).build();
    		} else {
    			ErrorResponse er = new ErrorResponse();
    			er.setMessage("Error querying for source information");
    			er.setDescription("Service not found");
                return Response.serverError().type(MediaType.APPLICATION_JSON).entity(er.asJson()).build();
    		}
    		
    	} catch (Exception ex) {
            ErrorResponse er = new ErrorResponse("Error querying for source information", ex);
            return Response.serverError().type(MediaType.APPLICATION_JSON).entity(er.asJson()).build();
    	}
    }
}