/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.hankedan.jlogchart;

import java.awt.event.MouseEvent;

/**
 *
 * @author daniel
 */
public interface ChartViewListener {
    public void onSampleClicked(int absSample, boolean isPanClick);
    public void onLeftClicked(MouseEvent e);
    public void onRightClicked(MouseEvent e);
    public void onDragStarted(int startAbsSample);
    public void onDragging(int startAbsSample, int currAbsSample);
    public void onDragComplete(int startAbsSample, int stopAbsSample);
}
