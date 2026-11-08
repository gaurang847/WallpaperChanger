package com.gaurang847;

import org.junit.Test;

import static org.junit.Assert.*;

public class SubfolderDepthTest {

    @Test
    public void testEnumDepthMapping() {
        assertEquals(1, SubfolderDepth.LEVEL_1.getDepth());
        assertEquals(2, SubfolderDepth.LEVEL_2.getDepth());
        assertEquals(3, SubfolderDepth.LEVEL_3.getDepth());
        assertEquals(5, SubfolderDepth.LEVEL_5.getDepth());
        assertEquals(10, SubfolderDepth.LEVEL_10.getDepth());
    }

    @Test
    public void testFromDepthValidValues() {
        assertEquals(SubfolderDepth.LEVEL_1, SubfolderDepth.fromDepth(1));
        assertEquals(SubfolderDepth.LEVEL_2, SubfolderDepth.fromDepth(2));
        assertEquals(SubfolderDepth.LEVEL_3, SubfolderDepth.fromDepth(3));
        assertEquals(SubfolderDepth.LEVEL_5, SubfolderDepth.fromDepth(5));
        assertEquals(SubfolderDepth.LEVEL_10, SubfolderDepth.fromDepth(10));
    }

    @Test
    public void testFromDepthFallbackToDefault() {
        assertEquals(SubfolderDepth.LEVEL_3, SubfolderDepth.fromDepth(0));
        assertEquals(SubfolderDepth.LEVEL_3, SubfolderDepth.fromDepth(-5));
        assertEquals(SubfolderDepth.LEVEL_3, SubfolderDepth.fromDepth(99));
    }

    @Test
    public void testToStringReturnsHumanReadableLabel() {
        assertEquals("1 level (Direct subfolders only)", SubfolderDepth.LEVEL_1.toString());
        assertEquals("3 levels (Recommended)", SubfolderDepth.LEVEL_3.toString());
    }
}
