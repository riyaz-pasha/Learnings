class KMP {

    public int search(String text, String pattern) {
        int textLength = text.length();
        int patternLength = pattern.length();

        int[] lps = this.buildLPS(pattern);
        int textIndex = 0, patternIndex = 0;

        while (textIndex < textLength) {

            if (text.charAt(textIndex) == pattern.charAt(patternIndex)) {
                textIndex++;
                patternIndex++;
            }

            if (patternIndex == patternLength) {
                // Found a match!
                return textIndex - patternIndex;
            } else if (textIndex < textLength && text.charAt(textIndex) != pattern.charAt(patternIndex)) {
                // Mismatch after patternIndex matches
                if (patternIndex != 0) {
                    // This is the core of KMP. Shift the pattern using the LPS array.
                    patternIndex = lps[patternIndex - 1];
                } else {
                    // No prefix to fall back on, just move to the next character in the text.
                    textIndex++;
                }
            }
        }

        return -1;
    }

    private int[] buildLPS(String pattern) {
        int m = pattern.length();
        int[] lps = new int[m];

        int length = 0; // Length of the previous longest prefix suffix
        int index = 1;
        lps[0] = 0; // The first element is always 0. Bacause no prefix or suffix to match

        while (index < m) {
            if (pattern.charAt(index) == pattern.charAt(length)) {
                length++;
                lps[index] = length;
                index++;
            } else {
                if (length != 0) {
                    // This is the "look-back" step
                    length = lps[length - 1];
                } else {
                    lps[index] = 0;
                    index++;
                }
            }
        }

        return lps;
    }

}
