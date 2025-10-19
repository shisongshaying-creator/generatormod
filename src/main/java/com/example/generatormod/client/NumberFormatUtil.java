package com.example.generatormod.client;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.math.RoundingMode;
import java.util.Locale;

public final class NumberFormatUtil {
    private static final long[] THRESHOLDS = {
            1_000_000_000_000L,
            1_000_000_000L,
            1_000_000L,
            1_000L
    };
    private static final String[] SUFFIXES = {"T", "B", "M", "K"};
    private static final ThreadLocal<DecimalFormat> NO_DECIMAL_FORMAT = ThreadLocal.withInitial(() -> {
        DecimalFormat format = new DecimalFormat("0", DecimalFormatSymbols.getInstance(Locale.ROOT));
        format.setRoundingMode(RoundingMode.DOWN);
        return format;
    });
    private static final ThreadLocal<DecimalFormat> ONE_DECIMAL_FORMAT = ThreadLocal.withInitial(() -> {
        DecimalFormat format = new DecimalFormat("0.#", DecimalFormatSymbols.getInstance(Locale.ROOT));
        format.setRoundingMode(RoundingMode.DOWN);
        return format;
    });

    private NumberFormatUtil() {
    }

    public static String formatCount(long value) {
        long absValue = Math.abs(value);
        for (int i = 0; i < THRESHOLDS.length; i++) {
            long threshold = THRESHOLDS[i];
            if (absValue >= threshold) {
                double scaled = (double) value / (double) threshold;
                return formatScaled(scaled) + SUFFIXES[i];
            }
        }
        return String.format(Locale.ROOT, "%,d", value);
    }

    private static String formatScaled(double value) {
        double absValue = Math.abs(value);
        DecimalFormat format = absValue >= 100.0 ? NO_DECIMAL_FORMAT.get() : ONE_DECIMAL_FORMAT.get();
        return format.format(value);
    }
}
