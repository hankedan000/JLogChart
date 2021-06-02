/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.hankedan.jlogchart;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.math3.exception.OutOfRangeException;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;

/**
 *
 * @author daniel
 */
public class SeriesBoundMarker {
    private final Logger logger = Logger.getLogger(SeriesBoundMarker.class.getName());
    private final Series s;
    private final Marker m;
    private int absOffset;
    
    public SeriesBoundMarker(Series s, Marker m) {
        this.s = s;
        this.m = m;
        setAbsOffset(0);
    }
    
    public Marker getMarker() {
        return m;
    }
    
    public Series getSeries() {
        return s;
    }
    
    public SeriesBoundMarker setAbsOffset(int newOffset) {
        absOffset = newOffset;
        
        boolean DEBUG_OFFSET = false;
        if (DEBUG_OFFSET) {
            m.setHoverable(true);
            m.setHoverText(Integer.toString(absOffset));
        }
        
        return updatePosition();
    }
    
    public int getAbsOffset() {
        return absOffset;
    }
    
    public SeriesBoundMarker updatePosition() {
        Object val;
        try {
            val = s.getAbsSampleValue(absOffset);
        } catch (OutOfRangeException e) {
            if (absOffset < (int)e.getLo()) {
                val = s.getAbsSampleValue((int)e.getLo());
            } else {
                val = s.getAbsSampleValue((int)e.getHi());
            }
        }
        if (val instanceof Double) {
            m.setPosition((double)val, (double)val);
        } else if (val instanceof Vector2D) {
            m.setPosition((Vector2D)val);
        } else {
            throw new UnsupportedOperationException("Unknown series class type " + s.getClass().getName());
        }
        return this;
    }
}
