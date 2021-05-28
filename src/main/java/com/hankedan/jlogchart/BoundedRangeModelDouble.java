/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.hankedan.jlogchart;

import javax.swing.DefaultBoundedRangeModel;

/**
 *
 * @author daniel
 */
public class BoundedRangeModelDouble extends DefaultBoundedRangeModel {
    private final double step;
    
    public BoundedRangeModelDouble() {
        this(1.0);
    }
    
    public BoundedRangeModelDouble(double step) {
        this(step,0,0,0,0);
    }
    
    public BoundedRangeModelDouble(double step, double value, double extent, double min, double max) {
        super();
        this.step = step;
        setMinimumDouble(min);
        setMaximumDouble(max);
        setValueDouble(value);
        setExtentDouble(extent);
    }
    
    public double getMaximumDouble() {
        return getMaximum() * step;
    }
    
    public double getMinimumDouble() {
        return getMinimum() * step;
    }

    public double getValueDouble() {
        return getValue() * step;
    }

    public double getExtentDouble() {
        return getExtent() * step;
    }
    
    public double getStep() {
        return this.step;
    }
    
    public void setValueDouble(double d) {
        setValue((int)Math.round(d / step));
    }
    
    public void setExtentDouble(double d) {
        setExtent((int)Math.round(d / step));
    }
    
    public void setMaximumDouble(double d) {
        setMaximum((int)Math.round(d / step));
    }
    
    public void setMinimumDouble(double d) {
        setMinimum((int)Math.round(d / step));
    }
    
}
