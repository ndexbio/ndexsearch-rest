/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ndexbio.ndexsearch.rest.model;

import java.util.List;
import org.ndexbio.enrichment.rest.model.DatabaseResult;

/**
 *
 * @author churas
 */
public class SourceResult {
    
    public static final String ENRICHMENT_SERVICE = "enrichment";
    public static final String KEYWORD_SERVICE = "keyword";
    public static final String INTERACTOME_SERVER = "interactome";
    
    private String _uuid;
    private String _description;
    private String _name;
    private String _numberOfNetworks;
    private String _status;
    private String _endPoint;
    private String _version;
    private List<DatabaseResult> _databases;
    

    public String getUuid() {
        return _uuid;
    }

    public void setUuid(String _uuid) {
        this._uuid = _uuid;
    }

    public String getDescription() {
        return _description;
    }

    public void setDescription(String _description) {
        this._description = _description;
    }

    public String getName() {
        return _name;
    }

    public void setName(String _name) {
        this._name = _name;
    }

    public String getNumberOfNetworks() {
        return _numberOfNetworks;
    }

    public void setNumberOfNetworks(String _numberOfNetworks) {
        this._numberOfNetworks = _numberOfNetworks;
    }

    public String getStatus() {
        return _status;
    }

    public void setStatus(String _status) {
        this._status = _status;
    }

    public String getEndPoint() {
        return _endPoint;
    }

    public void setEndPoint(String _endPoint) {
        this._endPoint = _endPoint;
    }

    public String getVersion() {
        return _version;
    }

    public void setVersion(String _version) {
        this._version = _version;
    }

    public List<DatabaseResult> getDatabases() {
        return _databases;
    }

    public void setDatabases(List<DatabaseResult> _databases) {
        this._databases = _databases;
    }
}
