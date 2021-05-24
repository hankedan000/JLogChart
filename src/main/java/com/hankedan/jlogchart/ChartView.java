/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.hankedan.jlogchart;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
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
        MouseListener, MouseMotionListener {
    private final Logger logger = Logger.getLogger(ChartView.class.getName());

    /**
     * Model used to manage the scroll bar and chart's visible samples.
     * 
     * value   -> left most visible sample
     * extent  -> visible range of samples indices displayed in chart currently
     * minimum -> always zero
     * maximum -> the maximum number of samples across all series
     */
    private final BoundedRangeModel xRange = new DefaultBoundedRangeModel();
    private final BoundedRangeModel yRange = new DefaultBoundedRangeModel();

    // Set to true if the mouse is hovering over the chart
    private boolean mouseFocused = false;

    /**
     * IDLE
     * No drag rectangle is drawn to the chart.
     * This state is the default state when the LogChart is constructed.
     * This state can be entered when the user single clicks on a point in
     * the chart.
     */
    private static final int DRAG_STATE_IDLE = 0;
    /**
     * PRESSED
     * No drag rectangle is drawn to the chart.
     * This state is only transitioned to temporarily. It's entered from
     * IDLE state when the mouseClicked() event occurs.
     */
    private static final int DRAG_STATE_PRESSED = 1;
    /**
     * DRAGGING
     * Drag rectangle is drawn to the chart.
     * This state is entered from the PRESSED state when the mouseDragged()
     * event occurs.
     */
    private static final int DRAG_STATE_DRAGGING = 2;
    /**
     * COMPLETE
     * Drag rectangle is drawn to the chart.
     * This state is entered from the DRAGGING state when the mouseReleased()
     * event occurs.
     */
    private static final int DRAG_STATE_COMPLETE = 3;
    private int dragState = DRAG_STATE_IDLE;
    private int selectionSamp1 = 0;
    private int selectionSamp2 = 0;
    
    // If enabled, when the user clicks near edge, the chart will pan
    private boolean clickToPanEnabled = true;
    
    public ChartView() {
        addMouseListener(this);
        addMouseMotionListener(this);
        addMouseWheelListener(this);
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
    
    public int visibleSamps() {
        return visibleSamps(xRange);
    }
    
    protected int visibleSamps(BoundedRangeModel brm) {
        return brm.getExtent();
    }
    
    public int leftViewedSamp() {
        return leftViewedSamp(xRange);
    }
    
    protected int leftViewedSamp(BoundedRangeModel brm) {
        return brm.getValue();
    }
    
    public int rightViewedSamp() {
        return rightViewedSamp(xRange);
    }
    
    protected int rightViewedSamp(BoundedRangeModel brm) {
        return brm.getValue() + brm.getExtent();
    }

    public void setViewBounds(int leftIdx, int rightIdx) {
        setViewBounds(leftIdx, rightIdx, false);
    }
    
    protected void setViewBounds(int leftIdx, int rightIdx, boolean updateFromScrollEvent) {
        if (leftIdx >= rightIdx) {
            logger.log(Level.WARNING,
                    "View bounds are invalid. leftIdx must be < rightIdx." + 
                    " leftIdx = {0}; rightIdx = {1}",
                    new Object[]{leftIdx,rightIdx});
            return;
        }

        // Update the bounds accordingly
        int newLeftViewedSamp = leftIdx;
        if (leftIdx < 0) {
            newLeftViewedSamp = 0;
        }
        int newRightViewedSamp = rightIdx;
        if (rightIdx > xRange.getMaximum()) {
            newRightViewedSamp = xRange.getMaximum();
        }
        
        int visibleSamps = newRightViewedSamp - newLeftViewedSamp;
//        ignoreNextScrollEvent = updateFromScrollEvent;
        xRange.setValue(newLeftViewedSamp);
        xRange.setExtent(visibleSamps);

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
        int visibleSamps = visibleSamps() + 1;
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

        // No need to set repaintRequired, setViewBounds() repaints
        setViewBounds(newLeftViewedSamp, newRightViewedSamp);
    }

    /**
     * Gets the nearest sample index to the x pixel position on the chart.
     * @param xPos
     * Pixel position on the chart
     * @return 
     * The nearest time sample index
     */
    protected int getNearestSampleIdx(int xPos) {
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
     *   1.0 -> new visible portion is 0% of current visible portion
     *   0.0 -> new visible portion is 100% of current visible portion
     */
    private void zoom(boolean zoomIn, int center, double amount) {
        if (amount <= 0) {
            amount = 0;
        } else if (amount >= 1.0) {
            amount = 1.0;
        }

        // We center the zooming around the mouse's x position
        double leftZoomRatio = (double)(center) / getWidth();
        double rightZoomRatio = 1.0 - leftZoomRatio;

        double sampsZoomed = visibleSamps() * amount;
        int newLeftViewedSamp = leftViewedSamp();
        int newRightViewedSamp = rightViewedSamp();
        if (zoomIn) {
            // zoom in
            newLeftViewedSamp += sampsZoomed * leftZoomRatio;
            newRightViewedSamp -= sampsZoomed * rightZoomRatio;
        } else {
            // zoom out
            newLeftViewedSamp -= sampsZoomed * leftZoomRatio;
            newRightViewedSamp += sampsZoomed * rightZoomRatio;
        }

        // Update view bounds and redraw chart
        if (newLeftViewedSamp < newRightViewedSamp) {
            setViewBounds(newLeftViewedSamp, newRightViewedSamp);
        }
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        boolean repaintRequired = false;
        
        if (e.getButton() == MouseEvent.BUTTON1) {// Left click
            // See if selection is new, if so update and redraw
            int newSelectedSampleIdx = getNearestSampleIdx(e.getX());
            // FIXME need to move sample selection/show to subclass
//            boolean isNewSelection = newSelectedSampleIdx != selectedSampleIdx;
//            if ( ! showSelectedSample || isNewSelection) {
//                selectedSampleIdx = getNearestSampleIdx(e.getX());
//                showSelectedSample = true;
//                dragState = DRAG_STATE_IDLE;
//                repaintRequired = true;
//            }
            
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
                }
            }
        } else if (e.getButton() == MouseEvent.BUTTON3) {// Right click
            // Clear selection and drag on a right click
            // FIXME need to move sample selection/show to subclass
//            showSelectedSample = false;
            dragState = DRAG_STATE_IDLE;
            repaintRequired = true;
        }
        
        // Repaint the chart if an update is required
        if (repaintRequired) {
            repaint();
        }
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (dragState == DRAG_STATE_IDLE || dragState == DRAG_STATE_COMPLETE) {
            selectionSamp1 = getNearestSampleIdx(e.getX());
            dragState = DRAG_STATE_PRESSED;
        }
        repaint();
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (dragState == DRAG_STATE_DRAGGING) {
            selectionSamp2 = getNearestSampleIdx(e.getX());
            dragState = DRAG_STATE_COMPLETE;
        }
        repaint();
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        mouseFocused = true;
        repaint();
    }

    @Override
    public void mouseExited(MouseEvent e) {
        mouseFocused = false;
        repaint();
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if (dragState == DRAG_STATE_PRESSED) {
            dragState = DRAG_STATE_DRAGGING;
        }
        selectionSamp2 = getNearestSampleIdx(e.getX());
        repaint();
    }

    @Override
    public void mouseMoved(MouseEvent e) {
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        zoom(e.getWheelRotation() < 0, e.getX(), 0.2);
    }
    
}

