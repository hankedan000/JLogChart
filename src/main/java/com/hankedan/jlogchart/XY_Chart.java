/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.hankedan.jlogchart;

import com.hankedan.jlogchart.LabelGroup.DrawOrigin;
import com.hankedan.jlogchart.util.ColorPalette;
import com.hankedan.jlogchart.util.ColorPalette.WrapBehavior;
import com.hankedan.jlogchart.util.VectorUtils;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFileChooser;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;

/**
 *
 * @author daniel
 */
public class XY_Chart extends javax.swing.JPanel implements Series.SeriesChangeListener {
    private final Logger logger = Logger.getLogger(XY_Chart.class.getName());
    
    private final XY_ChartView view = new XY_ChartView();
    
    // Map of all added chart series data vectors
    private final List<Series<Vector2D>> allSeries = new ArrayList<>();
    
    private final MarkerManager markerMgr = new MarkerManager();

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
    
    // allows us to flip the axis so that +y is up
    private Vector2D scale = new Vector2D(1, -1);
    
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
    
    /**
     * Returns the XY location of the visible pixel location
     * @param xyPixel
     * @return 
     */
    public Vector2D getXY_Value(Vector2D xyPixel) {
        return view.px2val_global(xyPixel);
    }
    
    public void clear() {
        allSeries.clear();
        markerMgr.clear();
        upperLeftLocation = VectorUtils.MAX_VALUE;
        minBounds = null;
        maxBounds = null;
        repaint();
    }
    
    public Series<Vector2D> addSeries(String name, List<Vector2D> data) {
        Series series = getSeriesByName(name);
        
        if (series != null) {
            logger.log(Level.WARNING,
                    "Duplicate add of series data with name {0}. Ignoring.",
                    new Object[]{name});
        } else {
            Color color = ColorPalette.getDefault(allSeries.size(), WrapBehavior.REPEAT);
            series = new Series<Vector2D>(name, color, data);
            series.addSeriesListener(this);
            allSeries.add(series);
            
            logger.log(Level.FINE,
                    "name = {2}; series.minValue = {0}; series.maxValue = {1}; ",
                    new Object[]{series.minValue(),series.maxValue(),name});
            if (allSeries.size() == 1) {
                minBounds = (Vector2D)series.minValue();
                maxBounds = (Vector2D)series.maxValue();
            } else {
                minBounds = VectorUtils.min(minBounds, (Vector2D)series.minValue());
                maxBounds = VectorUtils.max(maxBounds, (Vector2D)series.maxValue());
            }
            
            // FIXME this isn't taking into acount the 'scale' member
            upperLeftLocation = new Vector2D(minBounds.getX(), maxBounds.getY());
            logger.log(Level.FINE,
                    "minBounds = {0}; maxBounds = {1}; upperLeftLocation = {2}; ",
                    new Object[]{minBounds,maxBounds,upperLeftLocation});
            
            repaint();
        }
        
        return series;
    }
    
    public void removeSeries(String seriesName) {
        Series s = getSeriesByName(seriesName);
        if (s != null) {
            allSeries.remove(s);
            
            // Remove any markers that were bound to this series
            markerMgr.removeSeriesMarkers(seriesName);
        }
        
        // TODO handle view bounds resizing
        
        repaint();
    }
    
    /**
     * @return
     * An immutable list of all the FixedRateSeries that are added to the chart
     */
    public List<Series<Vector2D>> getAllSeries() {
        return Collections.unmodifiableList(allSeries);
    }
    
    public Series<Vector2D> getSeriesByName(String name) {
        for (Series series : allSeries) {
            if (series.name.compareTo(name) == 0) {
                return series;
            }
        }
        return null;
    }

    public void setSeriesVisible(String name, boolean visible) {
        Series series = getSeriesByName(name);
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
        markerMgr.addMarker(m);
        repaint();
    }
    
    public void removeMarker(Marker m) {
        markerMgr.removeMarker(m);
        repaint();
    }
    
    public void addSeriesBoundMarker(SeriesBoundMarker sbm) {
        String seriesName = sbm.getSeries().name;
        if (hasSeries(seriesName)) {
            markerMgr.addSeriesBoundMarker(sbm);
            repaint();
        } else {
            logger.log(Level.WARNING,
                    "Attempted to add a SeriesBoundMarker for a series this chart doesn't have. seriesName = {0}",
                    new Object[]{seriesName});
        }
    }
    /**
     * @param visibleOnly 
     * If true, then the view will only be fit around the visible series
     */
    public void fitViewToData(boolean visibleOnly) {
        view.fitViewToData(true);
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

    @Override
    public void seriesDataChanged(String seriesName) {
        repaint();
    }

    @Override
    public void seriesOffsetChanged(String seriesName, int oldOffset, int newOffset) {
        // All SeriesBoundMarkers need their drawn positions updated
        markerMgr.syncMarkersWithSeriesOffset(seriesName);
        
        repaint();
    }
    
    private class XY_ChartView extends ChartView implements ChartViewListener, MouseWheelListener {
        private final Color OVERLAY_BG_COLOR = new Color(0, 0, 0, 100);
        
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
        
        // Mouse location in pixels, relative to XY_Chart's upper left corner
        private Vector2D mousePosPx = null;
        private boolean mouseLocationVisible = true;
        
        
        public XY_ChartView() {
            addChartViewListener(this);
            addMouseWheelListener(this);
            addMouseListener(new PopClickListener());
        }
        
        /**
         * @param visibleOnly 
         * If true, then the view will only be fit around the visible series
         */
        public void fitViewToData(boolean visibleOnly) {
            // rescompute min/max bounds point based on series data and optional visibilty
            Vector2D minB = null;
            Vector2D maxB = null;
            if (visibleOnly) {
                for (Series s : allSeries) {
                    if (s.getVisible()) {
                        if (minB == null) {
                            minB = (Vector2D)s.minValue();
                            maxB = (Vector2D)s.maxValue();
                        } else {
                            minB = VectorUtils.min(minB, (Vector2D)s.minValue());
                            maxB = VectorUtils.max(maxB, (Vector2D)s.maxValue());
                        }
                    }
                }
            } else {
                minB = minBounds;
                maxB = maxBounds;
            }
            
            // no data yet or none of the series are visible
            if (minB == null || maxB == null) {
                return;
            }

            Vector2D dataDiag = maxBounds.subtract(minBounds);
            Vector2D dispDiag = new Vector2D(getWidth(), getHeight());
            double dataSlope = dataDiag.getY() / dataDiag.getX();
            double dispSlope = dispDiag.getY() / dispDiag.getX();
            if (dataSlope > dispSlope) {
                // visible data is constrained by screen height
                pxPerValue = (double)getHeight() / dataDiag.getY();
            } else {
                // visible data is constrained by screen width
                pxPerValue = (double)getWidth() / dataDiag.getX();
            }
            Vector2D middle = minBounds.add(dataDiag.scalarMultiply(0.5));
            double upperLeftLocX = middle.getX() - (dispDiag.getX() * (0.5 / pxPerValue) * scale.getX());
            double upperLeftLocY = middle.getY() - (dispDiag.getY() * (0.5 / pxPerValue) * scale.getY());
            upperLeftLocation = new Vector2D(upperLeftLocX, upperLeftLocY);
            
            repaint();
        }
        
        public void setMouseLocationVisbile(boolean visible) {
            boolean oldValue = mouseLocationVisible;
            mouseLocationVisible = visible;
            if (oldValue != mouseLocationVisible) {
                repaint();
            }
        }
        
        public boolean getMouseLocationVisible() {
            return mouseLocationVisible;
        }

        private void drawSeries(Graphics g, Vector2D upperLeftValue, Series<Vector2D> series) {
            List<Vector2D> sData = series.getData();
            if (sData == null) {
                return;
            }
            
            Graphics2D g2 = (Graphics2D)g;
            g2.setColor(series.getColor());
            if (series.isBolded()) {
                g2.setStroke(new BasicStroke(Series.BOLD_THICKNESS));
            } else {
                g2.setStroke(new BasicStroke(Series.NORMAL_THICKNESS));
            }
            
            boolean firstPass = true;
            int prevX = 0;
            int prevY = 0;
            for (Vector2D p : sData) {
                int x = (int)((p.getX() - upperLeftValue.getX()) * pxPerValue);
                int y = (int)((p.getY() - upperLeftValue.getY()) * pxPerValue);
                if ( ! firstPass) {
                    g.drawLine(prevX, prevY, x, y);
                }
                
                prevX = x; prevY = y;
                firstPass = false;
            }
        }
        
        private void drawVisibleSeries(Graphics g, Vector2D upperLeftValue) {
            for (Series series : allSeries) {
                if (series.getVisible()) {
                    drawSeries(g, upperLeftValue, series);
                }
            }
        }
        
        private void drawMarker(Graphics g, Vector2D upperLeftValue, Marker m) {
            Vector2D pxScale = new Vector2D(pxPerValue,pxPerValue);
            m.paintMarker(g, upperLeftValue, pxScale);
        }
        
        private void drawSeriesBoundMarkers(Graphics g, Vector2D upperLeftValue) {
            for (List<SeriesBoundMarker> sbmList : markerMgr.getAllSBMsBySeriesName().values()) {
                for (SeriesBoundMarker sbm : sbmList) {
                    if (sbm.getSeries().getVisible() && sbm.getMarker().isVisible()) {
                        drawMarker(g, upperLeftValue, sbm.getMarker());
                    } else {
                        // don't display if the series is not visible
                        break;
                    }
                }
            }
        }
        
        private void drawFloatingMarkers(Graphics g, Vector2D upperLeftValue) {
            for (Marker m : markerMgr.getAllFloatingMarkers()) {
                drawMarker(g, upperLeftValue, m);
            }
        }
        
        private void drawOverlays(Graphics g, Vector2D upperLeftValue) {
            if (mousePosPx != null && mouseLocationVisible) {
                Vector2D mousePosVal = upperLeftValue.add(px2val(mousePosPx));
                
                String str = String.format("x: %+f; y: %+f", mousePosVal.getX(), mousePosVal.getY());
                LabelGroup labelGroup = new LabelGroup();
                labelGroup.addLabel(new Label(str, Color.LIGHT_GRAY));
                labelGroup.draw(g, OVERLAY_BG_COLOR, DrawOrigin.LOWER_LEFT, 0, getHeight(), 2, 2);
            }
        }
        
        /**
         * Converts an x,y location in visible pixel space to and x,y location
         * in "value" space where (0,0) is the upper left corner of the chart.
         * @param pxVect
         * x,y location to convert to value space
         * @return 
         * the converted vector
         */
        private Vector2D px2val(Vector2D pxVect) {
            double x = pxVect.getX() * scale.getX() / pxPerValue;
            double y = pxVect.getY() * scale.getY() / pxPerValue;
            return new Vector2D(x, y);
        }
        
        /**
         * Converts an x,y location in visible pixel space to and x,y location
         * in the chart's global "value" space
         * @param pxVect
         * x,y location to convert to value space
         * @return 
         * the converted vector
         */
        private Vector2D px2val_global(Vector2D pxVect) {
            Vector2D upperLeft = upperLeftLocation;
            if (dragVectorPx != null) {
                upperLeft = upperLeftLocation.subtract(px2val(dragVectorPx));
            }
            return upperLeft.add(px2val(pxVect));
        }
        
        private Vector2D val2px(Vector2D valVect) {
            double x = valVect.getX() * scale.getX() * pxPerValue;
            double y = valVect.getY() * scale.getY() * pxPerValue;
            return new Vector2D(x,y);
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
            
            // flip axis according to scale when drawing series data
            Graphics2D g2d = (Graphics2D)g;
            g2d.scale(scale.getX(), scale.getY());
            
            drawVisibleSeries(g, upperLeftValue);
            drawSeriesBoundMarkers(g, upperLeftValue);
            drawFloatingMarkers(g, upperLeftValue);
            
            // flip axis back when drawing overlays, so text looks normal
            g2d.scale(scale.getX(), scale.getY());
            
            drawOverlays(g, upperLeftValue);
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
        public void onMouseMotion(MouseEvent e) {
        }

        @Override
        public void mouseWheelMoved(MouseWheelEvent e) {
            double ZOOM_AMOUNT = 0.2;
            boolean zoomIn = e.getWheelRotation() < 0;
            xyChartZoom(zoomIn, VectorUtils.toVector(e), ZOOM_AMOUNT);
        }

        @Override
        public void mouseMoved(MouseEvent e) {
            super.mouseMoved(e);
            updateMousePosition(e);
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            super.mouseDragged(e);
            updateMousePosition(e);
        }
        
        private void updateMousePosition(MouseEvent e) {
            mousePosPx = new Vector2D(e.getX(),e.getY());
            if (mouseLocationVisible) {
                repaint();
            }
        }
    }
    
    private class PopupMenu extends JPopupMenu {
        public final JMenuItem locationVisibleMenuItem = new JCheckBoxMenuItem("Mouse location visible");
        
        private final JFileChooser fc = new JFileChooser();
        
        public PopupMenu() {
            add(locationVisibleMenuItem);
            
            locationVisibleMenuItem.getModel().setSelected(view.getMouseLocationVisible());
            locationVisibleMenuItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent arg0) {
                    view.setMouseLocationVisbile(locationVisibleMenuItem.getModel().isSelected());
                }
            });
        }
    }
    
    private class PopClickListener extends MouseAdapter {
        @Override
        public void mouseReleased(MouseEvent e) {
            if (e.isPopupTrigger())
                doPop(e);
        }

        @Override
        public void mousePressed(MouseEvent e) {
            if (e.isPopupTrigger())
                doPop(e);
        }
        
        private void doPop(MouseEvent e) {
            PopupMenu menu = new PopupMenu();
            menu.show(e.getComponent(), e.getX(), e.getY());
        }
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
