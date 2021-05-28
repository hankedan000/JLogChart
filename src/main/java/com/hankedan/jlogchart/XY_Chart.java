/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.hankedan.jlogchart;

import com.formdev.flatlaf.FlatDarkLaf;
import com.hankedan.jlogchart.util.VectorUtils;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BoundedRangeModel;
import javax.swing.JFrame;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;

/**
 *
 * @author daniel
 */
public class XY_Chart extends javax.swing.JPanel {
    private final Logger logger = Logger.getLogger(XY_Chart.class.getName());
    
    private final XY_ChartView view = new XY_ChartView();
    
    // Map of all added chart series data vectors
    private final List<XY_Series> allSeries = new ArrayList<>();

    //                      MarginalBoundingBox
    //    +-----------------------------------------------------+
    //    |                        MARGIN                       |
    //    |                     BoundingBox                     |
    //    |     +-----------------------------------------+     |
    //    |     |\__minBounds                             |     |
    //    |     |                                         |     |
    //    |     |                                         |     |
    //    |     |            +----------------------+     |     |
    //    |     |            |                      |     |     |
    //    |     |            |                      |     |     |
    //    |     |            |    VISIBLE           |     |     |
    //    |     |            |     AREA             |     |     |
    //    |     |            |                      |     |     |
    //    |     |            |                      |     |     |
    //    |     |            +----------------------+     |     |
    //    |     |                                         |     |
    //    |     |                                         |     |
    //    |     |                                         |     |
    //    |     |       Series Data Is Drawn With Here    |     |
    //    |     |                                         |     |
    //    |     |                                         |     |
    //    |     |                             maxBounds__ |     |
    //    |     |                                        \|     |
    //    |     +-----------------------------------------+     |
    //    |                                                     |
    //    |                        MARGIN                       |
    //    +-----------------------------------------------------+
    
    private final double MARGIN_PERCENT = 0.1;
    private Vector2D minBounds = VectorUtils.MAX_VALUE;
    private Vector2D maxBounds = VectorUtils.MIN_VALUE;
    
    /**
     * Creates new form XY_Chart
     */
    public XY_Chart() {
        initComponents();
        
        add(view, BorderLayout.CENTER);
    }
    
    public ChartView getChartView() {
        return view;
    }
    
    public XY_Series addSeries(String name, List<Vector2D> data) {
        XY_Series series = getSeriesByName(name);
        
        if (series != null) {
            logger.log(Level.WARNING,
                    "Duplicate add of series data with name {0}. Ignoring.",
                    new Object[]{name});
        } else {
            Color color = Series.getDefaultColor(allSeries.size());
            series = new XY_Series(name, color, data);
            allSeries.add(series);
            Vector2D tempMinBounds = VectorUtils.min(minBounds, series.minValue());
            Vector2D tempMaxBounds = VectorUtils.min(maxBounds, series.maxValue());
            Vector2D diag = tempMaxBounds.subtract(tempMinBounds);
            // Compute middle point of the new bounding box
            Vector2D newMiddle = tempMinBounds.add(diag.scalarMultiply(0.5));
            
            // Compute square bounds around newMiddle point
            // Assume bounding box is taller than it is wide
            double halfSqW = diag.getY() / 2.0;
            if (diag.getX() > diag.getY()) {
                // Realize bounding box is wider than it is tall
                halfSqW = diag.getX() / 2.0;
            }
            Vector2D halfSqDiag = new Vector2D(halfSqW,halfSqW);
            Vector2D newMinSqBounds = newMiddle.subtract(halfSqDiag);
            Vector2D newMaxSqBounds = newMiddle.add(halfSqDiag);
            int newX_Min = Integer.min(xRange.getMinimum(), 0);
            int newX_Max = Integer.max(xRange.getMaximum(), data.size());
            int newLeftViewedSamp = 0;
            int newRightViewedSamp = newX_Max - 1;
            if (allSeries.size() > 1) {
                newLeftViewedSamp = Integer.max(view.leftViewedSamp(), newLeftViewedSamp);
                newRightViewedSamp = Integer.min(view.rightViewedSamp(), newRightViewedSamp);
            }
            view.setX_RangeMin(newX_Min);
            view.setX_RangeMax(newX_Max);
            view.setHorzViewBounds(newLeftViewedSamp, newRightViewedSamp);
            series.addSeriesListener(this);
            series.addFixedRateSeriesListener(this);
            logger.log(Level.FINE,
                    "leftViewedSamp = {0}; rightViewedSamp = {1};",
                    new Object[]{view.leftViewedSamp(),view.rightViewedSamp()});
            repaint();
        }
        
        return series;
    }
    
    /**
     * @return
     * An immutable list of all the FixedRateSeries that are added to the chart
     */
    public List<XY_Series> getAllSeries() {
        return Collections.unmodifiableList(allSeries);
    }
    
    public XY_Series getSeriesByName(String name) {
        for (XY_Series series : allSeries) {
            if (series.name.compareTo(name) == 0) {
                return series;
            }
        }
        return null;
    }
    
    private class XY_ChartView extends ChartView {

        private void drawSeries(Graphics g, XY_Series series,
                BoundedRangeModelDouble xr, BoundedRangeModelDouble yr) {
            if (series.getData() == null) {
                return;
            }
        }
        
        private void drawVisibleSeries(Graphics g, 
                BoundedRangeModelDouble xr, BoundedRangeModelDouble yr) {
            for (XY_Series series : allSeries) {
                if (series.getVisible()) {
                    drawSeries(g, series, xr, yr);
                }
            }
        }
        
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            
            drawVisibleSeries(g, xRange, yRange);
        }
        
    }
    
    public static void main(String[] args) {
        FlatDarkLaf.install();
        
        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                JFrame frame = new JFrame("XY_Chart Standalone");
                frame.setLayout(new BorderLayout());
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                
                // Create and show the JLogChart GUI
                XY_Chart chart = new XY_Chart();
                chart.setPreferredSize(new Dimension(600, 400));
                frame.add(chart, BorderLayout.CENTER);
                
                int N_SAMPS = 20;
                double radPerSamp = Math.PI * 2 / N_SAMPS;
                List<Vector2D> circData = new ArrayList();
                for (int i=0; i<N_SAMPS; i++) {
                    double angle = i * radPerSamp;
                    circData.add(new Vector2D(Math.cos(angle), Math.sin(angle)));
                }
                chart.addSeries("circle", circData);
                chart.getChartView().fitViewWidthToData();
                
                // Show the GUI
                frame.pack();
                frame.setVisible(true);
            }
        });
    }
    
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        setLayout(new java.awt.BorderLayout());
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    // End of variables declaration//GEN-END:variables
}
