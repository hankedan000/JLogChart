/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.hankedan.jlogchart.util;

import java.awt.Color;

/**
 *
 * @author daniel
 */
public class ColorPalette {
    public static final Color[] DEFAULT_COLOR_PALETTE = {
        ColorUtils.fromHexString("3a86ff"),// blue
        ColorUtils.fromHexString("50bf1c"),// green
        ColorUtils.fromHexString("ffd503"),// yellow
        ColorUtils.fromHexString("fb5607"),// orange
        ColorUtils.fromHexString("ff006e"),// pink
        ColorUtils.fromHexString("8338ec"),// purple
    };
    
    public static enum WrapBehavior {
        REPEAT, FADE
    }
    
    public static Color getDefault(int idx) {
        return getDefault(idx, WrapBehavior.REPEAT);
    }
    
    public static Color getDefault(int idx, WrapBehavior wb) {
        int colorIdx = idx % DEFAULT_COLOR_PALETTE.length;
        switch (wb) {
            case REPEAT:
                return DEFAULT_COLOR_PALETTE[colorIdx];
            case FADE:
                return DEFAULT_COLOR_PALETTE[colorIdx];
            default:
                throw new UnsupportedOperationException(
                        String.format("%s WrapBehvior not supported", wb));
        }
    }
}
