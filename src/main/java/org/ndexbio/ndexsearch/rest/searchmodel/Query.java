package org.ndexbio.ndexsearch.rest.searchmodel;

import java.util.List;

/**
 * Represents a Query
 * @author churas
 */
public class Query {
    
    private List<String> _geneList;
    private List<String> _sourceList;

    public List<String> getGeneList() {
        return _geneList;
    }

    public void setGeneList(List<String> _geneList) {
        this._geneList = _geneList;
    }

    public List<String> getSourceList() {
        return _sourceList;
    }

    public void setSourceList(List<String> _sourceList) {
        this._sourceList = _sourceList;
    }
}
