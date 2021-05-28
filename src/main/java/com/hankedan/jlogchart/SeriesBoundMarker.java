/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.hankedan.jlogchart;

/**
 *
 * @author daniel
 */
public class SeriesBoundMarker {
    private final Series s;
    private final Marker m;
    private int offset;
    
    public SeriesBoundMarker(Series s, Marker m) {
        this.s = s;
        this.m = m;
        setOffset(0);
    }
    
    public Marker getMarker() {
        return m;
    }
    
    public Series getSeries() {
        return s;
    }
    
    public void setOffset(int newOffset) {
        if (newOffset < 0) {
            newOffset = 0;
        } if (newOffset >= s.size()) {
            newOffset = s.size() - 1;
        }
        offset = newOffset;
        
        if (s instanceof FixedRateSeries) {
            FixedRateSeries frs = (FixedRateSeries)s;
            double val = frs.getData().get(offset);
            m.setPosition(val, val);
        } else if (s instanceof XY_Series) {
            XY_Series xys = (XY_Series)s;
            m.setPosition(xys.getData().get(offset));
        } else {
            throw new UnsupportedOperationException("Unknown series class type " + s.getClass().getName());
        }
    }
    
    public int getOffset() {
        return offset;
    }
}
