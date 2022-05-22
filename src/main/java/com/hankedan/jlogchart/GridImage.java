/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.hankedan.jlogchart;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

/**
 *
 * @author daniel
 */
public class GridImage extends BufferedImage {
    private boolean hRuleVisible = true;
    private boolean vRuleVisible = true;
    private double hScaleMin = 0;
    private double hScaleMax = 0;
    private double vScaleMin = 0;
    private double vScaleMax = 0;
    private double hMajorRuleStride = 0;
    private double vMajorRuleStride = 0;
    
    private BasicStroke originStroke = new BasicStroke(2);
    private BasicStroke majorStroke = new BasicStroke(1);

    /**
     * Constructs a new GridImage with the provided dimensions
     * @param width
     * width of the image in pixels
     * @param height 
     * height of the image in pixels
     */
    public GridImage(int width, int height) {
        super(width,height,BufferedImage.TYPE_4BYTE_ABGR);
    }
    
    /**
     * Clears the image for a fresh draw() call
     * @return 
     * a reference to the GridImage
     */
    public GridImage clear() {
        Graphics2D g2 = (Graphics2D)getGraphics();
        g2.setBackground(new Color(0,0,0,0));
        g2.clearRect(0, 0, getWidth(), getHeight());
        return this;
    }
    
    /**
     * Set visibility of the horizontal rules
     * @param visible
     * true if the horizontal rules should be drawn; false if not.
     * @return 
     * a reference to the GridImage
     */
    public GridImage setHorzRuleVisible(boolean visible) {
        hRuleVisible = visible;
        return this;
    }
    
    public boolean getHorzRuleVisible() {
        return hRuleVisible;
    }
    
    /**
     * Set visibility of the vertical rules
     * @param visible
     * true if the vertical rules should be drawn; false if not.
     * @return 
     * a reference to the GridImage
     */
    public GridImage setVertRuleVisible(boolean visible) {
        vRuleVisible = visible;
        return this;
    }
    
    public boolean getVertRuleVisible() {
        return vRuleVisible;
    }
    
    /**
     * Set visibility of the horizontal/vertical rules
     * @param hVisible
     * true if the horizontal rules should be drawn; false if not.
     * @param vVisible
     * true if the vertical rules should be drawn; false if not.
     * @return 
     * a reference to the GridImage
     */
    public GridImage setRuleVisibilty(boolean hVisible, boolean vVisible) {
        setHorzRuleVisible(hVisible);
        setVertRuleVisible(vVisible);
        return this;
    }
    
    /**
     * Set the full scale of the x-axis
     * @param min
     * the lowest value on the x-axis (left side of chart)
     * @param max
     * the highest value on the x-axis (right side of chart)
     * @return
     * a reference to the GridImage
     */
    public GridImage setHorzScale(double min, double max) {
        hScaleMin = min;
        hScaleMax = max;
        return this;
    }
    
    /**
     * Set the full scale of the y-axis
     * @param min
     * the lowest value on the y-axis (bottom of chart)
     * @param max
     * the highest value on the y-axis (top of chart)
     * @return 
     * a reference to the GridImage
     */
    public GridImage setVertScale(double min, double max) {
        vScaleMin = min;
        vScaleMax = max;
        return this;
    }
    
    /**
     * Set the distance between major-rule lines
     * @param hStride
     * the vertical distance (in value, not pixels) between horizontal rules
     * @param vStride
     * the horizontal distance (in value, not pixels) between vertical rules
     * @return 
     * a reference to the GridImage
     */
    public GridImage setMajorRuleStride(double hStride, double vStride) {
        hMajorRuleStride = hStride;
        vMajorRuleStride = vStride;
        return this;
    }
    
    /**
     * Issues a full draw of the GridImage based on the most resent parameters
     * @return 
     * a reference to the GridImage
     */
    public GridImage draw() {
        if (hRuleVisible) {
            drawH_Rules();
        }
        if (vRuleVisible) {
            drawV_Rules();
        }
        
        return this;
    }
    
    private void drawH_Rules() {
        double vFullScale = Math.abs(vScaleMax - vScaleMin);
        double hFullScale = Math.abs(hScaleMax - hScaleMin);
        if (vFullScale == 0) {
            return;
        } else if (hFullScale == 0) {
            return;
        } else if (hMajorRuleStride == 0) {
            return;
        }
        
        Graphics2D g2 = (Graphics2D)getGraphics();
        double yPxPerValue = getHeight() / vFullScale;
        double xPxPerValue = getWidth() / hFullScale;
        double valuePerPx = 1.0 / yPxPerValue;
        
        double yVal = Math.floor(vScaleMax / hMajorRuleStride) * hMajorRuleStride;
        int yPx= (int)((vScaleMax - yVal) * yPxPerValue);
        // draw labels on origin line, or at border if origin is off screen
        int labelX = (int)((0.0 - hScaleMin) * xPxPerValue);
        labelX = Math.max(labelX, 0);// clamp to left of screen
        labelX = Math.min(labelX, getWidth());// or clamp to right of screen
        
        // draw horizontal rules
        g2.setColor(Color.BLACK);
        while (yPx < getHeight()) {
            if (Math.abs(yVal) < valuePerPx) {
                g2.setStroke(originStroke);
            } else {
                g2.setStroke(majorStroke);
                g2.drawString(String.format("%.2f",yVal), labelX + 2, yPx - 2);
            }
            g2.drawLine(0, yPx, getWidth(), yPx);
            yVal -= hMajorRuleStride; yPx= (int)((vScaleMax - yVal) * yPxPerValue);
        }
    }
    
    private void drawV_Rules() {
        double vFullScale = Math.abs(vScaleMax - vScaleMin);
        double hFullScale = Math.abs(hScaleMax - hScaleMin);
        if (vFullScale == 0) {
            return;
        } else if (hFullScale == 0) {
            return;
        } else if (vMajorRuleStride == 0) {
            return;
        }
        
        Graphics2D g2 = (Graphics2D)getGraphics();
        double yPxPerValue = getHeight() / vFullScale;
        double xPxPerValue = getWidth() / hFullScale;
        double valuePerPx = 1.0 / xPxPerValue;
        
        double xVal = Math.floor(hScaleMin / vMajorRuleStride) * vMajorRuleStride;
        int xPx= (int)((xVal - hScaleMin) * xPxPerValue);
        // draw labels on origin line, or at border if origin is off screen
        int labelY = (int)((vScaleMax - 0.0) * yPxPerValue);
        labelY = Math.max(labelY, 0);// clamp to top of screen
        labelY = Math.min(labelY, getHeight());// or clamp to bottom of screen
        
        // draw vertical rules
        g2.setColor(Color.BLACK);
        while (xPx < getWidth()) {
            if (Math.abs(xVal) < valuePerPx) {
                g2.setStroke(originStroke);
            } else {
                g2.setStroke(majorStroke);
                g2.drawString(String.format("%.2f",xVal), xPx + 2, labelY - 2);
            }
            g2.drawLine(xPx, 0, xPx, getHeight());
            xVal += vMajorRuleStride; xPx= (int)((xVal - hScaleMin) * xPxPerValue);
        }
    }
}
