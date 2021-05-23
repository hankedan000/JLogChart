/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.hankedan.jlogchart;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JScrollBar;
import javax.swing.plaf.basic.BasicScrollBarUI;

/**
 *
 * @author daniel
 */
public class MiniMapScrollbar extends JScrollBar {
    public interface MiniMapable {
        public BufferedImage getMiniMapImage();
    }
    
    private class MiniMapScrollbarUI extends BasicScrollBarUI {
        protected MiniMapable miniMapable = null;
        
        @Override
        public Dimension getPreferredSize(JComponent c) {
            Dimension dim = super.getPreferredSize(c);
            dim.height = 60;
            return dim;
        }

        @Override
        protected void paintThumb(Graphics g, JComponent c, Rectangle bounds) {
            // Draw transparent fill rect
            g.setColor(new Color(255, 255, 255, 25));
            g.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
            // Draw border rect
            g.setColor(Color.LIGHT_GRAY);
            g.drawRect(bounds.x, bounds.y, bounds.width, bounds.height);
        }

        @Override
        protected void paintTrack(Graphics g, JComponent c, Rectangle bounds) {
            if (miniMapable == null) {
                return;
            }
            
            BufferedImage mmImg = miniMapable.getMiniMapImage();
            if (mmImg != null) {
                g.drawImage(mmImg, bounds.x, bounds.y, bounds.width, bounds.height, null);
            }
        }

        @Override
        protected JButton createIncreaseButton(int orientation) {
            return createZeroButton();
        }

        @Override
        protected JButton createDecreaseButton(int orientation) {
            return createZeroButton();
        }
        
        private JButton createZeroButton() {
            JButton jbutton = new JButton();
            jbutton.setPreferredSize(new Dimension(0, 0));
            jbutton.setMinimumSize(new Dimension(0, 0));
            jbutton.setMaximumSize(new Dimension(0, 0));
            jbutton.setEnabled(false);
            return jbutton;
        }
    }
    
    private final MiniMapScrollbarUI minimapUI = new MiniMapScrollbarUI();
    
    public MiniMapScrollbar() {
        setUI(minimapUI);
        // Full transparent background
        setBackground(new Color(0, 0, 0, 20));
    }
    
    public void setMiniMapable(MiniMapable mm) {
        minimapUI.miniMapable = mm;
    }
}
