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
    private int originX = 0;
    private int originY = 0;
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
    
    public GridImage setOriginLocation(int x, int y) {
        originX = x;
        originY = y;
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
        double valueDelta = Math.abs(vScaleMax - vScaleMin);
        if (valueDelta == 0) {
            return;
        }
        
        Graphics2D g2 = (Graphics2D)getGraphics();
        double pxPerValue = getHeight() / valueDelta;
        
        // draw origin line
        double originValueY = vScaleMax - originY / pxPerValue;
        double yVal = originValueY;
        int yPx= (int)((vScaleMax - yVal) * pxPerValue);
        g2.setColor(Color.BLACK);
        g2.setStroke(originStroke);
        g2.drawLine(0, yPx, getWidth(), yPx);
        
        // draw major lines
        g2.setStroke(majorStroke);
        yVal += hMajorRuleStride; yPx= (int)((vScaleMax - yVal) * pxPerValue);// advance past origin
        while (yPx >= 0) {
            g2.drawLine(0, yPx, getWidth(), yPx);
            g2.drawString(String.format("%.2f",yVal), originX + 2, yPx - 2);
            yVal += hMajorRuleStride; yPx= (int)((vScaleMax - yVal) * pxPerValue);
        }
        
        yVal = originValueY;
        yVal -= hMajorRuleStride; yPx= (int)((vScaleMax - yVal) * pxPerValue);// advance past origin
        while (yPx < getHeight()) {
            g2.drawLine(0, yPx, getWidth(), yPx);
            g2.drawString(String.format("%.2f",yVal), originX + 2, yPx - 2);
            yVal -= hMajorRuleStride; yPx= (int)((vScaleMax - yVal) * pxPerValue);
        }
    }
    
    private void drawV_Rules() {
        double valueDelta = Math.abs(hScaleMax - hScaleMin);
        if (valueDelta == 0) {
            return;
        }
        
        Graphics2D g2 = (Graphics2D)getGraphics();
        double pxPerValue = getWidth()/ valueDelta;
        
        // draw origin line
        double originValueX = hScaleMin + originX / pxPerValue;
        double xVal = originValueX;
        int xPx= (int)((xVal - hScaleMin) * pxPerValue);
        g2.setColor(Color.BLACK);
        g2.setStroke(originStroke);
        g2.drawLine(xPx, 0, xPx, getHeight());
        
        // draw major lines
        g2.setStroke(majorStroke);
        xVal += vMajorRuleStride; xPx= (int)((xVal - hScaleMin) * pxPerValue);// advance past origin
        while (xPx < getWidth()) {
            g2.drawLine(xPx, 0, xPx, getHeight());
            g2.drawString(String.format("%.2f",xVal), xPx + 2, originY - 2);
            xVal += vMajorRuleStride; xPx= (int)((xVal - hScaleMin) * pxPerValue);
        }
        
        xVal = originValueX;
        xVal -= vMajorRuleStride; xPx= (int)((xVal - hScaleMin) * pxPerValue);// advance past origin
        while (xPx > 0) {
            g2.drawLine(xPx, 0, xPx, getHeight());
            g2.drawString(String.format("%.2f",xVal), xPx + 2, originY - 2);
            xVal -= vMajorRuleStride; xPx= (int)((xVal - hScaleMin) * pxPerValue);
        }
    }
}
