/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.hankedan.jlogchart;

import com.formdev.flatlaf.FlatDarkLaf;
import com.hankedan.jlogchart.util.VectorUtils;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFrame;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;

/**
 *
 * @author daniel
 */
public class XY_Chart extends javax.swing.JPanel implements Series.SeriesChangeListener {
    private final Logger logger = Logger.getLogger(XY_Chart.class.getName());
    
    private final XY_ChartView view = new XY_ChartView();
    
    // Map of all added chart series data vectors
    private final List<XY_Series> allSeries = new ArrayList<>();
    
    private final List<Marker> floatingMarkers = new ArrayList();
    private final Map<String,List<SeriesBoundMarker>> sbmBySeriesName = new HashMap<>();

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
    private Vector2D upperLeftLocation = VectorUtils.MAX_VALUE;
    
    private Vector2D minBounds = null;
    private Vector2D maxBounds = null;
    
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
            series.addSeriesListener(this);
            allSeries.add(series);
            
            logger.log(Level.FINE,
                    "name = {2}; series.minValue = {0}; series.maxValue = {1}; ",
                    new Object[]{series.minValue(),series.maxValue(),name});
            if (allSeries.size() == 1) {
                minBounds = series.minValue();
                maxBounds = series.maxValue();
                upperLeftLocation = minBounds;
            } else {
                minBounds = VectorUtils.min(minBounds, series.minValue());
                maxBounds = VectorUtils.max(maxBounds, series.maxValue());
            }
            logger.log(Level.FINE,
                    "minBounds = {0}; maxBounds = {1}; ",
                    new Object[]{minBounds,maxBounds});
            
            repaint();
        }
        
        return series;
    }
    
    public void removeSeries(String seriesName) {
        Series s = getSeriesByName(seriesName);
        if (s != null) {
            allSeries.remove(s);
            
            // Remove any markers that were bound to this series
            sbmBySeriesName.remove(seriesName);
        }
        
        // TODO handle view bounds resizing
        
        repaint();
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

    public void setSeriesVisible(String name, boolean visible) {
        XY_Series series = getSeriesByName(name);
        if (series != null) {
            // Update and repaint if visibility is changing
            if (series.getVisible() != visible) {
                series.setVisible(visible);
            }
        } else {
            logger.log(Level.WARNING,
                    "Unknown series {0}. Ignoring.",
                    name);
        }
    }

    public boolean hasSeries(String name) {
        return getSeriesByName(name) != null;
    }
    
    public void addMarker(Marker m) {
        floatingMarkers.add(m);
        repaint();
    }
    
    public void removeMarker(Marker m) {
        floatingMarkers.remove(m);
        repaint();
    }
    
    public void addSeriesBoundMarker(SeriesBoundMarker sbm) {
        String seriesName = sbm.getSeries().name;
        if (hasSeries(seriesName)) {
            List<SeriesBoundMarker> sbmList = null;
            if (sbmBySeriesName.containsKey(seriesName)) {
                sbmList = sbmBySeriesName.get(seriesName);
            } else {
                sbmList = new ArrayList();
                sbmBySeriesName.put(seriesName, sbmList);
            }
            sbmList.add(sbm);
            repaint();
        } else {
            logger.log(Level.WARNING,
                    "Attempted to add a SeriesBoundMarker for a series this chart doesn't have. seriesName = {0}",
                    new Object[]{seriesName});
        }
    }

    @Override
    public void seriesSubTitleChanged(String seriesName, String oldSubTitle, String newSubTitle) {
        repaint();
    }

    @Override
    public void seriesVisibilityChanged(String seriesName, boolean visible) {
        repaint();
    }

    @Override
    public void seriesBoldnessChanged(String seriesName, boolean bold) {
        repaint();
    }

    @Override
    public void seriesColorChanged(String seriesName, Color oldColor, Color newColor) {
        repaint();
    }
    
    private class XY_ChartView extends ChartView implements ChartViewListener, MouseWheelListener {
        /**
         * Pixels per series value in the x and y direction. This value changes
         * with zooming.
         */
        private double pxPerValue = Double.NaN;
        
        /**
         * Equal to (currentMouseLocation - dragStartMouseLocation)
         * 
         * Temporary vector used to offset drawing while drag is in progress.
         * Once drag completes, this vector is applied to the chart's
         * 'upperLeftLocation' and then nullified again.
         */
        private Vector2D dragVectorPx = null;
        
        // Set to true when the middle mouse was pressed on the dragStarted
        private boolean panOnDrag = false;
        
        public XY_ChartView() {
            addChartViewListener(this);
            addMouseWheelListener(this);
        }
        
        public void fitViewToData() {
            if (allSeries.isEmpty()) {
                upperLeftLocation = new Vector2D(0, 0);
                pxPerValue = 0;
                return;
            }

            Vector2D diag = maxBounds.subtract(minBounds);
            Vector2D middle = minBounds.add(diag.scalarMultiply(0.5));
            // Compute sqaure bounding box, assume wider than tall
            double halfSqW = diag.getX() / 2.0;
            if (diag.getY() > diag.getX()) {
                // Discover bounding bix is taller than it is wide
                halfSqW = diag.getY() / 2.0;
            }
            Vector2D halfSqDiag = new Vector2D(halfSqW,halfSqW);
            Vector2D minSqBounds = middle.subtract(halfSqDiag);
            Vector2D maxSqBounds = middle.add(halfSqDiag);
        }

        private void drawSeries(Graphics g, Vector2D upperLeftValue, XY_Series series) {
            if (series.getData() == null) {
                return;
            }
            
            Graphics2D g2 = (Graphics2D)g;
            g2.setColor(series.getColor());
            if (series.isBolded()) {
                g2.setStroke(new BasicStroke(Series.BOLD_THICKNESS));
            } else {
                g2.setStroke(new BasicStroke(Series.NORMAL_THICKNESS));
            }
            
            Vector2D prevOffsetPx = null;
            for (Vector2D p : series.getData()) {
                Vector2D offset = p.subtract(upperLeftValue);
                Vector2D offsetPx = offset.scalarMultiply(pxPerValue);
                
                if (prevOffsetPx != null) {
                    g.drawLine(
                            (int)prevOffsetPx.getX(), (int)prevOffsetPx.getY(),
                            (int)offsetPx.getX(), (int)offsetPx.getY());
                }
                
                prevOffsetPx = offsetPx;
            }
        }
        
        private void drawVisibleSeries(Graphics g, Vector2D upperLeftValue) {
            for (XY_Series series : allSeries) {
                if (series.getVisible()) {
                    drawSeries(g, upperLeftValue, series);
                }
            }
        }
        
        private void drawMarker(Graphics g, Vector2D upperLeftValue, Marker m) {
            m.paintMarker(g, upperLeftValue, pxPerValue);
        }
        
        private void drawSeriesBoundMarkers(Graphics g, Vector2D upperLeftValue) {
            for (List<SeriesBoundMarker> sbmList : sbmBySeriesName.values()) {
                for (SeriesBoundMarker sbm : sbmList) {
                    if (sbm.getSeries().getVisible()) {
                        drawMarker(g, upperLeftValue, sbm.getMarker());
                    } else {
                        // don't display if the series is not visible
                        break;
                    }
                }
            }
        }
        
        private void drawFloatingMarkers(Graphics g, Vector2D upperLeftValue) {
            for (Marker m : floatingMarkers) {
                drawMarker(g, upperLeftValue, m);
            }
        }
        
        private Vector2D px2val(Vector2D pxVect) {
            return pxVect.scalarMultiply(1.0 / pxPerValue);
        }
        
        private Vector2D val2px(Vector2D valVect) {
            return valVect.scalarMultiply(pxPerValue);
        }

        /**
         * Zooms the visible portion of the chart and redraws
         * @param zoomIn
         * True if zooming in, false to zoom out
         * @param centerPx
         * The x or y pixel component to center the zoom around
         * @param amount 
         * Amount to zoom by. Valid range is 0.0 to 1.0.
         *   1.0 -> new visible portion is 0% times current visible portion
         *   0.0 -> new visible portion is 100% times current visible portion
         * @param brm
         * The range to perform the zoom on
         */
        private void xyChartZoom(boolean zoomIn, Vector2D centerPx, double amount) {
            if (amount < 0) {
                amount = 0;
            } else if (amount > 1) {
                amount = 1;
            }
            
            /**
             * The below code is tricky, the values really do need to be
             * computed and applied in different order based on if you're
             * zooming in vs zooming out.
             */
            if (zoomIn) {
                pxPerValue *= 1.0 + amount;
                Vector2D zoomOffsetVal = px2val(centerPx).scalarMultiply(amount);
                upperLeftLocation = upperLeftLocation.add(zoomOffsetVal);
            } else {
                Vector2D zoomOffsetVal = px2val(centerPx).scalarMultiply(amount);
                upperLeftLocation = upperLeftLocation.subtract(zoomOffsetVal);
                pxPerValue /= 1.0 + amount;
            }
            
            repaint();
        }
        
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            // See if we should try to recompute pxPerValue
            if (Double.isNaN(pxPerValue)) {
                // attempt to recompute
                if (minBounds != null && maxBounds != null) {
                    Vector2D diag = maxBounds.subtract(minBounds);
                    // TODO make this smarter
                    pxPerValue = (double)getWidth() / diag.getX();
                }
            }
            
            // Still nothing to draw even after attempted recompute
            if (Double.isNaN(pxPerValue)) {
                return;
            }
            
            Vector2D upperLeftValue = upperLeftLocation;
            if (dragVectorPx != null) {
                upperLeftValue = upperLeftLocation.subtract(px2val(dragVectorPx));
            }
            drawVisibleSeries(g, upperLeftValue);
            
            drawSeriesBoundMarkers(g, upperLeftValue);
            drawFloatingMarkers(g, upperLeftValue);
        }

        @Override
        public void onSampleClicked(int absSample, boolean isPanClick) {
        }

        @Override
        public void onLeftClicked(MouseEvent e) {
        }

        @Override
        public void onMiddleClicked(MouseEvent e) {
        }

        @Override
        public void onRightClicked(MouseEvent e) {
        }

        @Override
        public void onDragStarted(int startAbsSample) {
        }

        @Override
        public void onDragStarted(MouseEvent e) {
            if (e.getButton() == MouseEvent.BUTTON2) {
                panOnDrag = true;
            }
        }

        @Override
        public void onDragging(int startAbsSample, int currAbsSample) {
        }

        @Override
        public void onDragging(MouseEvent e1, MouseEvent e2) {
            if (panOnDrag) {
                Vector2D v2 = VectorUtils.toVector(e2);
                Vector2D v1 = VectorUtils.toVector(e1);
                dragVectorPx = v2.subtract(v1);
                
                repaint();
            }
        }

        @Override
        public void onDragComplete(int startAbsSample, int stopAbsSample) {
        }

        @Override
        public void onDragComplete(MouseEvent e1, MouseEvent e2) {
            // Apply pan to chart's location
            if (panOnDrag && dragVectorPx != null) {
                upperLeftLocation = upperLeftLocation.subtract(px2val(dragVectorPx));
            }
            dragVectorPx = null;
            panOnDrag = false;
        }

        @Override
        public void mouseWheelMoved(MouseWheelEvent e) {
            double ZOOM_AMOUNT = 0.2;
            boolean zoomIn = e.getWheelRotation() < 0;
            xyChartZoom(zoomIn, VectorUtils.toVector(e), ZOOM_AMOUNT);
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
                
                int N_SAMPS = 50;
                double radPerSamp = Math.PI * 2 / (N_SAMPS - 1);
                List<Vector2D> circData = new ArrayList();
                List<Vector2D> ovalData = new ArrayList();
                for (int i=0; i<N_SAMPS; i++) {
                    double angle = i * radPerSamp;
                    circData.add(new Vector2D(Math.cos(angle), Math.sin(angle)));
                    ovalData.add(new Vector2D(1.0 + 0.5 * Math.cos(angle), 0.75 * Math.sin(angle)));
                }
                Series circSeries = chart.addSeries("circle", circData);
                Series ovalSeries = chart.addSeries("oval", ovalData).setBolded(true);
                
                SeriesBoundMarker sbm = new SeriesBoundMarker(circSeries, new DotMarker(5));
                chart.addSeriesBoundMarker(sbm);
                
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
