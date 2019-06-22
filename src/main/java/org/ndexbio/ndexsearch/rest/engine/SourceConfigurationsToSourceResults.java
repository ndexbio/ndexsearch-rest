package org.ndexbio.ndexsearch.rest.engine;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.ndexbio.ndexsearch.rest.model.InternalSourceResults;
import org.ndexbio.ndexsearch.rest.model.SourceConfigurations;
import org.ndexbio.ndexsearch.rest.model.SourceResult;

public class SourceConfigurationsToSourceResults implements Function<SourceConfigurations, InternalSourceResults>{

	@Override
	public InternalSourceResults apply(SourceConfigurations sourceConfigurations) {
		final InternalSourceResults internalSourceResults = new InternalSourceResults();
		final List<SourceResult> sourceResults = sourceConfigurations.getSources().stream().map(new SourceConfigToSourceResult()).collect(Collectors.toList());
		internalSourceResults.setResults(sourceResults);
		return internalSourceResults;
	}

}
