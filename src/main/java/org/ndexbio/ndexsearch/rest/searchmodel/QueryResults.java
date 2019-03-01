/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ndexbio.ndexsearch.rest.searchmodel;

import java.util.List;

/**
 *
 * @author churas
 */
public class QueryResults extends QueryStatus {

    private int _numberOfHits;
    private int _start;
    private int _size;
    private List<QueryResult> _results;

    public QueryResults(){
        super();
    }
    public QueryResults(long startTime){
        super(startTime);
    }
    
    /**
     * Copy constructor that copies the {@code eqr} data with exception of the
     * {@link #getResults()} data. That is set to value of {@code results} parameter
     * passed in.
     * @param eqr {@link org.ndexbio.ndexsearch.rest.model.EnrichmentQueryResults} to copy data from
     * @param results List of {@link org.ndexbio.ndexsearch.rest.searchmodel.QueryResult} 
     *        to set as results for this object
     */
    public QueryResults(QueryResults eqr, List<QueryResult> results){
        super(eqr);
        this._numberOfHits = eqr.getNumberOfHits();
        this._start = eqr.getStart();
        this._size = eqr.getSize();
        _results = results;
    }
    
    public QueryResults updateStartTime(QueryResults eqs) {
        super.updateStartTime(eqs);
        return this;
    }

    public int getNumberOfHits() {
        return _numberOfHits;
    }

    public void setNumberOfHits(int _numberOfHits) {
        this._numberOfHits = _numberOfHits;
    }

    public int getStart() {
        return _start;
    }

    public void setStart(int _start) {
        this._start = _start;
    }

    public int getSize() {
        return _size;
    }

    public void setSize(int _size) {
        this._size = _size;
    }

    public List<QueryResult> getResults() {
        return _results;
    }

    public void setResults(List<QueryResult> _results) {
        this._results = _results;
    }
    
}
