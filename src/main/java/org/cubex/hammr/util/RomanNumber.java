package org.cubex.hammr.util;

public final class RomanNumber {

    private static final String[] ROMAN = {
            "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X",
            "XI", "XII", "XIII", "XIV", "XV", "XVI"
    };

    public static String toRoman(int number) {
        if (number < 1 || number > ROMAN.length) {
            return String.valueOf(number);
        }
        return ROMAN[number - 1];
    }

    private RomanNumber() {}
}
