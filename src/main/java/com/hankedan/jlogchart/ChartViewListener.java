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
    public void onMiddleClicked(MouseEvent e);
    public void onRightClicked(MouseEvent e);
    public void onDragStarted(int startAbsSample);
    public void onDragStarted(MouseEvent e);
    public void onDragging(int startAbsSample, int currAbsSample);
    public void onDragging(MouseEvent e1, MouseEvent e2);
    public void onDragComplete(int startAbsSample, int stopAbsSample);
    public void onDragComplete(MouseEvent e1, MouseEvent e2);
}
