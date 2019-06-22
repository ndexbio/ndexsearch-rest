package org.ndexbio.ndexsearch.rest.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Contains information about sources that can
 * be queried by this search
 * @author churas
 */
public class SourceConfigurations {
    
    private List<SourceConfiguration> _sources;

    /**
     * Constructor
     */
    public SourceConfigurations(){
        _sources = new ArrayList<SourceConfiguration>();
    }
    

    
    /**
     * Gets list of source configurations
     * @return 
     */
    public List<SourceConfiguration> getSources() {
        return _sources;
    }

    /**
     * Sets the list of source configurations
     * @param _sources 
     */
    public void setSources(List<SourceConfiguration> _sources) {
        this._sources = _sources;
    }
    
}
