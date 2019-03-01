/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ndexbio.ndexsearch.rest.searchmodel;

import java.util.List;

/**
 * Represents results of databases
 * @author churas
 */
public class SourceResults {
    
    private List<SourceResult> _results;

    public SourceResults(){
        this(null);
    }
    
    /**
     * Performs a shallow copy of {@link org.ndexbio.ndexsearch.rest.searchmodel.InternalSourceResults}
     * passed in via {@code isr} parameter
     * @param isr {@link org.ndexbio.ndexsearch.rest.searchmodel.InternalSourceResults} object to copy from
     */
    public SourceResults(InternalSourceResults isr){
        if (isr == null){
            return;
        }
        _results = isr.getResults();
    }
    
    public List<SourceResult> getResults() {
        return _results;
    }

    public void setResults(List<SourceResult> _results) {
        this._results = _results;
    }
    
}
