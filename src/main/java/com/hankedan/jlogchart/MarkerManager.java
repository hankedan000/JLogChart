/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.hankedan.jlogchart;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author daniel
 */
public class MarkerManager {
    private final List<Marker> floatingMarkers = new ArrayList();
    private final Map<String,List<SeriesBoundMarker>> sbmBySeriesName = new HashMap<>();
    
    public void removeSeriesMarkers(String seriesName) {
        sbmBySeriesName.remove(seriesName);
    }
    
    public void addMarker(Marker m) {
        floatingMarkers.add(m);
    }
    
    public void removeMarker(Marker m) {
        floatingMarkers.remove(m);
    }
    
    public void addSeriesBoundMarker(SeriesBoundMarker sbm) {
        String seriesName = sbm.getSeries().name;
        List<SeriesBoundMarker> sbmList = null;
        if (sbmBySeriesName.containsKey(seriesName)) {
            sbmList = sbmBySeriesName.get(seriesName);
        } else {
            sbmList = new ArrayList();
            sbmBySeriesName.put(seriesName, sbmList);
        }
        sbmList.add(sbm);
    }
    
    public void syncMarkersWithSeriesOffset(String seriesName) {
        if (sbmBySeriesName.containsKey(seriesName)) {
            for (SeriesBoundMarker sbm : sbmBySeriesName.get(seriesName)) {
                sbm.updatePosition();
            }
        }
    }
    
    public Map<String,List<SeriesBoundMarker>> getAllSBMsBySeriesName() {
        return Collections.unmodifiableMap(sbmBySeriesName);
    }
    
    public List<Marker> getAllFloatingMarkers() {
        return Collections.unmodifiableList(floatingMarkers);
    }
}
