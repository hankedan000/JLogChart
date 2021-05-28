/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.hankedan.jlogchart.util;

import java.awt.event.MouseEvent;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;

/**
 *
 * @author daniel
 */
public class VectorUtils {
    public static final Vector2D MAX_VALUE = new Vector2D(Double.MAX_VALUE, Double.MAX_VALUE); 
    public static final Vector2D MIN_VALUE = new Vector2D(-Double.MAX_VALUE, -Double.MAX_VALUE);
    
    public static Vector2D toVector(MouseEvent e) {
        return new Vector2D(e.getX(),e.getY());
    }
    
    public static Vector2D min(Vector2D a, Vector2D b) {
        return new Vector2D(
                Double.min(a.getX(), b.getX()),
                Double.min(a.getY(), b.getY()));
    }
    
    public static Vector2D max(Vector2D a, Vector2D b) {
        return new Vector2D(
                Double.max(a.getX(), b.getX()),
                Double.max(a.getY(), b.getY()));
    }
}
