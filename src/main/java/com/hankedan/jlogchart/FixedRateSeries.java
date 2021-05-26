/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.hankedan.jlogchart;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

/**
 *
 * @author daniel
 */
public class FixedRateSeries extends Series {
    public interface FixedRateSeriesChangeListener {
        public void seriesDataChanged(String seriesName);
        public void seriesOffsetChanged(String seriesName, int oldOffset, int newOffset);
    }
    
    private static final Logger logger = Logger.getLogger(FixedRateSeries.class.getName());
    
    private List<Double> data;
    private double minValue = Double.MAX_VALUE;
    private double maxValue = Double.MIN_VALUE;
    // Allows series samples to be shifted in the x-axis
    private int offset = 0;
    private final List<FixedRateSeriesChangeListener> listeners = new ArrayList();

    public FixedRateSeries(String name, Color color, List<Double> data) {
        this(name,"",color,data);
    }

    public FixedRateSeries(String name, String subTitle, Color color, List<Double> data) {
        super(name,subTitle,color);
        setData(data);
    }

    public double minValue() {
        return minValue;
    }

    public double maxValue() {
        return maxValue;
    }

    public FixedRateSeries setData(List<Double> newData) {
        minValue = Double.POSITIVE_INFINITY;
        maxValue = Double.NEGATIVE_INFINITY;
        for (double val : newData) {
            minValue = Double.min(minValue, val);
            maxValue = Double.max(maxValue, val);
        }
        this.data = newData;
        for (FixedRateSeriesChangeListener l : listeners) {
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

    public FixedRateSeries setOffset(int newOffset) {
        int oldOffset = this.offset;
        this.offset = newOffset;
        for (FixedRateSeriesChangeListener l : listeners) {
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

    public void addFixedRateSeriesListener(FixedRateSeriesChangeListener l) {
        if (l != null) {
            listeners.add(l);
        }
    }

    public void removeFixedRateSeriesListener(FixedRateSeriesChangeListener l) {
        listeners.remove(l);
    }
}
