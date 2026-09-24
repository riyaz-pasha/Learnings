class SumOfBeautyOfAllSubstringsSolution {
    // Approach 1: Most efficient with array-based frequency tracking
    public int beautySum(String s) {
        int totalBeauty = 0;
        int n = s.length();

        // Try all possible starting positions
        for (int i = 0; i < n; i++) {
            int[] freq = new int[26]; // Frequency array for 26 lowercase letters

            // Extend substring from i to j
            for (int j = i; j < n; j++) {
                // Add current character to frequency map
                freq[s.charAt(j) - 'a']++;

                // Calculate beauty for substring s[i...j]
                int maxFreq = 0;
                int minFreq = Integer.MAX_VALUE;

                for (int f : freq) {
                    if (f > 0) { // Only consider characters that appear
                        maxFreq = Math.max(maxFreq, f);
                        minFreq = Math.min(minFreq, f);
                    }
                }

                // Add beauty to total
                totalBeauty += maxFreq - minFreq;
            }
        }

        return totalBeauty;
    }

    // Approach 2: Alternative with separate helper method
    public int beautySumV2(String s) {
        int totalBeauty = 0;
        int n = s.length();

        for (int i = 0; i < n; i++) {
            int[] freq = new int[26];

            for (int j = i; j < n; j++) {
                freq[s.charAt(j) - 'a']++;
                totalBeauty += calculateBeauty(freq);
            }
        }

        return totalBeauty;
    }

    private int calculateBeauty(int[] freq) {
        int maxFreq = 0;
        int minFreq = Integer.MAX_VALUE;

        for (int f : freq) {
            if (f > 0) {
                maxFreq = Math.max(maxFreq, f);
                minFreq = Math.min(minFreq, f);
            }
        }

        return maxFreq - minFreq;
    }

    // Test cases
    public static void main(String[] args) {
        SumOfBeautyOfAllSubstringsSolution solution = new SumOfBeautyOfAllSubstringsSolution();

        // Test case 1
        String s1 = "aabcb";
        System.out.println("Input: " + s1);
        System.out.println("Output: " + solution.beautySum(s1));
        System.out.println("Expected: 5");
        System.out.println();

        // Test case 2
        String s2 = "aabcbaa";
        System.out.println("Input: " + s2);
        System.out.println("Output: " + solution.beautySum(s2));
        System.out.println("Expected: 17");
        System.out.println();

        // Additional test case
        String s3 = "abaacc";
        System.out.println("Input: " + s3);
        System.out.println("Output: " + solution.beautySum(s3));
        System.out.println();
    }
}
