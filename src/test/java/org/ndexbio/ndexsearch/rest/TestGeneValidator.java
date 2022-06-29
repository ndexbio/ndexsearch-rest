package org.ndexbio.ndexsearch.rest;



import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.ndexbio.ndexsearch.rest.model.ValidatedQueryGenes;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class TestGeneValidator {

	@Test
    public void testFull() throws URISyntaxException, JsonProcessingException {
		GeneValidator validator = new GeneValidator(new File(
		getClass().getClassLoader().getResource("test_genes.tsv").toURI()));
		
        assertEquals(9, validator.getGeneSymbolSet().size());
        assertEquals(16, validator.getAliasMap().size());

        assertTrue(validator.getGeneSymbolSet().contains("A1BG"));
        assertTrue(validator.getGeneSymbolSet().contains("A2M"));
        assertTrue(validator.getGeneSymbolSet().contains("A2M-AS1"));
        assertTrue(validator.getGeneSymbolSet().contains("A2MP1"));
        
        assertEquals("A1BG-AS1",validator.getAliasMap().get("A1BGAS") );
        assertEquals("A2MP1",validator.getAliasMap().get("A2MP") );
        assertEquals("A1BG-AS1",validator.getAliasMap().get("FLJ23569") );
        assertEquals("A2ML1",validator.getAliasMap().get("p170") );
        assertEquals("A2ML1",validator.getAliasMap().get("CPAMD9") );
        
        List<String> searchTerms = Arrays.asList("A1BG", "CPAMD5", "CPAMD9", "A2MP1", "foo", "bar");
        
        
        ValidatedQueryGenes result = validator.validateHumanGenes(searchTerms);
        
       ObjectMapper mapper = new ObjectMapper();
   
		String s0 = mapper.writeValueAsString( result);
		System.out.println(s0);
     
		assertEquals("{\"queryGenes\":[\"A1BG\",\"A2M\",\"A2ML1\",\"A2MP1\"],\"invalid\":[\"bar\",\"foo\"],\"normalizedGenes\":{\"A2M\":\"CPAMD5\",\"A2ML1\":\"CPAMD9\"}}", s0);
	
	}


}