/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.hankedan.jlogchart.util;

import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;

/**
 *
 * @author daniel
 */
public class VectorUtils {
    public static final Vector2D MAX_VALUE = new Vector2D(Double.MAX_VALUE, Double.MAX_VALUE); 
    public static final Vector2D MIN_VALUE = new Vector2D(-Double.MAX_VALUE, -Double.MAX_VALUE);
    
    public static int compare(Vector2D lhs, Vector2D rhs) {
        int xCompare = Double.compare(lhs.getX(), rhs.getX());
        if (xCompare == 0) {
            return Double.compare(lhs.getY(), rhs.getY());
        } else {
            return xCompare;
        }
    }
    
    public static Vector2D min(Vector2D a, Vector2D b) {
        if (compare(a, b) < 0) {
            return a;
        } else {
            return b;
        }
    }
    
    public static Vector2D max(Vector2D a, Vector2D b) {
        if (compare(a, b) > 0) {
            return a;
        } else {
            return b;
        }
    }
}
