/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ndexbio.ndexsearch.rest.model.comparators;

import java.util.Comparator;
import org.ndexbio.ndexsearch.rest.model.SourceQueryResults;

/**
 * Sorts {@link org.ndexbio.ndexsearch.rest.model.SourceQueryResults} by 
 * {@link org.ndexbio.ndexsearch.rest.model.SourceQueryResults#getSourceRank()}
 * with lower rank value appearing first.
 * @author churas
 */
public class SourceQueryResultsBySourceRank implements Comparator{
    
    /**
     * Compares two {@link org.ndexbio.ndexsearch.rest.model.SourceQueryResult} objects
     * by rank
     * @param o1 {@link org.ndexbio.ndexsearch.rest.model.SourceQueryResult} object 1 to compare
     * @param o2 {@link org.ndexbio.ndexsearch.rest.model.SourceQueryResult} object 2 to compare
     * @return If both {@code o1} and {@code o2} are null 0 is returned. If only
     *         {@code o1} is null 1 is returned. If only {@code o2} is null -1 is returned.
     *        -1 if {@code o1}'s rank is lower then {@code o2}, 0 if same else 1.
     * @throws ClassCastException if either input parameter cannot be cast to {@link org.ndexbio.ndexsearch.rest.model.SourceQueryResult}       
     */
    @Override
    public int compare(Object o1, Object o2) {
        if (o1 == null && o2 == null){
            return 0;
        }
        if (o1 != null && o2 == null){
            return -1;
        }
        if (o1 == null && o2 != null){
            return 1;
        }
        if (o1 instanceof SourceQueryResults == false){
            throw new ClassCastException ("o1 is not of type SourceQueryResults");
        }
        if (o2 instanceof SourceQueryResults == false){
            throw new ClassCastException ("o2 is not of type SourceQueryResults");
        }
        SourceQueryResults sqr1 = (SourceQueryResults)o1;
        SourceQueryResults sqr2 = (SourceQueryResults)o2;
        if (sqr1.getSourceRank() < sqr2.getSourceRank()){
            return -1;
        }
        if (sqr1.getSourceRank() == sqr2.getSourceRank()){
            return 0;
        }
        return 1;
    }

}
