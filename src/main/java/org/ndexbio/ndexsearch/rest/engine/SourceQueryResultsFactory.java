package org.ndexbio.ndexsearch.rest.engine;

import org.ndexbio.ndexsearch.rest.model.Query;
import org.ndexbio.ndexsearch.rest.model.SourceQueryResults;
import org.ndexbio.ndexsearch.rest.model.SourceResults;

/**
 *
 * @author churas
 */
public interface SourceQueryResultsFactory {
	
	public void updateSourceResults(final SourceResults sourceResults);
	
	public SourceQueryResults getSourceQueryResults(final Query query);
	
}
