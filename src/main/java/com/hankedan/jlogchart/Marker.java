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
public abstract class Marker {
    protected Vector2D pos;
    
    private boolean hoverable = false;
    private String hoverText = "";
    
    private boolean visible = true;
    
    public Marker(double x, double y) {
        this(new Vector2D(x,y));
    }
    
    public Marker(Vector2D p) {
        this.pos = p;
    }
    
    public Marker setPosition(double x, double y) {
        return setPosition(new Vector2D(x,y));
    }
    
    public Marker setPosition(Vector2D newPos) {
        this.pos = newPos;
        return this;
    }
    
    public Vector2D getPosition() {
        return this.pos;
    }
    
    public Marker setHoverable(boolean hoverable) {
        this.hoverable = hoverable;
        return this;
    }
    
    public boolean isHoverable() {
        return this.hoverable;
    }
    
    public Marker setHoverText(String text) {
        this.hoverText = text;
        return this;
    }
    
    public String getHoverText() {
        return this.hoverText;
    }
    
    public Marker setVisible(boolean visible) {
        this.visible = visible;
        return this;
    }
    
    public boolean isVisible() {
        return this.visible;
    }
    
    public double getX() {
        return this.pos.getX();
    }
    
    public double getY() {
        return this.pos.getY();
    }
    
    protected void paintHoverText(Graphics g, Vector2D viewOrigin, Vector2D pxScale, Vector2D textOffset) {
        if ( ! visible) {
            return;
        }
        
        Vector2D paintPos = pos.subtract(viewOrigin).add(textOffset);
        paintPos = new Vector2D(paintPos.getX() * pxScale.getX(), paintPos.getY() * pxScale.getY());
        g.setColor(Color.BLACK);
        g.drawString(hoverText, (int)paintPos.getX(), (int)paintPos.getY());
    }
    
    protected void paintMarker(Graphics g) {
        paintMarker(g, Vector2D.ZERO, new Vector2D(1,1));
    }
    
    abstract 
    protected void paintMarker(Graphics g, Vector2D viewOrigin, Vector2D pxScale);
}
