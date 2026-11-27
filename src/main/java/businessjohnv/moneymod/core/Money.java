package businessjohnv.moneymod.core;

import java.util.OptionalLong;
import java.util.regex.Pattern;

public final class Money {
    private static final Pattern AMOUNT = Pattern.compile("(\\d{1,15})(?:[.,](\\d{1,2}))?");

    private Money() {}

    public static OptionalLong parse(String input) {
        if (input == null) return OptionalLong.empty();

        var m = AMOUNT.matcher(input.trim());
        if (!m.matches()) return OptionalLong.empty();

        long whole = Long.parseLong(m.group(1));
        String fracText = m.group(2) == null ? "" : m.group(2);
        long frac = fracText.isEmpty() ? 0 : Long.parseLong(fracText);
        if (fracText.length() == 1) frac *= 10;

        return OptionalLong.of(whole * 100 + frac);
    }

    public static String format(long pennies) {
        String sign = pennies >= 0 ? "" : "-";
        long abs = Math.abs(pennies);
        long frac = abs % 100;

        return sign = (abs / 100) + "." + (frac < 10 ? "0" : "") + frac;
    }
}
