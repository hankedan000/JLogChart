/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.hankedan.jlogchart;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.geom.Rectangle2D;

/**
 *
 * @author daniel
 */
public class Label {
    public final String text;
    public final Color textColor;

    public Label(String text, Color textColor) {
        this.text = text;
        this.textColor = textColor;
    }

    /**
     *  BOUNDING BOX
     *  +------------------------------------------------------+
     *  |                                                      |
     *  |   +----------------------------------------------+   |
     *  |   |                                              |   |
     *  |   |                                              |   |
     *  |   |    vvv TEXT BASELINE vvv                     |   |
     *  |   | -------------------------------------        |   |
     *  |   +----------------------------------------------+   |
     *  | PADDING                                              |
     *  +------------------------------------------------------+
     * 
     * @param g
     * 
     * @param x
     * absolute x offset of the upper left corner of the bounding box
     * 
     * @param y
     * absolute y offset of the upper left corner of the bounding box
     * 
     * @param xPadding
     * optional padding in pixels around the text on the left and right.
     * value just accounts for one side, so total padding from left to right
     * is xPadding * 2.
     * 
     * @param yPadding
     * optional padding in pixels around the text on the top and bottom
     * value just accounts for one side, so total padding from top to bottom
     * is yPadding * 2.
     * @return 
     */
    public void draw(Graphics g, FontMetrics fm, Color background, int x, int y, int xPadding, int yPadding) {
        g.setColor(background);
        int textHeight = fm.getHeight();
        int bgWidth = xPadding * 2 + fm.stringWidth(text);
        int bgHeight = yPadding * 2 + textHeight;
        g.fillRect(x, y, bgWidth, bgHeight);
        drawText(g, fm, x, y, xPadding, yPadding);
    }

    /**
     * See documentation on draw() method for info on x, y, and padding
     */
    public void draw(Graphics g, FontMetrics fm, int x, int y, int xPadding, int yPadding) {
        drawText(g, fm, x, y, xPadding, yPadding);
    }

    /**
     * See documentation on draw() method for info on x, y, and padding
     */
    private void drawText(Graphics g, FontMetrics fm, int x, int y, int xPadding, int yPadding) {
        g.setColor(textColor);
        g.drawString(text, x + xPadding, y + yPadding + fm.getMaxAscent());
    }

    public Rectangle2D getBoundingBox(FontMetrics fm) {
        return getBoundingBox(fm, 0, 0, 0, 0);
    }

    public Rectangle2D getBoundingBox(FontMetrics fm, int xPadding, int yPadding) {
        return getBoundingBox(fm, 0, 0, xPadding, yPadding);
    }

    /**
     *  BOUNDING BOX
     *  +------------------------------------------------------+
     *  |                                                      |
     *  |   +----------------------------------------------+   |
     *  |   |                                              |   |
     *  |   |                                              |   |
     *  |   |    vvv TEXT BASELINE vvv                     |   |
     *  |   | -------------------------------------        |   |
     *  |   +----------------------------------------------+   |
     *  | PADDING                                              |
     *  +------------------------------------------------------+
     * 
     * @param fm FontMetrics from the Graphics class that the label will be
     * draw in.
     * 
     * @param x
     * absolute x offset of the upper left corner of the bounding box
     * 
     * @param y
     * absolute y offset of the upper left corner of the bounding box
     * 
     * @param xPadding
     * optional padding in pixels around the text on the left and right.
     * value just accounts for one side, so total padding from left to right
     * is xPadding * 2.
     * 
     * @param yPadding
     * optional padding in pixels around the text on the top and bottom
     * value just accounts for one side, so total padding from top to bottom
     * is yPadding * 2.
     * 
     * @return 
     */
    public Rectangle2D getBoundingBox(FontMetrics fm, int x, int y, int xPadding, int yPadding) {
        int textHeight = fm.getHeight();
        int bgWidth = xPadding * 2 + fm.stringWidth(text);
        int bgHeight = yPadding * 2 + textHeight;
        return new Rectangle2D.Double(x , y, bgWidth, bgHeight);
    }
}
