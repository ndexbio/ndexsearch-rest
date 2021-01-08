package org.ndexbio.ndexsearch.rest.engine;


import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.ndexbio.ndexsearch.rest.model.SourceConfiguration;
import org.ndexbio.ndexsearch.rest.model.SourceConfigurations;
import org.ndexbio.ndexsearch.rest.model.SourceResult;
import org.ndexbio.ndexsearch.rest.services.Configuration;



/**
 *
 * @author churas
 */
public class TestBasicSearchEngineFactory {

	@Rule
	public TemporaryFolder _folder = new TemporaryFolder();
	
	@Test
	public void testConstructor(){
		Configuration config = mock(Configuration.class);
		BasicSearchEngineFactory factory = new BasicSearchEngineFactory(config);
		verify(config).getNDExClient();
		verify(config).getUnsetImageURL();
		verify(config).getSearchDatabaseDirectory();
		verify(config).getSearchTaskDirectory();
		verify(config).getSourceConfigurations();
		verify(config).getSourcePollingInterval();
	}
	
	@Test
	public void testGetSearchEngineWithAllPlusUnknownSource() throws Exception {
		Configuration config = mock(Configuration.class);
		SourceConfigurations sConfigs = new SourceConfigurations();
		SourceConfiguration scEnrich = new SourceConfiguration();
		scEnrich.setName(SourceResult.ENRICHMENT_SERVICE);
		scEnrich.setEndPoint("enrich");

		SourceConfiguration scKey = new SourceConfiguration();
		scKey.setName(SourceResult.KEYWORD_SERVICE);
		scKey.setEndPoint("keyword");
		UUID keyUUID = UUID.randomUUID();
		scKey.setUuid(keyUUID.toString());
		
		SourceConfiguration scPpi = new SourceConfiguration();
		scPpi.setName(SourceResult.INTERACTOME_PPI_SERVICE);
		scPpi.setEndPoint("ppi");
		
		SourceConfiguration scGene = new SourceConfiguration();
		scGene.setName(SourceResult.INTERACTOME_GENEASSOCIATION_SERVICE);
		scGene.setEndPoint("gene");
		
		SourceConfiguration scInvalid = new SourceConfiguration();
		scInvalid.setName("invalidsource");
		
		sConfigs.setSources(Arrays.asList(scEnrich, scKey, scPpi, scGene, scInvalid));
		
		
		when(config.getSourceConfigurations()).thenReturn(sConfigs);
		
		BasicSearchEngineFactory factory = new BasicSearchEngineFactory(config);
		BasicSearchEngineImpl engine = (BasicSearchEngineImpl)factory.getSearchEngine();
		
		Map<String, SourceEngine> sources = engine.getSources();
		assertEquals(4, sources.size());
		assertTrue(sources.containsKey(SourceResult.KEYWORD_SERVICE));
		assertTrue(sources.containsKey(SourceResult.ENRICHMENT_SERVICE));
		assertTrue(sources.containsKey(SourceResult.INTERACTOME_GENEASSOCIATION_SERVICE));
		assertTrue(sources.containsKey(SourceResult.INTERACTOME_PPI_SERVICE));
	}

	
}
