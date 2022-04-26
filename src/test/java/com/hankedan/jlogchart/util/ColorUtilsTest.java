/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.hankedan.jlogchart.util;

import com.hankedan.jlogchart.util.ColorUtils;
import java.awt.Color;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author daniel
 */
public class ColorUtilsTest {
    
    public ColorUtilsTest() {
    }
    
    @BeforeAll
    public static void setUpClass() {
    }
    
    @AfterAll
    public static void tearDownClass() {
    }
    
    @BeforeEach
    public void setUp() {
    }
    
    @AfterEach
    public void tearDown() {
    }

    /**
     * Test of toHexString method, of class ColorUtils.
     */
    @Test
    public void testToHexString() {
        System.out.println("toHexString");
        assertEquals("#ff0000", ColorUtils.toHexString(Color.RED));
        assertEquals("#00ff00", ColorUtils.toHexString(Color.GREEN));
        assertEquals("#0000ff", ColorUtils.toHexString(Color.BLUE));
    }

    /**
     * Test of fromHexString method, of class ColorUtils.
     */
    @Test
    public void testFromHexString() {
        System.out.println("fromHexString");
        assertEquals(Color.RED,   ColorUtils.fromHexString("#ff0000"));
        assertEquals(Color.GREEN, ColorUtils.fromHexString("#00ff00"));
        assertEquals(Color.BLUE,  ColorUtils.fromHexString("#0000ff"));
        
        assertEquals(Color.RED,   ColorUtils.fromHexString("ff0000"));
        assertEquals(Color.GREEN, ColorUtils.fromHexString("00ff00"));
        assertEquals(Color.BLUE,  ColorUtils.fromHexString("0000ff"));
    }
    
}
