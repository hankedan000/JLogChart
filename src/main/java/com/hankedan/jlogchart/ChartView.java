/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.hankedan.jlogchart;

import java.awt.Graphics;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BoundedRangeModel;
import javax.swing.DefaultBoundedRangeModel;
import javax.swing.JPanel;

/**
 *
 * @author daniel
 */
public class ChartView extends JPanel implements MouseWheelListener,
        MouseListener, MouseMotionListener, AdjustmentListener {
    private final Logger logger = Logger.getLogger(ChartView.class.getName());

    /**
     * Model used to manage the scroll bar and chart's visible samples.
     * 
     * value   -> left most visible sample
     * extent  -> visible range of samples indices displayed in chart currently
     * minimum -> always zero
     * maximum -> the maximum number of samples across all series
     */
    protected BoundedRangeModel xRange = new DefaultBoundedRangeModel();
    protected BoundedRangeModel yRange = new DefaultBoundedRangeModel();

    // Set to true if the mouse is hovering over the chart
    private boolean mouseFocused = false;
    
    // Set to true if zooming with the mouse wheel is enabled for each direction
    private boolean horzWheelZoomingEnable = true;
    private boolean vertWheelZoomingEnable = true;

    /**
     * IDLE
     * This state is the default state when the LogChart is constructed.
     * This state can be entered when the user single clicks on a point in
     * the chart.
     */
    private static final int DRAG_STATE_IDLE = 0;
    /**
     * PRESSED
     * This state is only transitioned to temporarily. It's entered from
     * IDLE state when the mouseClicked() event occurs.
     */
    private static final int DRAG_STATE_PRESSED = 1;
    /**
     * DRAGGING
     * This state is entered from the PRESSED state when the mouseDragged()
     * event occurs.
     */
    private static final int DRAG_STATE_DRAGGING = 2;
    private int dragState = DRAG_STATE_IDLE;
    private MouseEvent mEvt1 = null;
    private MouseEvent mEvt2 = null;
    private int selectionAbsSamp1 = 0;
    private int selectionAbsSamp2 = 0;
    
    // If enabled, when the user clicks near edge, the chart will pan
    private boolean clickToPanEnabled = true;
    
    private final List<ChartViewListener> listeners = new ArrayList();
    
    // Other views that are listening to my scroll & zoom events
    private final List<ChartView> viewsBoundToMe = new ArrayList();
    
    // The view that I'm taking scroll & zoom events from
    private ChartView myBoundView = null;
    
    public ChartView() {
        addMouseListener(this);
        addMouseMotionListener(this);
        addMouseWheelListener(this);
    }
    
    public void addChartViewListener(ChartViewListener l) {
        listeners.add(l);
    }
    
    public void removeChartViewLisener(ChartViewListener l) {
        listeners.remove(l);
    }
    
    public void bindTo(ChartView other) {
        // Cannot rebind to another view
        if (myBoundView != null) {
            throw new UnsupportedOperationException("ChartView is already bound!");
        }
        
        if (other != null) {
            other.viewsBoundToMe.add(this);
            xRange = other.xRange;
            yRange = other.yRange;
        }
    }
    
    public void setX_RangeMin(int min) {
        xRange.setMinimum(min);
    }
    
    public void setX_RangeMax(int max) {
        xRange.setMaximum(max);
    }
    
    public void setViewableX_Range(int leftViewedSamp, int extent) {
        xRange.setValue(leftViewedSamp);
        xRange.setExtent(extent);
    }
    
    public BoundedRangeModel getX_RangeModel() {
        return xRange;
    }

    public double getPxPerSample() {
        return getPxPerSample(xRange);
    }
    
    protected double getPxPerSample(BoundedRangeModel brm) {
        int viewableSamps = visibleSamps(brm);
        if (viewableSamps > 0) {
            return (double)getWidth() / viewableSamps;
        } else {
            return 0;
        }
    }
    
    public int visibleHorzSamps() {
        return visibleSamps(xRange);
    }
    
    protected int visibleSamps(BoundedRangeModel brm) {
        return brm.getExtent();
    }
    
    protected int lowerViewedSamp(BoundedRangeModel brm) {
        return brm.getValue();
    }
    
    protected int upperViewedSamp(BoundedRangeModel brm) {
        return brm.getValue() + brm.getExtent();
    }
    
    public int leftViewedSamp() {
        return lowerViewedSamp(xRange);
    }
    
    public int rightViewedSamp() {
        return upperViewedSamp(xRange);
    }
    
    public int topViewedSamp() {
        return lowerViewedSamp(yRange);
    }
    
    public int bottomViewedSamp() {
        return upperViewedSamp(yRange);
    }
    
    public void setHorzViewBounds(int leftIdx, int rightIdx) {
        setViewBounds(leftIdx, rightIdx, xRange);
    }

    public void setVertViewBounds(int topIdx, int bottomIdx) {
        setViewBounds(topIdx, bottomIdx, yRange);
    }
    
    protected void setWheelZoomEnables(boolean horzEnable, boolean vertEnable) {
        horzWheelZoomingEnable = horzEnable;
        vertWheelZoomingEnable = vertEnable;
    }
    
    protected void setViewBounds(int lowerIdx, int upperIdx, BoundedRangeModel brm) {
        if (lowerIdx >= upperIdx) {
            logger.log(Level.WARNING,
                    "View bounds are invalid. lowerIdx must be < upperIdx." + 
                    " leftIdx = {0}; upperIdx = {1}",
                    new Object[]{lowerIdx,upperIdx});
            return;
        }

        // Update the bounds accordingly
        int newLowerViewedSamp = lowerIdx;
        if (lowerIdx < brm.getMinimum()) {
            newLowerViewedSamp = brm.getMinimum();
        }
        int newUpperViewedSamp = upperIdx;
        if (upperIdx > brm.getMaximum()) {
            newUpperViewedSamp = brm.getMaximum();
        }
        
        int visibleSamps = newUpperViewedSamp - newLowerViewedSamp;
        brm.setValue(newLowerViewedSamp);
        brm.setExtent(visibleSamps);

        // Redraw chart with new bounds
        repaint();
    }
    
    /**
     * If enabled, when the user clicks near the edges of the chart, the visible
     * section will automatically pan to the clicking location.
     * 
     * @param enabled 
     * True if the feature is enabled
     */
    public void setClickToPan(boolean enabled) {
        clickToPanEnabled = enabled;
    }
    
    public boolean getClickToPan() {
        return clickToPanEnabled;
    }
    
    /**
     * Pan the chart's view left/right
     * 
     * @param xPixels 
     * Number of pixels to pan the view by. Positive numbers will shift the
     * view to the right.
     */
    public void panPixels(int xPixels) {
        int visibleSamps = visibleHorzSamps() + 1;
        int panSamps = (int)(xPixels / getPxPerSample());
        int newLeftViewedSamp = leftViewedSamp() + panSamps;
        int newRightViewedSamp = rightViewedSamp() + panSamps;

        // Make sure new window doesn't step outside sample range
        if (newRightViewedSamp >= xRange.getMaximum()) {
            newRightViewedSamp = xRange.getMaximum() - 1;
            newLeftViewedSamp = newRightViewedSamp - visibleSamps;
        } else if (newLeftViewedSamp < 0) {
            newLeftViewedSamp = 0;
            newRightViewedSamp = visibleSamps;
        }

        // No need to set repaintRequired, setHorzViewBounds() repaints
        setHorzViewBounds(newLeftViewedSamp, newRightViewedSamp);
    }
    
    /**
     * Sets the viewable area of the chart so that the full x-range of the
     * data is visible.
     */
    public void fitViewWidthToData() {
        xRange.setValue(xRange.getMinimum());
        xRange.setExtent(xRange.getMaximum());
    }
    /**
     * Sets the viewable area of the chart so that the full y-range of the
     * data is visible.
     */
    public void fitViewHeightToData() {
        yRange.setValue(yRange.getMinimum());
        yRange.setExtent(yRange.getMaximum());
    }

    /**
     * Gets the nearest sample index to the x pixel position on the chart.
     * @param xPos
     * Pixel position on the chart
     * @return 
     * The nearest time sample index
     */
    protected int getNearestAbsSampleIdx(int xPos) {
        double pxPerSamp = getPxPerSample();
        if (pxPerSamp > 0) {
            return leftViewedSamp() + (int)Math.round(xPos / pxPerSamp);
        } else {
            return leftViewedSamp();
        }
    }

    /**
     * Zooms the visible portion of the chart and redraws
     * @param zoomIn
     * True if zooming in, false to zoom out
     * @param center
     * The x pixel location to center the zoom around
     * @param amount 
     * Amount to zoom by. Valid range is 0.0 to 1.0.
     *   1.0 -> new visible portion is 0% times current visible portion
     *   0.0 -> new visible portion is 100% times current visible portion
     */
    protected void zoomX(boolean zoomIn, int center, double amount) {
        zoom1D(zoomIn,center,getWidth(),amount,xRange);
    }

    /**
     * Zooms the visible portion of the chart and redraws
     * @param zoomIn
     * True if zooming in, false to zoom out
     * @param center
     * The y pixel location to center the zoom around
     * @param amount 
     * Amount to zoom by. Valid range is 0.0 to 1.0.
     *   1.0 -> new visible portion is 0% times current visible portion
     *   0.0 -> new visible portion is 100% times current visible portion
     */
    protected void zoomY(boolean zoomIn, int center, double amount) {
        zoom1D(zoomIn,center,getHeight(),amount,yRange);
    }

    /**
     * Zooms the visible portion of the chart and redraws
     * @param zoomIn
     * True if zooming in, false to zoom out
     * @param center
     * The x or y pixel component to center the zoom around
     * @param charWidthOrHeight
     * Width or height of the screen. If zooming horizontally, pass in the view
     * width in pixels. If zooming vertically, pass in the view width in pixels.
     * @param amount 
     * Amount to zoom by. Valid range is 0.0 to 1.0.
     *   1.0 -> new visible portion is 0% times current visible portion
     *   0.0 -> new visible portion is 100% times current visible portion
     * @param brm
     * The range to perform the zoom on
     */
    protected void zoom1D(boolean zoomIn, int center, int charWidthOrHeight, double amount, BoundedRangeModel brm) {
        if (amount <= 0) {
            amount = 0;
        } else if (amount >= 1.0) {
            amount = 1.0;
        }

        // We center the zooming around the mouse's position
        double lowerZoomRatio = (double)(center) / charWidthOrHeight;
        double upperZoomRatio = 1.0 - lowerZoomRatio;

        double sampsZoomed = visibleSamps(brm) * amount;
        int newLowerViewedSamp = lowerViewedSamp(brm);
        int newUpperViewedSamp = upperViewedSamp(brm);
        if (zoomIn) {
            // zoom1D in
            newLowerViewedSamp += sampsZoomed * lowerZoomRatio;
            newUpperViewedSamp -= sampsZoomed * upperZoomRatio;
        } else {
            // zoom1D out
            newLowerViewedSamp -= sampsZoomed * lowerZoomRatio;
            newUpperViewedSamp += sampsZoomed * upperZoomRatio;
        }

        // Update view bounds and redraw chart
        if (newLowerViewedSamp < newUpperViewedSamp) {
            setViewBounds(newLowerViewedSamp, newUpperViewedSamp, brm);
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        
        List<ChartView> cvToRemove = new ArrayList();
        // If I'm repainting, then tell those bound to me to also repaint
        for (ChartView cv : viewsBoundToMe) {
            if (cv == null) {
                // must not exist anymore, mark for removal
                cvToRemove.add(cv);
            } else {
                cv.repaint();
            }
        }
        
        // Clean up bound charts that no longer exists
        viewsBoundToMe.removeAll(cvToRemove);
    }
    
    @Override
    public void mouseClicked(MouseEvent e) {
        boolean isPanClick = false;
        boolean sampleClicked = false;
        int clickedAbsSample = 0;

        if (e.getButton() == MouseEvent.BUTTON1) {// Left click
            sampleClicked = true;
            clickedAbsSample = getNearestAbsSampleIdx(e.getX());
            
            // Click panning centers the visible area around the click location
            if (clickToPanEnabled) {
                double CLICK_PAN_MARGIN = 0.1;
                int marginPx = (int)(getWidth() * CLICK_PAN_MARGIN);
                
                boolean withinPanMargin = false;
                withinPanMargin = withinPanMargin || e.getX() < marginPx;
                withinPanMargin = withinPanMargin || (getWidth() - e.getX()) < marginPx;
                
                // Recenter visible section around clicked location
                if (withinPanMargin) {
                    int panDistance = e.getX() - getWidth() / 2;
                    panPixels(panDistance);
                    isPanClick = true;
                }
            }
        }
        
        // Notify any listeners of the event
        for (ChartViewListener cvl : listeners) {
            if (cvl == null) {
                continue;
            }
            
            if (sampleClicked) {
                cvl.onSampleClicked(clickedAbsSample, isPanClick);
            }
            if (e.getButton() == MouseEvent.BUTTON1) {
                cvl.onLeftClicked(e);
            } else if (e.getButton() == MouseEvent.BUTTON2) {
                cvl.onMiddleClicked(e);
            } else if (e.getButton() == MouseEvent.BUTTON3) {
                cvl.onRightClicked(e);
            }
        }
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (dragState == DRAG_STATE_IDLE) {
            mEvt1 = e;
            selectionAbsSamp1 = getNearestAbsSampleIdx(e.getX());
            dragState = DRAG_STATE_PRESSED;
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (dragState == DRAG_STATE_DRAGGING) {
            mEvt2 = e;
            selectionAbsSamp2 = getNearestAbsSampleIdx(e.getX());
            
            // Notify any listeners of the drag complete event
            for (ChartViewListener cvl : listeners) {
                if (cvl == null) {
                    continue;
                }

                cvl.onDragComplete(mEvt1, mEvt2);
                cvl.onDragComplete(selectionAbsSamp1, selectionAbsSamp2);
            }
        }
        dragState = DRAG_STATE_IDLE;
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        mouseFocused = true;
    }

    @Override
    public void mouseExited(MouseEvent e) {
        mouseFocused = false;
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        boolean startedDrag = false;
        if (dragState == DRAG_STATE_PRESSED) {
            startedDrag = true;
            dragState = DRAG_STATE_DRAGGING;
            // Note: selectionSamp1 was assigned in the mousePressed handler
        } else {
            mEvt2 = e;
            selectionAbsSamp2 = getNearestAbsSampleIdx(e.getX());
        }
        
        // Notify any listeners of the drag start/complete event
        for (ChartViewListener cvl : listeners) {
            if (cvl == null) {
                continue;
            }

            if (startedDrag) {
                cvl.onDragStarted(mEvt1);
                cvl.onDragStarted(selectionAbsSamp1);
            } else {
                cvl.onDragging(mEvt1, mEvt2);
                cvl.onDragging(selectionAbsSamp1, selectionAbsSamp2);
            }
        }
    }

    @Override
    public void mouseMoved(MouseEvent e) {
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        double ZOOM_AMOUNT = 0.2;
        boolean zoomIn = e.getWheelRotation() < 0;
        if (horzWheelZoomingEnable) {
            zoomX(zoomIn, e.getX(), ZOOM_AMOUNT);
        }
        if (vertWheelZoomingEnable) {
            zoomY(zoomIn, e.getY(), ZOOM_AMOUNT);
        }
    }

    @Override
    public void adjustmentValueChanged(AdjustmentEvent e) {
        int newLeftViewSamp = e.getValue();
        int newRightViewSamp = newLeftViewSamp + visibleHorzSamps();
        if (newLeftViewSamp < newRightViewSamp) {
            setHorzViewBounds(newLeftViewSamp, newRightViewSamp);
        }
    }
    
}

