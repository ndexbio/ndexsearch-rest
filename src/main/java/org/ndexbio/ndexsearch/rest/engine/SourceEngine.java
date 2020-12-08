package org.ndexbio.ndexsearch.rest.engine;

import java.io.InputStream;
import org.ndexbio.model.exceptions.NdexException;
import org.ndexbio.ndexsearch.rest.exceptions.SearchException;
import org.ndexbio.ndexsearch.rest.model.Query;
import org.ndexbio.ndexsearch.rest.model.SourceQueryResults;
import org.ndexbio.ndexsearch.rest.model.SourceResult;

/**
 *
 * @author churas
 */
public interface SourceEngine {
    
	public SourceQueryResults getSourceQueryResults(final Query query);
	
	public void updateSourceResult(SourceResult sRes);
	
	public void updateSourceQueryResults(SourceQueryResults sqRes);
	
	public void delete(final String id) throws SearchException;
	
	public InputStream getOverlaidNetworkAsCXStream(final String id, final String networkId) throws SearchException, NdexException;
	
	public Object getDatabases() throws SearchException, NdexException;
	
	public void shutdown();
}
