/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ndexbio.ndexsearch.rest.model;

import java.util.List;

/**
 * Represents results of databases
 * @author churas
 */
public class SourceResults {
    
    private List<SourceResult> _results;

    public List<SourceResult> getResults() {
        return _results;
    }

    public void setResults(List<SourceResult> _results) {
        this._results = _results;
    }
    
}
