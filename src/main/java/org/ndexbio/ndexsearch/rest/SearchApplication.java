package org.ndexbio.ndexsearch.rest; 

import io.swagger.v3.jaxrs2.integration.resources.AcceptHeaderOpenApiResource;
import io.swagger.v3.jaxrs2.integration.resources.OpenApiResource;

import javax.ws.rs.core.Application;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.ndexbio.ndexsearch.rest.services.MockMutationFrequency;
import org.ndexbio.ndexsearch.rest.services.Search;
import org.ndexbio.ndexsearch.rest.services.SearchSource;
import org.ndexbio.ndexsearch.rest.services.Status;

public class SearchApplication extends Application {

    private final Set<Object> _singletons = new HashSet<>();
    public SearchApplication() {        
        // Register our hello service
    }
    @Override
    public Set<Object> getSingletons() {
        return _singletons;
    }
    
    @Override
    public Set<Class<?>> getClasses() {
        return Stream.of(Search.class,
                SearchSource.class,
                Status.class,
                OpenApiResource.class,
				MockMutationFrequency.class,
                AcceptHeaderOpenApiResource.class).collect(Collectors.toSet());
    }
}