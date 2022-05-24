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
import static java.lang.Integer.max;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author daniel
 */
public class LabelGroup {
    public enum DrawOrigin {
        UPPER_LEFT, UPPER_RIGHT, LOWER_LEFT, LOWER_RIGHT
    }
    
    private List<Label> labels = new ArrayList<>();

    public void addLabel(Label l) {
        labels.add(l);
    }

    public void draw(Graphics g, Color background, DrawOrigin origin, int x, int y, int xPadding, int yPadding) {
        ArrayList<Rectangle2D> relBounds = new ArrayList<>(labels.size());
        FontMetrics fm = g.getFontMetrics();
        int groupHeight = 0;
        int groupWidth = 0;
        for (Label l : labels) {
            Rectangle2D lBox = l.getBoundingBox(fm, 0, groupHeight, xPadding, yPadding);
            groupHeight += lBox.getHeight();
            groupWidth = max(groupWidth, (int)(lBox.getWidth()));
            relBounds.add(lBox);
        }

        int upperLeftX = x;
        int upperLeftY = y;
        switch (origin) {
            case UPPER_LEFT:
                // untouched
                break;
            case UPPER_RIGHT:
                upperLeftX -= groupWidth;
                break;
            case LOWER_LEFT:
                upperLeftY -= groupHeight;
                break;
            case LOWER_RIGHT:
                upperLeftX -= groupWidth;
                upperLeftY -= groupHeight;
                break;
        }

        g.setColor(background);
        g.fillRect(upperLeftX, upperLeftY, groupWidth, groupHeight);

        for (int ll=0; ll<labels.size(); ll++) {
            Label l = labels.get(ll);
            Rectangle2D lBox = relBounds.get(ll);
            l.draw(g, fm, (int)(upperLeftX + lBox.getX()), (int)(upperLeftY + lBox.getY()), xPadding, yPadding);
        }
    }
}
