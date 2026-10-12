package algorithms;

import models.OperatingRoom;
import models.Patient;
import models.Staff;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Time-dependent subset DP for one OR, including waiting cost and existing reservations. */
public class DPScheduler {
    /** Engineering threshold; validate against the reproducible benchmark suite before changing. */
    public static final int MAX_EXACT_DP_PATIENTS = 14;
    private static final int BASE_CASE_VALUE = 10_000;
    private static final int PRIORITY_BONUS = 1_000;
    private static final int WAITING_COST_PER_MINUTE = 5;

    public static class Job {
        public Patient patient;
        public int startTime;
        public int endTime;
        public int weight;

        public Job(Patient patient, int start, int end, int weight) {
            this.patient = patient;
            this.startTime = start;
            this.endTime = end;
            this.weight = weight;
        }
    }

    private static class State {
        final int score;
        final int previousMask;
        final int previousEnd;
        final Job job;
        State(int score, int previousMask, int previousEnd, Job job) {
            this.score = score;
            this.previousMask = previousMask;
            this.previousEnd = previousEnd;
            this.job = job;
        }
    }

    /**
     * Exact subset DP for small OR workloads. Each transition proposes the earliest
     * feasible start after the previous selected job and existing booked intervals.
     */
    public static List<Job> schedulePatients(List<Patient> patients, OperatingRoom or,
                                             List<Staff> staff, int earliestStart, int shiftEnd) {
        return schedulePatients(patients, or, staff, earliestStart, shiftEnd, earliestStart);
    }

    /** planningStart is an additional lower bound used when rebuilding after an emergency. */
    public static List<Job> schedulePatients(List<Patient> patients, OperatingRoom or,
                                             List<Staff> staff, int earliestStart, int shiftEnd,
                                             int planningStart) {
        List<Patient> eligible = new ArrayList<>();
        for (Patient patient : patients) {
            if (or.supportsSurgery(patient.getSurgeryType())) eligible.add(patient);
        }
        if (eligible.isEmpty()) return new ArrayList<>();
        if (eligible.size() > MAX_EXACT_DP_PATIENTS) {
            return scheduleLargeInstance(eligible, or, staff, earliestStart, shiftEnd, planningStart);
        }

        int n = eligible.size();
        int states = 1 << n;
        @SuppressWarnings("unchecked")
        Map<Integer, State>[] dp = new Map[states];
        for (int mask = 0; mask < states; mask++) dp[mask] = new HashMap<>();
        int initialStart = Math.max(earliestStart, planningStart);
        dp[0].put(initialStart, new State(0, -1, -1, null));
        State best = dp[0].get(initialStart);
        int bestMask = 0;
        int bestEnd = initialStart;

        for (int mask = 0; mask < states; mask++) {
            for (Map.Entry<Integer, State> entry : dp[mask].entrySet()) {
                int endTime = entry.getKey();
                State current = entry.getValue();
                if (current.score > best.score) {
                    best = current;
                    bestMask = mask;
                    bestEnd = endTime;
                }
                for (int i = 0; i < n; i++) {
                    if ((mask & (1 << i)) != 0) continue;
                    Patient patient = eligible.get(i);
                    int start = findEarliestStaffSlot(patient, or, staff,
                            Math.max(endTime, patient.getArrivalTime()), shiftEnd);
                    if (start < 0) continue;
                    int end = start + patient.getSurgeryDuration();
                    int waiting = start - patient.getArrivalTime();
                    int score = current.score + score(patient, waiting);
                    int nextMask = mask | (1 << i);
                    State old = dp[nextMask].get(end);
                    if (old == null || score > old.score) {
                        dp[nextMask].put(end, new State(score, mask, endTime,
                                new Job(patient, start, end, score(patient, waiting))));
                    }
                }
            }
        }

        // Find the best terminal state after all reachable masks have been evaluated.
        for (int mask = 0; mask < states; mask++) {
            for (Map.Entry<Integer, State> entry : dp[mask].entrySet()) {
                if (entry.getValue().score > best.score) {
                    best = entry.getValue(); bestMask = mask; bestEnd = entry.getKey();
                }
            }
        }
        List<Job> result = new ArrayList<>();
        int mask = bestMask;
        int end = bestEnd;
        while (mask != 0) {
            State state = dp[mask].get(end);
            result.add(state.job);
            mask = state.previousMask;
            end = state.previousEnd;
        }
        result.sort(Comparator.comparingInt(job -> job.startTime));
        return result;
    }

    private static int score(Patient patient, int waiting) {
        return BASE_CASE_VALUE + (4 - patient.getPriority().getLevel()) * PRIORITY_BONUS
                - waiting * WAITING_COST_PER_MINUTE;
    }

    public static boolean hasQualifiedSurgeon(List<Staff> staff, Patient patient, int start, int end) {
        for (Staff member : staff) {
            if (member.getRole() == Staff.Role.SURGEON
                    && member.canPerformSurgery(patient.getSurgeryType())
                    && member.isAvailableDuring(start, end)) return true;
        }
        return false;
    }

    private static List<Job> scheduleLargeInstance(List<Patient> patients, OperatingRoom or,
                                                   List<Staff> staff, int earliestStart, int shiftEnd,
                                                   int planningStart) {
        List<Job> jobs = new ArrayList<>();
        for (Patient patient : patients) {
            int start = findEarliestStaffSlot(patient, or, staff,
                    Math.max(Math.max(earliestStart, planningStart), patient.getArrivalTime()), shiftEnd);
            if (start >= 0) {
                jobs.add(new Job(patient, start, start + patient.getSurgeryDuration(),
                        score(patient, start - patient.getArrivalTime())));
            }
        }
        return scheduleMaxWeight(jobs);
    }

    private static int findEarliestStaffSlot(Patient patient, OperatingRoom or, List<Staff> staff,
                                             int earliest, int shiftEnd) {
        int request = Math.max(earliest, 0);
        while (request + patient.getSurgeryDuration() <= shiftEnd) {
            int start = or.findEarliestSlot(request, patient.getSurgeryDuration(), shiftEnd);
            if (start < 0) return -1;
            if (hasQualifiedSurgeon(staff, patient, start, start + patient.getSurgeryDuration())) return start;
            request = start + 1;
        }
        return -1;
    }

    /** Weighted interval DP retained as a directly testable primitive for fixed candidates. */
    public static List<Job> scheduleMaxWeight(List<Job> jobs) {
        if (jobs.isEmpty()) return new ArrayList<>();
        jobs.sort(Comparator.comparingInt(job -> job.endTime));
        int n = jobs.size();
        int[] best = new int[n];
        int[] previous = new int[n];
        for (int i = 0; i < n; i++) {
            previous[i] = -1;
            for (int j = i - 1; j >= 0; j--) {
                if (jobs.get(j).endTime <= jobs.get(i).startTime) { previous[i] = j; break; }
            }
            int include = jobs.get(i).weight + (previous[i] < 0 ? 0 : best[previous[i]]);
            best[i] = i == 0 ? include : Math.max(include, best[i - 1]);
        }
        List<Job> result = new ArrayList<>();
        for (int i = n - 1; i >= 0;) {
            int include = jobs.get(i).weight + (previous[i] < 0 ? 0 : best[previous[i]]);
            int exclude = i == 0 ? 0 : best[i - 1];
            if (include >= exclude) {
                result.add(jobs.get(i));
                i = previous[i];
            } else i--;
        }
        result.sort(Comparator.comparingInt(job -> job.startTime));
        return result;
    }
}
