package org.ndexbio.ndexsearch.rest.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.ndexbio.enrichment.rest.model.ErrorResponse;
import org.ndexbio.ndexsearch.rest.model.GeneList;
import org.ndexbio.ndexsearch.rest.model.MutationFrequencies;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Fake mutation frequency service, that can be used for testing
 * @author churas
 */
@Path(Configuration.V_ONE_PATH)
public class MockMutationFrequency {
	
	public static final String MUTATION_FREQ_PATH = "/mutationfrequency";
	
	static Logger _logger = LoggerFactory.getLogger(Status.class);

	@POST
	@Path(MUTATION_FREQ_PATH)
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Operation(summary = "Mock mutation frequency",
			   description="Gets fake mutation frequency information for a list of genes",
			   responses = {
				   @ApiResponse(responseCode = "200", description = "Mutation Frequencies",
						    content = @Content(mediaType = MediaType.APPLICATION_JSON,
							schema = @Schema(implementation = List.class))),
				   @ApiResponse(responseCode = "500", description = "Server Error",
						   content = @Content(mediaType = MediaType.APPLICATION_JSON,
								   schema = @Schema(implementation = ErrorResponse.class)))
			   })
	public Response requestMutationFrequency(@RequestBody(description="Query", required = true,
			content = @Content(schema = @Schema(implementation = MutationFrequencies.class))) final String geneListStr){

		ObjectMapper omappy = new ObjectMapper();
		try {
			GeneList gList = omappy.readValue(geneListStr, GeneList.class);
			MutationFrequencies mFreqs = new MutationFrequencies();
			if (gList != null || gList.getGenes() != null){
				Map<String, Double> mutFreqs = new HashMap<>();
				mFreqs.setMutationFrequencies(mutFreqs);
			
				for (String gene : gList.getGenes()){
					mutFreqs.put(gene, Math.random()*100.0);
				}
			}
			return Response.ok().type(MediaType.APPLICATION_JSON).entity(omappy.writeValueAsString(mFreqs)).build();
		} catch(Exception ex){
            ErrorResponse er = new ErrorResponse("Error requesting search", ex);
            return Response.serverError().type(MediaType.APPLICATION_JSON).entity(er.asJson()).build();
        }
	}	
}
