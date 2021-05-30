/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.hankedan.jlogchart;

import com.hankedan.jlogchart.util.VectorUtils;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import org.apache.commons.math3.exception.OutOfRangeException;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;
import org.w3c.dom.ranges.RangeException;

/**
 *
 * @author daniel
 */
public class Series<T> {
    public interface SeriesChangeListener {
        public void seriesSubTitleChanged(String seriesName, String oldSubTitle, String newSubTitle);
        public void seriesVisibilityChanged(String seriesName, boolean visible);
        public void seriesBoldnessChanged(String seriesName, boolean bold);
        public void seriesColorChanged(String seriesName, Color oldColor, Color newColor);
        public void seriesDataChanged(String seriesName);
        public void seriesOffsetChanged(String seriesName, int oldOffset, int newOffset);
    }
    
    public static final int NORMAL_THICKNESS = 1;
    public static final int BOLD_THICKNESS = 3;

    public static final Color[] SERIES_COLOR_PALETTE = {
        Color.RED,
        Color.ORANGE,
        Color.YELLOW,
        Color.GREEN,
        Color.BLUE,
        Color.MAGENTA,
    };
    
    private static final Logger logger = Logger.getLogger(Series.class.getName());
    
    public final String name;
    private String subTitle;
    private Color color = Color.WHITE;
    private boolean visible = true;
    private boolean bolded = false;
    private List<T> data;
    private Object minValue;
    private boolean minValueSet = false;
    private Object maxValue;
    private boolean maxValueSet = false;
    // Allows series samples to be shifted in the x-axis
    private int offset = 0;
    private final List<SeriesChangeListener> listeners = new ArrayList();

    public Series(String name, Color color, List<T> data) {
        this(name,"",color,data);
    }

    public Series(String name, String subTitle, Color color, List<T> data) {
        this.name = name;
        this.subTitle = subTitle;
        this.color = color;
        setData(data);
    }

    public Series setSubTitle(String newSubTitle) {
        String oldSubTitle = this.subTitle;
        this.subTitle = newSubTitle;
        for (SeriesChangeListener l : listeners) {
            l.seriesSubTitleChanged(name, oldSubTitle, newSubTitle);
        }
        return this;
    }
    
    public String getSubTitle() {
        return this.subTitle;
    }
    
    public Series setColor(Color newColor) {
        Color oldColor = this.color;
        this.color = newColor;
        for (SeriesChangeListener l : listeners) {
            l.seriesColorChanged(name, oldColor, newColor);
        }
        return this;
    }

    public Color getColor() {
        return this.color;
    }

    public Series setVisible(boolean visible) {
        this.visible = visible;
        for (SeriesChangeListener l : listeners) {
            l.seriesVisibilityChanged(name, visible);
        }
        return this;
    }

    public boolean getVisible() {
        return this.visible;
    }

    public Series setBolded(boolean bolded) {
        this.bolded = bolded;
        for (SeriesChangeListener l : listeners) {
            l.seriesBoldnessChanged(name, bolded);
        }
        return this;
    }

    public boolean isBolded() {
        return this.bolded;
    }

    public Series setData(List<T> newData) {
        minValueSet = false;
        maxValueSet = false;
        for (T val : newData) {
            if (minValueSet) {
                if (val instanceof Double) {
                    minValue = Double.min((double)minValue, (double)val);
                } else if (val instanceof Vector2D) {
                    minValue = VectorUtils.min((Vector2D)minValue, (Vector2D)val);
                } else {
                    throw new UnsupportedOperationException("type not supported: " + val.getClass().getName());
                }
            } else {
                minValue = val;
                minValueSet = true;
            }
            if (maxValueSet) {
                if (val instanceof Double) {
                    maxValue = Double.max((double)maxValue, (double)val);
                } else if (val instanceof Vector2D) {
                    maxValue = VectorUtils.max((Vector2D)maxValue, (Vector2D)val);
                } else {
                    throw new UnsupportedOperationException("type not supported: " + val.getClass().getName());
                }
            } else {
                maxValue = val;
                maxValueSet = true;
            }
        }
        this.data = newData;
        for (SeriesChangeListener l : listeners) {
            l.seriesDataChanged(name);
        }
        return this;
    }

    public List<T> getData() {
        return Collections.unmodifiableList(data);
    }

    public void addSeriesListener(SeriesChangeListener l) {
        if (l != null) {
            listeners.add(l);
        }
    }

    public void removeSeriesListener(SeriesChangeListener l) {
        listeners.remove(l);
    }
    
    public static Color getDefaultColor(int idx) {
        int colorIdx = idx % SERIES_COLOR_PALETTE.length;
        return SERIES_COLOR_PALETTE[colorIdx];
    }

    public Series setOffset(int newOffset) {
        int oldOffset = this.offset;
        this.offset = newOffset;
        for (SeriesChangeListener l : listeners) {
            l.seriesOffsetChanged(name, oldOffset, newOffset);
        }
        return this;
    }
    
    public int getOffset() {
        return this.offset;
    }
    
    public int getRelSampleIdx(int absSampleIdx) {
        return absSampleIdx - this.offset;
    }

    /**
     * Getter for sample value, given an absolute sample index. This method
     * should be used to get sample values any time you have been given an
     * absolute sample index.
     * 
     * @param absSampleIdx
     * Can be any integer index value, even outside of the series's data range.
     * 
     * @return 
     * The sample's value. Double.NEGATIVE_INFINITY is returned when the
     * absolute index maps to a sample below index 0 in the series's data
     * vector; likewise, Double.POSITIVE_INFINITY is returned if the index goes
     * beyond the end of the data vector.
     */
    public T getAbsSampleValue(int absSampleIdx) {
        int relSampleIdx = getRelSampleIdx(absSampleIdx);
        if (relSampleIdx < 0) {
            throw new OutOfRangeException(relSampleIdx, 0, this.data.size());
        } else if (relSampleIdx >= this.data.size()) {
            throw new OutOfRangeException(relSampleIdx, 0, this.data.size());
        } else {
            return this.data.get(relSampleIdx);
        }
    }

    public T minValue() {
        return (T)minValue;
    }

    public T maxValue() {
        return (T)maxValue;
    }
    
    public int size() {
        if (data == null) {
            return 0;
        } else {
            return data.size();
        }
    }
}
