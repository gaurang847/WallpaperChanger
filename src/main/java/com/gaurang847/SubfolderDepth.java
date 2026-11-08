package com.gaurang847;

/**
 * Enum representing supported subfolder search depths for Google Drive scanning.
 */
public enum SubfolderDepth {
    LEVEL_1(1, "1 level (Direct subfolders only)"),
    LEVEL_2(2, "2 levels"),
    LEVEL_3(3, "3 levels (Recommended)"),
    LEVEL_5(5, "5 levels (Deep search)"),
    LEVEL_10(10, "10 levels (Max / All)");

    private final int depth;
    private final String label;

    SubfolderDepth(int depth, String label) {
        this.depth = depth;
        this.label = label;
    }

    public int getDepth() {
        return depth;
    }

    public String getLabel() {
        return label;
    }

    public static SubfolderDepth fromDepth(int depth) {
        for (var option : values()) {
            if (option.depth == depth) {
                return option;
            }
        }
        return LEVEL_3;
    }

    @Override
    public String toString() {
        return label;
    }
}
