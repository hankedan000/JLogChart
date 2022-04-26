/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.hankedan.jlogchart.util;

import java.awt.Color;
import java.security.InvalidParameterException;

/**
 *
 * @author daniel
 */
public class ColorUtils {
    // converts a color to a string of format "#rrggbb"
    public static String toHexString(Color c) {
        return "#"+Integer.toHexString(c.getRGB()).substring(2);
    }
    
    // convert string of format "#rrggbb" to a color
    public static Color fromHexString(String s) {
        String s2 = s;
        if (s.length() > 0 && s.charAt(0) == '#') {
            s2 = s.substring(1);
        }
        if(s2.length() != 6) {
            throw new InvalidParameterException(
                    "s.length() not long enough for RGB components");
        }
        
        int r = Integer.parseInt(s2, 0, 2, 16);
        int g = Integer.parseInt(s2, 2, 4, 16);
        int b = Integer.parseInt(s2, 4, 6, 16);
        return new Color(r,g,b);
    }
}
