package algorithms;

import java.util.ArrayList;
import java.util.List;

/**
 * String Matching Algorithm from Module 2.
 * Knuth-Morris-Pratt (KMP) algorithm for finding substring matches in O(N + M) time.
 * Used for fast searching of Patient and Staff names.
 */
public class KMPAlgorithm {

    /**
     * Returns true if 'pattern' is found anywhere within 'text'.
     * Case-insensitive search.
     */
    public static boolean search(String text, String pattern) {
        if (pattern == null || pattern.isEmpty()) return true;
        if (text == null || text.isEmpty()) return false;

        text = text.toLowerCase();
        pattern = pattern.toLowerCase();

        int n = text.length();
        int m = pattern.length();
        int[] lps = computeLPSArray(pattern);

        int i = 0; // index for text
        int j = 0; // index for pattern

        while (i < n) {
            if (pattern.charAt(j) == text.charAt(i)) {
                j++;
                i++;
            }
            if (j == m) {
                return true; // Match found
                // j = lps[j - 1]; // Use this if we want to find multiple occurrences
            } else if (i < n && pattern.charAt(j) != text.charAt(i)) {
                if (j != 0) {
                    j = lps[j - 1];
                } else {
                    i = i + 1;
                }
            }
        }
        return false;
    }

    /**
     * Computes the Longest Prefix Suffix (LPS) array for the KMP algorithm.
     */
    private static int[] computeLPSArray(String pattern) {
        int m = pattern.length();
        int[] lps = new int[m];
        int len = 0;
        int i = 1;
        lps[0] = 0;

        while (i < m) {
            if (pattern.charAt(i) == pattern.charAt(len)) {
                len++;
                lps[i] = len;
                i++;
            } else {
                if (len != 0) {
                    len = lps[len - 1];
                } else {
                    lps[i] = 0;
                    i++;
                }
            }
        }
        return lps;
    }
}
