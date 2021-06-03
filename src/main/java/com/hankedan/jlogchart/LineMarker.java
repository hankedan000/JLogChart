/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.hankedan.jlogchart;

import java.awt.Color;
import java.awt.Graphics;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;

/**
 *
 * @author daniel
 */
public class LineMarker extends Marker {
    private ChartView view;
    private Color color;
    private boolean horzOrientation = false;
    
    public LineMarker() {
        super(Vector2D.ZERO);
    }
    
    public LineMarker(double x, double y) {
        super(x,y);
    }
    
    public LineMarker(Vector2D p) {
        super(p);
    }
    
    public LineMarker setFill(Color c) {
        this.color = c;
        return this;
    }
    
    public Color getFill() {
        return this.color;
    }
    
    public LineMarker setOrientation(boolean horizontal) {
        this.horzOrientation = horizontal;
        return this;
    }
    
    public boolean isHorizontal() {
        return this.horzOrientation;
    }
    
    public boolean isVertical() {
        return ! this.horzOrientation;
    }
    
    /**
     * Called by the parent ChartView when this marker is added to it. The
     * ChartView is used to get the total draw height/weight of the line marker
     * @param cv
     * @return 
     */
    protected final LineMarker setView(ChartView cv) {
        this.view = cv;
        return this;
    }

    @Override
    protected void paintMarker(Graphics g, Vector2D viewOrigin, Vector2D pxScale) {
        if (this.view == null) {
            return;
        } else if ( ! view.isVisible()) {
            return;
        } else if ( ! isVisible()) {
            return;
        }
        
        g.setColor(color);
        if (isHorizontal()) {
            int width = view.getWidth();
            int yPos = (int)((pos.getY() - viewOrigin.getY()) * pxScale.getY());
            g.drawLine(0, yPos, width, yPos);
        } else {
            int height = view.getHeight();
            int xPos = (int)((pos.getX() - viewOrigin.getX()) * pxScale.getX());
            g.drawLine(xPos, 0, xPos, height);
        }
    }
}
