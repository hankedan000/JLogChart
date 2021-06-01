/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.hankedan.jlogchart;

import java.awt.Graphics;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;

/**
 *
 * @author daniel
 */
public abstract class Marker {
    protected Vector2D pos;
    
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
    
    public double getX() {
        return this.pos.getX();
    }
    
    public double getY() {
        return this.pos.getY();
    }
    
    abstract
    protected void paintMarker(Graphics g);
    
    abstract
    protected void paintMarker(Graphics g, Vector2D viewOrigin, double pxScale);
}
