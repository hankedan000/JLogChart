/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.hankedan.jlogchart;

import com.formdev.flatlaf.FlatDarkLaf;
import com.hankedan.jlogchart.MiniMapScrollbar.MiniMapable;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BoundedRangeModel;
import javax.swing.DefaultBoundedRangeModel;
import javax.swing.JFrame;

/**
 *
 * @author daniel
 */
public class JLogChart extends javax.swing.JPanel implements MouseListener,
        MouseMotionListener, MouseWheelListener, Series.SeriesChangeListener,
        AdjustmentListener, MiniMapable {
    private final Logger logger = Logger.getLogger(JLogChart.class.getName());

    // Set to true if the mouse is hovering over the chart
    private boolean mouseFocused = false;

    // Current mouse position relative to chart's graphics origin
    private int mouseX = 0;
    private int mouseY = 0;

    // The user can select and place a vertical bar on a sample
    private boolean showSelectedSample = false;
    private int selectedSampleIdx = 0;

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

    // Delta time in seconds between time samples
    private double dt = 0.0;

    // The number of time samples in the record
    private int chartSamps = 0;

    /**
     * If set to true, drawn samples will be decimated when the number of
     * visible samples is greater than the chart's width in pixels. This
     * helps improve draw times for very large data sets.
     */
    private boolean sampleDecimationEnabled = true;
    
    private boolean singleY_ScaleEnabled = false;
    
    // Min/max values across all series. Used for single scale y-axis mode
    private double maxValueY = Double.NEGATIVE_INFINITY;
    private double minValueY = Double.POSITIVE_INFINITY;
    
    // If enabled, when the user clicks near edge, the chart will pan
    private boolean clickToPanEnabled = true;
    
    // Set to true during view bounds update to supress a cyclical scroll event
    private boolean ignoreNextScrollEvent = false;
    
    /**
     * Model used to manage the scroll bar and chart's visible samples.
     * 
     * value   -> left most visible sample
     * extent  -> visible range of samples indices displayed in chart currently
     * minimum -> always zero
     * maximum -> the maximum number of samples across all series
     */
    private final BoundedRangeModel sampRange = new DefaultBoundedRangeModel();

    private final Color[] SERIES_COLOR_PALETTE = {
        Color.RED,
        Color.ORANGE,
        Color.YELLOW,
        Color.GREEN,
        Color.BLUE,
        Color.MAGENTA,
    };

    // Map of all added chart series data vectors
    private final List<Series> allSeries = new ArrayList<>();

    public JLogChart() {
        initComponents();
        
        addMouseListener(this);
        addMouseMotionListener(this);
        addMouseWheelListener(this);
        
        scrollbarPanel.setBackground(new Color(0,0,0,0));
        scrollbar.setModel(sampRange);
        scrollbar.addAdjustmentListener(this);
        scrollbar.setVisible(false);
        miniMapScrollbar.setModel(sampRange);
        miniMapScrollbar.addAdjustmentListener(this);
        miniMapScrollbar.setMiniMapable(this);
        miniMapScrollbar.setVisible(true);
    }

    public void setSampleRate(double rateHz) {
        if (rateHz > 0) {
            dt = 1 / rateHz;
        } else {
            throw new IllegalArgumentException(String.format(
                    "rateHz of %s is not allowed. Must be >0",rateHz));
        }
    }

    public void setSampleDecimation(boolean enabled) {
        sampleDecimationEnabled = enabled;
        repaint();
    }
    
    /**
     * If enabled, the y-axis for all series will use the same scale, rather
     * than each series have it's on scaling based on their min/max value.
     * 
     * @param enabled 
     * True if the y-axis scale should be the same for all series
     */
    public void setSingleY_Scale(boolean enabled) {
        singleY_ScaleEnabled = enabled;
        repaint();
    }
    
    public boolean getSingleY_Scale() {
        return singleY_ScaleEnabled;
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
    
    public void setScrollbarVisible(boolean visible) {
        scrollbar.setVisible(visible);
    }
    
    public boolean isScrollbarVisible() {
        return scrollbar.isVisible();
    }
    
    public void setMiniMapVisible(boolean visible) {
        miniMapScrollbar.setVisible(visible);
    }
    
    public boolean isMiniMapVisible() {
        return miniMapScrollbar.isVisible();
    }

    public boolean hasSeries(String name) {
        return getSeriesByName(name) != null;
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
    
    public Series addSeries(String name, List<Double> data) {
        Series series = getSeriesByName(name);
        
        if (series != null) {
            logger.log(Level.WARNING,
                    "Duplicate add of series data with name {0}. Ignoring.",
                    new Object[]{name});
        } else {
            int colorIdx = allSeries.size() % SERIES_COLOR_PALETTE.length;
            Color color = SERIES_COLOR_PALETTE[colorIdx];
            series = new Series(name, color, data);
            allSeries.add(series);
            minValueY = Double.min(minValueY, series.minValue());
            maxValueY = Double.max(maxValueY, series.maxValue());
            chartSamps = Integer.max(chartSamps, data.size());
            int newLeftViewedSamp = 0;
            int newRightViewedSamp = chartSamps - 1;
            if (allSeries.size() > 1) {
                newLeftViewedSamp = Integer.max(leftViewedSamp(), newLeftViewedSamp);
                newRightViewedSamp = Integer.min(rightViewedSamp(), newRightViewedSamp);
            }
            setViewBounds(newLeftViewedSamp, newRightViewedSamp);
            series.addSeriesListener(this);
            logger.log(Level.FINE,
                    "leftViewedSamp = {0}; rightViewedSamp = {1};",
                    new Object[]{leftViewedSamp(),rightViewedSamp()});
            repaint();
        }
        
        return series;
    }

    public void removeSeries(String name) {
        int idxToRemove = -1;
        for (int i=0; i<allSeries.size(); i++) {
            Series series = allSeries.get(i);
            if (series.name.compareTo(name) == 0) {
                idxToRemove = i;
            }
        }

        // Remove the found series
        if (idxToRemove >= 0) {
            allSeries.remove(idxToRemove);
        } else {
            logger.log(Level.WARNING,
                    "Unknown series {0}. Ignoring remove.",
                    name);
        }
        
        // Recompute view bounds, y-scale, etc. then repaint the chart
        updateAfterDataChange();
    }
    
    private int leftViewedSamp() {
        return leftViewedSamp(sampRange);
    }
    
    private int leftViewedSamp(BoundedRangeModel brm) {
        return brm.getValue();
    }
    
    private int rightViewedSamp() {
        return rightViewedSamp(sampRange);
    }
    
    private int rightViewedSamp(BoundedRangeModel brm) {
        return brm.getValue() + brm.getExtent();
    }
    
    private int visibleSamps() {
        return visibleSamps(sampRange);
    }
    
    private int visibleSamps(BoundedRangeModel brm) {
        return brm.getExtent();
    }

    public void setViewBounds(int leftIdx, int rightIdx) {
        setViewBounds(leftIdx, rightIdx, false);
    }
    
    private void setViewBounds(int leftIdx, int rightIdx, boolean updateFromScrollEvent) {
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
        if (rightIdx > chartSamps) {
            newRightViewedSamp = chartSamps;
        }
        
        int visibleSamps = newRightViewedSamp - newLeftViewedSamp;
        ignoreNextScrollEvent = updateFromScrollEvent;
        sampRange.setMinimum(0);
        sampRange.setMaximum(chartSamps);
        sampRange.setExtent(visibleSamps);
        sampRange.setValue(newLeftViewedSamp);

        // Redraw chart with new bounds
        repaint();
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
        if (newRightViewedSamp >= chartSamps) {
            newRightViewedSamp = chartSamps - 1;
            newLeftViewedSamp = newRightViewedSamp - visibleSamps;
        } else if (newLeftViewedSamp < 0) {
            newLeftViewedSamp = 0;
            newRightViewedSamp = visibleSamps;
        }

        // No need to set repaintRequired, setViewBounds() repaints
        setViewBounds(newLeftViewedSamp, newRightViewedSamp);
    }
    
    public Series getSeriesByName(String name) {
        for (Series series : allSeries) {
            if (series.name.compareTo(name) == 0) {
                return series;
            }
        }
        return null;
    }
    
    private void updateAfterDataChange() {
        chartSamps = 0;
        double newMinValueY = Double.POSITIVE_INFINITY;
        double newMaxValueY = Double.NEGATIVE_INFINITY;
        for (Series series : allSeries) {
            chartSamps = Integer.max(chartSamps, series.getData().size());
            newMinValueY = Double.min(newMinValueY, series.minValue());
            newMaxValueY = Double.max(newMaxValueY, series.maxValue());
        }

        // Update single scale min/max values
        minValueY = newMinValueY;
        maxValueY = newMaxValueY;

        // Update view bounds if they are now out of range
        int newLeftViewedSamp = 0;
        int newRightViewedSamp = 0;
        if ( ! allSeries.isEmpty()) {
            if (leftViewedSamp() >= chartSamps) {
                newLeftViewedSamp = 0;
            }
            if (rightViewedSamp() >= chartSamps) {
                newRightViewedSamp = chartSamps;
            }
        }

        if (newLeftViewedSamp < newRightViewedSamp) {
            setViewBounds(newLeftViewedSamp, newRightViewedSamp);
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

    /**
     * Gets the nearest sample index to the x pixel position on the chart.
     * @param xPos
     * Pixel position on the chart
     * @return 
     * The nearest time sample index
     */
    private int getNearestSampleIdx(int xPos) {
        double pxPerSamp = getPxPerSample();
        if (pxPerSamp > 0) {
            return leftViewedSamp() + (int)Math.round(xPos / pxPerSamp);
        } else {
            return leftViewedSamp();
        }
    }

    private double getPxPerSample() {
        return getPxPerSample(sampRange);
    }
    
    private double getPxPerSample(BoundedRangeModel brm) {
        int viewableSamps = visibleSamps(brm);
        if (viewableSamps > 0) {
            return (double)getWidth() / viewableSamps;
        } else {
            return 0;
        }
    }

    private void drawSelection(Graphics g) {
        double pxPerSamp = getPxPerSample();
        int offset1 = selectionSamp1 - leftViewedSamp();
        int offset2 = selectionSamp2 - leftViewedSamp();
        int x1 = (int)(offset1 * pxPerSamp);
        int x2 = (int)(offset2 * pxPerSamp);
        int width = Math.abs(x2 - x1);
        int leftX = Integer.min(x1, x2);
        g.setColor(new Color(200, 200, 200, 50));
        g.fillRect(leftX, 0, width, getHeight());
        
        // Draw end lines
        g.setColor(Color.LIGHT_GRAY);
        g.drawLine(x1, 0, x1, getHeight());
        g.drawLine(x2, 0, x2, getHeight());
    }

    private void drawSeries(Graphics g, Series series, BoundedRangeModel brm) {
        if (series.getData() == null) {
            return;
        }
        
        g.setColor(series.getColor());

        // Compute x-axis scaling factor
        double pxPerSamp = getPxPerSample(brm);
        
        // Compute y-axis scaling factor
        double minY = series.minValue();
        double maxY = series.maxValue();
        if (singleY_ScaleEnabled) {
            minY = minValueY;
            maxY = maxValueY;
        }
        double pxPerVal = getHeight() / (maxY - minY);

        /**
         * We really don't need to draw every single data point in the
         * series. Worst case scenario we draw a data point at each
         * horizontal pixel in the chart. This math computes the decimated
         * sample stride that helps improve draw time when the number of
         * horizontal chart pixels is less than the total number of 
         * visible samples.
         */
        int visibleSamps = visibleSamps(brm);
        double sampStride = 1.0;
        if (sampleDecimationEnabled && visibleSamps > getWidth()) {
            sampStride = visibleSamps / getWidth();
        }
        logger.log(Level.FINE,
                "visibleSamps = {0}; width = {1}; sampStride = {2}",
                new Object[]{visibleSamps,getWidth(),sampStride});

        int prevX = -1;
        int prevY = -1;
        int leftViewedSamp = leftViewedSamp(brm);
        int rightViewedSamp = rightViewedSamp(brm);
        double i = leftViewedSamp;
        while (i<=rightViewedSamp && i<series.getData().size()) {
            int currX = (int)((i - leftViewedSamp) * pxPerSamp);
            int currY = getHeight() - (int)((series.getData().get((int)i) - minY) * pxPerVal);

            if (prevX >= 0 && prevY >= 0) {
                g.drawLine(prevX, prevY, currX, currY);
            }

            prevX = currX;
            prevY = currY;

            i += sampStride;
        }
    }

    private void drawVisibleSeries(Graphics g, BoundedRangeModel brm) {
        for (Series series : allSeries) {
            if (series.getVisible()) {
                drawSeries(g, series, brm);
            }
        }
    }

    private void drawSelectedSample(Graphics g) {
        if (selectedSampleIdx < leftViewedSamp()) {
            return;
        } else if (selectedSampleIdx > rightViewedSamp()) {
            return;
        }

        g.setColor(Color.CYAN);
        int sampOffset = selectedSampleIdx - leftViewedSamp();
        int sampX = (int)(sampOffset * getPxPerSample());
        g.drawLine(sampX, 0, sampX, getHeight());
    }

    private class Label {
        public final String text;
        public final Color textColor;
        public final int x;
        public final int y;
        public final int margin;
        public static final int DEFAULT_MARGIN = 2;

        public Label(String text, Color textColor, int x, int y) {
            this(text,textColor,x,y,DEFAULT_MARGIN);
        }

        public Label(String text, Color textColor, int x, int y, int margin) {
            this.text = text;
            this.textColor = textColor;
            this.x = x;
            this.y = y;
            this.margin = margin;
        }

        public void draw(Graphics g, Color background) {
            g.setColor(background);
            FontMetrics fm = g.getFontMetrics();
            int textHeight = fm.getHeight();
            int bgWidth = margin * 2 + fm.stringWidth(text);
            int bgHeight = margin * 2 + textHeight;
            g.fillRect(x, y - fm.getMaxAscent() - margin, bgWidth, bgHeight);
            draw(g);
        }

        public void draw(Graphics g) {
            g.setColor(textColor);
            g.drawString(text, x + margin, y + margin);
        }
        
        public Rectangle2D getBoundingBox(Graphics g) {
            FontMetrics fm = g.getFontMetrics();
            int textHeight = fm.getHeight();
            int bgWidth = margin * 2 + fm.stringWidth(text);
            int bgHeight = margin * 2 + textHeight;
            return new Rectangle2D.Double(x, y - fm.getMaxAscent() - margin, bgWidth, bgHeight);
        }
    }
    
    private class LabelGroup {
        private List<Label> labels = new ArrayList<>();
        
        public void addLabel(Label l) {
            labels.add(l);
        }
        
        public void draw(Graphics g, Color background) {
            Rectangle2D bgBox = null;
            for (Label l : labels) {
                if (bgBox == null) {
                    bgBox = l.getBoundingBox(g);
                } else {
                    bgBox.add(l.getBoundingBox(g));
                }
            }
            
            if (bgBox != null) {
                g.setColor(background);
                g.fillRect(
                        (int)bgBox.getX(),
                        (int)bgBox.getY(),
                        (int)bgBox.getWidth(),
                        (int)bgBox.getHeight());
            }
            
            for (Label l : labels) {
                l.draw(g);
            }
        }
    }

    private void drawMinMaxOverlay(Graphics g) {
        int CHART_MARGIN = 3;// margin around chart to keep out of
        int TEXT_SEPERATION = 0;// px between text
        Color BG_COLOR = new Color(0, 0, 0, 50);
        
        FontMetrics fm = g.getFontMetrics();
        int textHeight = fm.getHeight();

        LabelGroup maxGroup = new LabelGroup();
        LabelGroup minGroup = new LabelGroup();
        for (int i=0; i<allSeries.size(); i++) {
            Series series = allSeries.get(i);

            int yOffset = textHeight * (i+1) + TEXT_SEPERATION * i;

            String maxText = String.format("max: %.3f",series.maxValue());
            int maxPosY = CHART_MARGIN + yOffset;
            int maxPosX = CHART_MARGIN;
            maxGroup.addLabel(new Label(maxText,series.getColor(),maxPosX,maxPosY));

            String minText = String.format("min: %.3f",series.minValue());
            int minPosY = getHeight() - CHART_MARGIN - textHeight * allSeries.size() + yOffset;
            int minPosX = CHART_MARGIN;
            minGroup.addLabel(new Label(minText,series.getColor(),minPosX,minPosY));
        }
        
        maxGroup.draw(g, BG_COLOR);
        minGroup.draw(g, BG_COLOR);
    }
    
    private void drawSelectionOverlay(Graphics g) {
        int CHART_MARGIN = 3;// margin around chart to keep out of
        int TEXT_SEPERATION = 0;// px between text
        Color BG_COLOR = new Color(0, 0, 0, 50);
        
        FontMetrics fm = g.getFontMetrics();
        int textHeight = fm.getHeight();
        int yOffset = CHART_MARGIN + textHeight;
        
        LabelGroup group = new LabelGroup();
        int deltaSamps = selectionSamp2 - selectionSamp1;
        double deltaTime = deltaSamps * dt;
        String dtText = String.format("delta time = %.3fs", deltaTime);
        group.addLabel(new Label(dtText, Color.LIGHT_GRAY, getWidth() - 150, yOffset));
        
        group.draw(g, BG_COLOR);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        drawVisibleSeries(g, sampRange);
        
        switch (dragState) {
            case DRAG_STATE_IDLE:
            case DRAG_STATE_PRESSED:
                break;
            case DRAG_STATE_DRAGGING:
            case DRAG_STATE_COMPLETE:
                drawSelection(g);
                drawSelectionOverlay(g);
                break;
            default:
                break;
        }
        
        drawMinMaxOverlay(g);

        if (showSelectedSample) {
            drawSelectedSample(g);
        }
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        boolean repaintRequired = false;
        
        if (e.getButton() == MouseEvent.BUTTON1) {// Left click
            // See if selection is new, if so update and redraw
            int newSelectedSampleIdx = getNearestSampleIdx(e.getX());
            boolean isNewSelection = newSelectedSampleIdx != selectedSampleIdx;
            if ( ! showSelectedSample || isNewSelection) {
                selectedSampleIdx = getNearestSampleIdx(e.getX());
                showSelectedSample = true;
                dragState = DRAG_STATE_IDLE;
                repaintRequired = true;
            }
            
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
            showSelectedSample = false;
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
        mouseX = e.getX();
        mouseY = e.getY();
        repaint();
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        mouseX = e.getX();
        mouseY = e.getY();
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        zoom(e.getWheelRotation() < 0, e.getX(), 0.2);
    }
    
    public static void main(String[] args) {
        FlatDarkLaf.install();
        
        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                JFrame frame = new JFrame("JLogChart Standalone");
                frame.setLayout(new BorderLayout());
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                
                // Create and show the JLogChart GUI
                JLogChart jlc = new JLogChart();
                jlc.setPreferredSize(new Dimension(600, 400));
                jlc.setSingleY_Scale(true);
                jlc.setSampleRate(100.0);
                frame.add(jlc, BorderLayout.CENTER);
                
                int N_CYCLES = 3;
                int N_SAMPS = 1000;
                double radPerSamp = Math.PI * 2 * N_CYCLES / N_SAMPS;
                List<Double> sinData = new ArrayList();
                List<Double> cosData = new ArrayList();
                List<Double> negSinData = new ArrayList();
                for (int i=0; i<N_SAMPS; i++) {
                    sinData.add(Math.sin(i * radPerSamp));
                    cosData.add(Math.cos(i * radPerSamp) * 2.0);
                    negSinData.add(Math.sin(i * radPerSamp) - 2.0);
                }
                jlc.addSeries("sin", sinData);
                jlc.addSeries("cos", cosData);
                jlc.addSeries("sin - 2", negSinData);
                
                // Show the GUI
                frame.pack();
                frame.setVisible(true);
            }
        });
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
    public void seriesColorChanged(String seriesName, Color oldColor, Color newColor) {
        repaint();
    }

    @Override
    public void seriesDataChanged(String seriesName) {
        // Recompute view bounds, y-scale, etc. then repaint the chart
        updateAfterDataChange();
    }

    @Override
    public void adjustmentValueChanged(AdjustmentEvent e) {
        // Avoid cyclical events if updating the view bounds programmatically
        if (ignoreNextScrollEvent) {
            ignoreNextScrollEvent = false;
            return;
        }
        
        int newLeftViewSamp = e.getValue();
        int newRightViewSamp = newLeftViewSamp + visibleSamps();
        /**
         * The updateFromScollEvent is set to true during view bounds update
         * because it would induce a recursive event chain.
         */
        if (newLeftViewSamp < newRightViewSamp) {
            setViewBounds(newLeftViewSamp, newRightViewSamp, true);
        }
    }
    
    @Override
    public Image getMiniMapImage() {
        /**
         * TODO I can improve performance by only drawing this image once when
         * the series data changes or visibility is modified.
         */
        BoundedRangeModel fullView = new DefaultBoundedRangeModel();
        fullView.setMinimum(sampRange.getMinimum());
        fullView.setMaximum(sampRange.getMaximum());
        fullView.setExtent(sampRange.getMaximum());
        
        // Paint an image for the full chart series
        BufferedImage mmImg = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics g = mmImg.getGraphics();
        drawVisibleSeries(g, fullView);
        
        return mmImg;
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        scrollbarPanel = new javax.swing.JPanel();
        miniMapScrollbar = new com.hankedan.jlogchart.MiniMapScrollbar();
        scrollbar = new javax.swing.JScrollBar();

        setLayout(new java.awt.BorderLayout());

        scrollbarPanel.setLayout(new java.awt.BorderLayout());

        miniMapScrollbar.setOrientation(javax.swing.JScrollBar.HORIZONTAL);
        scrollbarPanel.add(miniMapScrollbar, java.awt.BorderLayout.CENTER);

        scrollbar.setOrientation(javax.swing.JScrollBar.HORIZONTAL);
        scrollbarPanel.add(scrollbar, java.awt.BorderLayout.SOUTH);

        add(scrollbarPanel, java.awt.BorderLayout.SOUTH);
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private com.hankedan.jlogchart.MiniMapScrollbar miniMapScrollbar;
    private javax.swing.JScrollBar scrollbar;
    private javax.swing.JPanel scrollbarPanel;
    // End of variables declaration//GEN-END:variables
}
