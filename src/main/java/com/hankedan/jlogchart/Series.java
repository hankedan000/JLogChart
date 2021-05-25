/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.hankedan.jlogchart;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author daniel
 */
public class Series {
    public interface SeriesChangeListener {
        public void seriesSubTitleChanged(String seriesName, String oldSubTitle, String newSubTitle);
        public void seriesVisibilityChanged(String seriesName, boolean visible);
        public void seriesBoldnessChanged(String seriesName, boolean bold);
        public void seriesColorChanged(String seriesName, Color oldColor, Color newColor);
        public void seriesDataChanged(String seriesName);
        public void seriesOffsetChanged(String seriesName, int oldOffset, int newOffset);
    }
    
    private static final Logger logger = Logger.getLogger(Series.class.getName());
    
    public final String name;
    private String subTitle;
    private List<Double> data;
    private double minValue = Double.MAX_VALUE;
    private double maxValue = Double.MIN_VALUE;
    private Color color = Color.WHITE;
    private boolean visible = true;
    private boolean bolded = false;
    // Allows series samples to be shifted in the x-axis
    private int offset = 0;
    private final List<SeriesChangeListener> listeners = new ArrayList();

    public Series(String name, Color color, List<Double> data) {
        this(name,"",color,data);
    }

    public Series(String name, String subTitle, Color color, List<Double> data) {
        this.name = name;
        this.subTitle = subTitle;
        this.color = color;
        setData(data);
    }

    public double minValue() {
        return minValue;
    }

    public double maxValue() {
        return maxValue;
    }

    public Series setSubTitle(String newSubTitle) {
        String oldSubTitle = this.subTitle;
        this.subTitle = newSubTitle;
        for (SeriesChangeListener l : listeners) {
            l.seriesSubTitleChanged(name, oldSubTitle, newSubTitle);
        }
        return this;
    }
    
    public String getSubTitle() {
        return this.subTitle;
    }
    
    public Series setColor(Color newColor) {
        Color oldColor = this.color;
        this.color = newColor;
        for (SeriesChangeListener l : listeners) {
            l.seriesColorChanged(name, oldColor, newColor);
        }
        return this;
    }

    public Color getColor() {
        return this.color;
    }

    public Series setVisible(boolean visible) {
        this.visible = visible;
        for (SeriesChangeListener l : listeners) {
            l.seriesVisibilityChanged(name, visible);
        }
        return this;
    }

    public boolean getVisible() {
        return this.visible;
    }

    public Series setBolded(boolean bolded) {
        this.bolded = bolded;
        for (SeriesChangeListener l : listeners) {
            l.seriesBoldnessChanged(name, bolded);
        }
        return this;
    }

    public boolean isBolded() {
        return this.bolded;
    }

    public Series setData(List<Double> newData) {
        minValue = Double.POSITIVE_INFINITY;
        maxValue = Double.NEGATIVE_INFINITY;
        for (double val : newData) {
            minValue = Double.min(minValue, val);
            maxValue = Double.max(maxValue, val);
        }
        this.data = newData;
        for (SeriesChangeListener l : listeners) {
            l.seriesDataChanged(name);
        }
        return this;
    }

    public List<Double> getData() {
        return Collections.unmodifiableList(data);
    }
    
    /**
     * Getter for sample value, given an absolute sample index. This method
     * should be used to get sample values any time you have been given an
     * absolute sample index.
     * 
     * @param absSampleIdx
     * Can be any integer index value, even outside of the series's data range.
     * 
     * @return 
     * The sample's value. Double.NEGATIVE_INFINITY is returned when the
     * absolute index maps to a sample below index 0 in the series's data
     * vector; likewise, Double.POSITIVE_INFINITY is returned if the index goes
     * beyond the end of the data vector.
     */
    public double getAbsSampleValue(int absSampleIdx) {
        int relSampleIdx = getRelSampleIdx(absSampleIdx);
        if (relSampleIdx < 0) {
            return Double.NEGATIVE_INFINITY;
        } else if (relSampleIdx >= this.data.size()) {
            return Double.POSITIVE_INFINITY;
        } else {
            return this.data.get(relSampleIdx);
        }
    }

    public Series setOffset(int newOffset) {
        int oldOffset = this.offset;
        this.offset = newOffset;
        for (SeriesChangeListener l : listeners) {
            l.seriesOffsetChanged(name, oldOffset, newOffset);
        }
        return this;
    }
    
    public int getOffset() {
        return this.offset;
    }
    
    public int getRelSampleIdx(int absSampleIdx) {
        return absSampleIdx - this.offset;
    }

    public void addSeriesListener(SeriesChangeListener l) {
        if (l != null) {
            listeners.add(l);
        }
    }

    public void removeSeriesListener(SeriesChangeListener l) {
        listeners.remove(l);
    }
}
