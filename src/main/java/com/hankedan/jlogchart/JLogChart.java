/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.hankedan.jlogchart;

import com.formdev.flatlaf.FlatDarkLaf;
import com.hankedan.jlogchart.MiniMapScrollbar.MiniMapable;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
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
public class JLogChart extends javax.swing.JPanel implements 
        FixedRateSeries.FixedRateSeriesChangeListener, Series.SeriesChangeListener {
    private final Logger logger = Logger.getLogger(JLogChart.class.getName());

    private final JLogChartView view = new JLogChartView();

    // Delta time in seconds between time samples
    private double dt = 0.0;

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
    
    private final BoundedRangeModel xRange;

    // Map of all added chart series data vectors
    private final List<FixedRateSeries> allSeries = new ArrayList<>();

    public JLogChart() {
        initComponents();
        
        // only allow horizontal zooming with the mouse wheel
        view.setWheelZoomEnables(true, false);
        
        add(view, BorderLayout.CENTER);
        xRange = view.getX_RangeModel();
        
        scrollbarPanel.setBackground(new Color(0,0,0,0));
        scrollbar.setModel(xRange);
        scrollbar.addAdjustmentListener(view);
        scrollbar.setVisible(false);
        miniMapScrollbar.setModel(xRange);
        miniMapScrollbar.addAdjustmentListener(view);
        miniMapScrollbar.setMiniMapable(view);
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
        FixedRateSeries series = getSeriesByName(name);
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
    
    public FixedRateSeries addSeries(String name, List<Double> data) {
        FixedRateSeries series = getSeriesByName(name);
        
        if (series != null) {
            logger.log(Level.WARNING,
                    "Duplicate add of series data with name {0}. Ignoring.",
                    new Object[]{name});
        } else {
            Color color = Series.getDefaultColor(allSeries.size());
            series = new FixedRateSeries(name, color, data);
            allSeries.add(series);
            minValueY = Double.min(minValueY, series.minValue());
            maxValueY = Double.max(maxValueY, series.maxValue());
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

    public void removeSeries(String name) {
        int idxToRemove = -1;
        for (int i=0; i<allSeries.size(); i++) {
            FixedRateSeries series = allSeries.get(i);
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
    
    /**
     * @return
     * An immutable list of all the FixedRateSeries that are added to the chart
     */
    public List<FixedRateSeries> getAllSeries() {
        return Collections.unmodifiableList(allSeries);
    }
    
    public FixedRateSeries getSeriesByName(String name) {
        for (FixedRateSeries series : allSeries) {
            if (series.name.compareTo(name) == 0) {
                return series;
            }
        }
        return null;
    }
    
    /**
     * Getter for the JLogChart's ChartView component
     * @return 
     */
    public ChartView getChartView() {
        return view;
    }
    
    private void updateAfterDataChange() {
        int newMinX = Integer.MAX_VALUE;
        int newMaxX = Integer.MIN_VALUE;
        double newMinValueY = Double.POSITIVE_INFINITY;
        double newMaxValueY = Double.NEGATIVE_INFINITY;
        for (FixedRateSeries series : allSeries) {
            newMinX = Integer.min(newMinX, series.getOffset());
            newMaxX = Integer.max(newMaxX, series.getOffset() + series.getData().size());
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
            if (view.leftViewedSamp() >= newMaxX) {
                newLeftViewedSamp = newMinX;
            } else if (view.leftViewedSamp() <= newMinX) {
                newLeftViewedSamp = newMinX;
            }
            if (view.rightViewedSamp() >= newMaxX) {
                newRightViewedSamp = newMaxX;
            } else if (view.rightViewedSamp() <= newMinX) {
                newRightViewedSamp = newMaxX;
            }
        }

        xRange.setMinimum(newMinX);
        xRange.setMaximum(newMaxX);
        if (newLeftViewedSamp < newRightViewedSamp) {
            view.setHorzViewBounds(newLeftViewedSamp, newRightViewedSamp);
        }
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
                jlc.addSeries("sin", sinData).setBolded(true);
                jlc.addSeries("cos", cosData).setOffset(500);
                jlc.addSeries("sin - 2", negSinData);
                jlc.getChartView().fitViewWidthToData();
                
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
        view.updateMiniMapImage();
    }

    @Override
    public void seriesBoldnessChanged(String seriesName, boolean bold) {
        repaint();
    }

    @Override
    public void seriesColorChanged(String seriesName, Color oldColor, Color newColor) {
        repaint();
        view.updateMiniMapImage();
    }

    @Override
    public void seriesDataChanged(String seriesName) {
        // Recompute view bounds, y-scale, etc. then repaint the chart
        updateAfterDataChange();
        view.updateMiniMapImage();
    }

    @Override
    public void seriesOffsetChanged(String seriesName, int oldOffset, int newOffset) {
        // Recompute view bounds, scroll bounds, etc with new offset values
        updateAfterDataChange();
        repaint();
        view.updateMiniMapImage();
    }

    private class JLogChartView extends ChartView implements ChartViewListener,
            MiniMapable {
        // The user can select and place a vertical bar on a sample
        private int selectedAbsSample = -1;
        
        private boolean selectionValid = false;
        private int selectionAbsSamp1, selectionAbsSamp2;
    
        // A "full view" image of all the series used for minimap scrollbar
        private BufferedImage miniMapImage = null;
        
        public JLogChartView() {
            addChartViewListener(this);
        }
        
        private void drawSelection(Graphics g) {
            double pxPerSamp = getPxPerSample();
            int offset1 = selectionAbsSamp1 - leftViewedSamp();
            int offset2 = selectionAbsSamp2 - leftViewedSamp();
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

        private void drawSeries(Graphics g, FixedRateSeries series, BoundedRangeModel brm) {
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
            int leftViewedSamp = lowerViewedSamp(brm);
            int rightViewedSamp = upperViewedSamp(brm);
            double absIdx = leftViewedSamp;
            int relIdx = series.getRelSampleIdx((int)absIdx);
            while (absIdx<=rightViewedSamp && relIdx<series.getData().size()) {
                // Don't draw anything below the FixedRateSeries's absolute range
                if (relIdx >= 0) {
                    int currX = (int)((absIdx - leftViewedSamp) * pxPerSamp);
                    int currY = getHeight() - (int)((series.getData().get(relIdx) - minY) * pxPerVal);

                    if (prevX >= 0 && prevY >= 0) {
                        g.drawLine(prevX, prevY, currX, currY);
                    }

                    prevX = currX;
                    prevY = currY;
                }

                absIdx += sampStride;
                relIdx = series.getRelSampleIdx((int)absIdx);
            }
        }

        private void drawVisibleSeries(Graphics g, BoundedRangeModel brm) {
            for (FixedRateSeries series : allSeries) {
                if (series.getVisible()) {
                    drawSeries(g, series, brm);
                }
            }
        }

        private void drawSelectedSample(Graphics g) {
            if (selectedAbsSample < leftViewedSamp()) {
                return;
            } else if (selectedAbsSample > rightViewedSamp()) {
                return;
            }

            int sampOffset = selectedAbsSample - leftViewedSamp();
            int sampX = (int)(sampOffset * getPxPerSample());
            g.setColor(Color.CYAN);
            g.drawLine(sampX, 0, sampX, getHeight());
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
                FixedRateSeries series = allSeries.get(i);

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
            int deltaSamps = selectionAbsSamp2 - selectionAbsSamp1;
            double deltaTime = deltaSamps * dt;
            String dtText = String.format("delta time = %.3fs", deltaTime);
            group.addLabel(new Label(dtText, Color.LIGHT_GRAY, getWidth() - 150, yOffset));
            
            group.draw(g, BG_COLOR);
        }
        
        private void updateMiniMapImage() {
            // We can't generate images if view has no width or height
            if (getWidth() == 0 || getHeight() == 0) {
                miniMapImage = null;
                return;
            }
            
            BoundedRangeModel fullView = new DefaultBoundedRangeModel();
            fullView.setMinimum(xRange.getMinimum());
            fullView.setMaximum(xRange.getMaximum());
            fullView.setExtent(xRange.getMaximum());

            // Paint an image for the full chart series
            miniMapImage = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
            Graphics g = miniMapImage.getGraphics();
            view.drawVisibleSeries(g, fullView);
        }
        
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            
            // Draw the chart view
            drawVisibleSeries(g, xRange);
            if (selectedAbsSample != -1) {
                drawSelectedSample(g);
            }

            // Draw chart overlays
            if (selectionValid) {
                drawSelection(g);
                drawSelectionOverlay(g);
            }
            drawMinMaxOverlay(g);
        }

        @Override
        public void onSampleClicked(int absSample, boolean isPanClick) {
            if ( ! isPanClick) {
                selectedAbsSample = absSample;
                repaint();
            }
        }

        @Override
        public void onLeftClicked(MouseEvent e) {
        }

        @Override
        public void onMiddleClicked(MouseEvent e) {
        }

        @Override
        public void onRightClicked(MouseEvent e) {
            selectedAbsSample = -1;
            selectionValid = false;
            repaint();
        }

        @Override
        public void onDragStarted(int startAbsSample) {
            selectionAbsSamp1 = startAbsSample;
            repaint();
        }

        @Override
        public void onDragStarted(MouseEvent e) {
        }

        @Override
        public void onDragging(int startAbsSample, int currAbsSample) {
            selectionAbsSamp1 = startAbsSample;
            selectionAbsSamp2 = currAbsSample;
            selectionValid = true;
            repaint();
        }

        @Override
        public void onDragging(MouseEvent e1, MouseEvent e2) {
        }

        @Override
        public void onDragComplete(int startAbsSample, int stopAbsSample) {
            selectionAbsSamp1 = startAbsSample;
            selectionAbsSamp2 = stopAbsSample;
            selectionValid = true;
            repaint();
        }

        @Override
        public void onDragComplete(MouseEvent e1, MouseEvent e2) {
        }
    
        @Override
        public BufferedImage getMiniMapImage() {
            if (miniMapImage == null) {
                updateMiniMapImage();
            }
            return miniMapImage;
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
