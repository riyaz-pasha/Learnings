package org.example;

public class Base62Encoder {

    private static final String BASE62 = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final char[] BASE62_CHARS = BASE62.toCharArray();
    private static final int BASE = 62;

    public static String encode(long value) {
        if (value == 0) {
            return "0";
        }
        StringBuilder sb = new StringBuilder();

        while (value > 0) {
            int reminder = (int) (value % BASE);
            sb.append((BASE62_CHARS[reminder]));
            value /= BASE;
        }

        return sb.reverse().toString();
    }

    public static long decode(String str) {
        long result = 0;
        for (char ch : str.toCharArray()) {
            result = result * BASE + BASE62.indexOf(ch);
        }
        return result;
    }
}
