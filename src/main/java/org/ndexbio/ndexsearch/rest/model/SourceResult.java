/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ndexbio.ndexsearch.rest.model;

/**
 *
 * @author churas
 */
public class SourceResult {
    private String _uuid;
    private String _description;
    private String _name;
    private String _numberOfNetworks;
    private String _status;
    private String _endPoint;
    private String _version;
    

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
    
    
    
}
