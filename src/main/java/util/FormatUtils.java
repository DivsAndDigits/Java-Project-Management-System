package util;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * Utility helper methods for formatting currencies, numbers, and display strings.
 */
public final class FormatUtils {

    private static final NumberFormat CURRENCY_FORMAT = NumberFormat.getCurrencyInstance(Locale.US);

    private FormatUtils() {}

    public static String formatCurrency(double amount) {
        return CURRENCY_FORMAT.format(amount);
    }

    public static String formatPercentage(double value) {
        return String.format(Locale.US, "%.1f%%", value);
    }

    public static String formatCompactNumber(long count) {
        if (count >= 1_000_000) {
            return String.format(Locale.US, "%.1fM", count / 1_000_000.0);
        } else if (count >= 1_000) {
            return String.format(Locale.US, "%.1fk", count / 1_000.0);
        }
        return String.valueOf(count);
    }
}
