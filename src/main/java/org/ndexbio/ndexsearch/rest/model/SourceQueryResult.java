/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ndexbio.ndexsearch.rest.model;

import java.util.List;
import java.util.Set;

/**
 *
 * @author churas
 */
public class SourceQueryResult {
    private String _networkUUID;
    private String _description;
    private int _percentOverlap;
    private int _nodes;
    private int _edges;
    private int _rank;
    private Set<String> _hitGenes;


    public String getNetworkUUID() {
        return _networkUUID;
    }

    public void setNetworkUUID(String _networkUUID) {
        this._networkUUID = _networkUUID;
    }

    public String getDescription() {
        return _description;
    }

    public void setDescription(String _description) {
        this._description = _description;
    }

    public int getPercentOverlap() {
        return _percentOverlap;
    }

    public void setPercentOverlap(int _percentOverlap) {
        this._percentOverlap = _percentOverlap;
    }

    public int getNodes() {
        return _nodes;
    }

    public void setNodes(int _nodes) {
        this._nodes = _nodes;
    }

    public int getEdges() {
        return _edges;
    }

    public void setEdges(int _edges) {
        this._edges = _edges;
    }

    public int getRank() {
        return _rank;
    }

    public void setRank(int _rank) {
        this._rank = _rank;
    }

    public Set<String> getHitGenes() {
        return _hitGenes;
    }

    public void setHitGenes(Set<String> _hitGenes) {
        this._hitGenes = _hitGenes;
    }
}
