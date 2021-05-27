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
    private double minimum = 0;
    private double maximum = 0;
    private double step = 0;
    
    public double getMaximumDouble() {
        return this.maximum;
    }
    
    public double getMinimumDouble() {
        return this.maximum;
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
    
    public void setMaximumDouble(double d) {
        this.maximum = d;
        computeStep();
    }
    
    public void setMinimumDouble(double d) {
        this.minimum = d;
        computeStep();
    }
    
    @Override
    public void setMaximum(int n) {
        super.setMaximum(n);
        computeStep();
    }

    @Override
    public void setMinimum(int n) {
        super.setMinimum(n);
        computeStep();
    }

    private void computeStep() {
        int spread = getMaximum() - getMinimum();
        if (spread == 0) {
            step = 0;
        } else {
            step = (maximum - minimum) / (getMaximum() - getMinimum());
        }
    }
    
}
