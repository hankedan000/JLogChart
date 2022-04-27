/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.hankedan.jlogchart;

import java.awt.Color;
import java.awt.Graphics;
import java.util.logging.Logger;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;

/**
 *
 * @author daniel
 */
public class DotMarker extends Marker {
    private double radius;
    private Color fill;
    private Color stroke;
    
    public DotMarker(double radius) {
        this(0,0,radius);
    }
    
    public DotMarker(double x, double y, double radius) {
        this(new Vector2D(x,y),radius);
    }
    
    public DotMarker(Vector2D p, double radius) {
        super(p);
        this.radius = radius;
        this.fill = Color.CYAN;
        this.stroke = Color.BLACK;
    }
    
    public Marker setRadius(double radius) {
        this.radius = radius;
        return this;
    }
    
    public double getRadius(double radius) {
        return this.radius;
    }
    
    public Marker setFill(Color c) {
        this.fill = c;
        return this;
    }
    
    public Color getFill() {
        return this.fill;
    }
    
    public Marker setStroke(Color c) {
        this.stroke = c;
        return this;
    }
    
    public Color getStroke() {
        return this.stroke;
    }

    @Override
    protected void paintMarker(Graphics g, Vector2D viewOrigin, Vector2D pxScale) {
        if ( ! isVisible()) {
            return;
        }
        
        Vector2D offsetPos = pos.subtract(viewOrigin);
        g.setColor(fill);
        g.fillOval(
                (int)(offsetPos.getX() * pxScale.getX() - radius),(int)(offsetPos.getY() * pxScale.getY() - radius),
                (int)(radius * 2), (int)(radius * 2));
        g.setColor(stroke);
        g.drawOval(
                (int)(offsetPos.getX() * pxScale.getX() - radius),(int)(offsetPos.getY() * pxScale.getY() - radius),
                (int)(radius * 2), (int)(radius * 2));
        
        if (isHoverable()) {
            paintHoverText(g, viewOrigin, pxScale, new Vector2D(radius,-radius));
        }
    }
    
}
