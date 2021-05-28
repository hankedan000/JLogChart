/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.hankedan.jlogchart;

import com.hankedan.jlogchart.util.VectorUtils;
import java.awt.Color;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;

/**
 *
 * @author daniel
 */
public class XY_Series extends Series {
    public interface XY_SeriesChangeListener {
        public void xySeriesDataChanged(String seriesName);
    }
    
    private static final Logger logger = Logger.getLogger(XY_Series.class.getName());
    
    private List<Vector2D> data;
    private Vector2D minValue = null;
    private Vector2D maxValue = null;
    private final List<XY_SeriesChangeListener> listeners = new ArrayList();

    public XY_Series(String name, Color color, List<Vector2D> data) {
        this(name,"",color,data);
    }

    public XY_Series(String name, String subTitle, Color color, List<Vector2D> data) {
        super(name,subTitle,color);
        setData(data);
    }

    public Vector2D minValue() {
        return minValue;
    }

    public Vector2D maxValue() {
        return maxValue;
    }
    
    public Rectangle2D getBoundingRectangle() {
        if (minValue == null || maxValue == null) {
            throw new RuntimeException("series data is empty.");
        }
        
        return new Rectangle2D.Double(
                minValue.getX(),
                minValue.getY(),
                maxValue.getX() - minValue.getX(),
                maxValue.getY() - minValue.getY());
    }

    public XY_Series setData(List<Vector2D> newData) {
        minValue = null;
        maxValue = null;
        for (Vector2D val : newData) {
            if (minValue == null) {
                minValue = val;
            } else {
                minValue = VectorUtils.min(minValue, val);
            }
            if (maxValue == null) {
                maxValue = val;
            } else {
                maxValue = VectorUtils.max(maxValue, val);
            }
        }
        this.data = newData;
        for (XY_SeriesChangeListener l : listeners) {
            l.xySeriesDataChanged(name);
        }
        return this;
    }

    public List<Vector2D> getData() {
        return Collections.unmodifiableList(data);
    }

    @Override
    public int size() {
        if (data == null) {
            return 0;
        } else {
            return data.size();
        }
    }
}
