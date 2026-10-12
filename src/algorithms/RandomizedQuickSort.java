package algorithms;

import models.Patient;
import java.util.List;
import java.util.Random;

/**
 * Randomised Algorithm from Module 6.
 * Randomized three-way quicksort has expected O(n log n) time and O(n^2) worst case.
 * Three-way partitioning also handles many equal durations without repeatedly recursing
 * over one equal item at a time; randomization does not remove the worst case.
 */
public class RandomizedQuickSort {

    private static final Random RANDOM = new Random();

    /**
     * Sorts patients by Surgery Duration in DESCENDING order (Longest Processing Time first).
     * Used by the Approximation Algorithm.
     */
    public static void sortDescendingByDuration(List<Patient> patients) {
        if (patients == null || patients.size() <= 1) return;
        quickSort(patients, 0, patients.size() - 1);
    }

    private static void quickSort(List<Patient> list, int low, int high) {
        while (low < high) {
            int pivotIndex = low + RANDOM.nextInt(high - low + 1);
            int pivot = list.get(pivotIndex).getSurgeryDuration();
            int greater = low, current = low, lesser = high;
            while (current <= lesser) {
                int value = list.get(current).getSurgeryDuration();
                if (value > pivot) swap(list, greater++, current++);
                else if (value < pivot) swap(list, current, lesser--);
                else current++;
            }
            // Recurse into the smaller partition first and iterate over the larger one
            // to keep stack use bounded on unusually unbalanced pivots.
            if (greater - low < high - lesser) {
                quickSort(list, low, greater - 1);
                low = lesser + 1;
            } else {
                quickSort(list, lesser + 1, high);
                high = greater - 1;
            }
        }
    }

    private static void swap(List<Patient> list, int i, int j) {
        Patient temp = list.get(i);
        list.set(i, list.get(j));
        list.set(j, temp);
    }
}
