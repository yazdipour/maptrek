package mobi.maptrek.util;

public class MathUtils {
    private static final double DOUBLE_EPSILON = 1.0E-6d;
    private static final float FLOAT_EPSILON = 1.0E-5f;

    public static boolean equals(float a, float b) {
        return a == b || Math.abs(a - b) < FLOAT_EPSILON;
    }

    public static boolean equals(double a, double b) {
        return a == b || Math.abs(a - b) < 1.0E-6d;
    }
}
