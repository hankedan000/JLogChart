/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.hankedan.jlogchart;

import java.awt.Color;
import java.awt.Graphics;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;

/**
 *
 * @author daniel
 */
public class DotMarker extends Marker {
    private Logger logger = Logger.getLogger(DotMarker.class.getName());
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
    
    public Color getStrokel() {
        return this.stroke;
    }

    @Override
    protected void paintMarker(Graphics g) {
        paintMarker(g, Vector2D.ZERO, 1.0);
    }

    @Override
    protected void paintMarker(Graphics g, Vector2D viewOrigin, double pxScale) {
        Vector2D offsetPos = pos.subtract(viewOrigin);
        g.setColor(fill);
        g.fillOval(
                (int)(offsetPos.getX() * pxScale - radius),(int)(offsetPos.getY() * pxScale - radius),
                (int)(radius * 2), (int)(radius * 2));
        g.setColor(stroke);
        g.drawOval(
                (int)(offsetPos.getX() * pxScale - radius),(int)(offsetPos.getY() * pxScale - radius),
                (int)(radius * 2), (int)(radius * 2));
        
        if (isHoverable()) {
            paintHoverText(g, viewOrigin, pxScale, new Vector2D(radius,-radius));
        }
    }
    
}
