/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.hankedan.jlogchart;

import java.awt.Color;
import java.util.ArrayList;
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
        return data;
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
