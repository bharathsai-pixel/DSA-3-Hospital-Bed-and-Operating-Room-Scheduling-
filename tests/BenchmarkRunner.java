import algorithms.DPScheduler;
import models.OperatingRoom;
import models.Patient;
import models.Staff;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/** Reproducible OR-sequencing threshold benchmark. Run after compiling tests; CSV goes to stdout. */
public final class BenchmarkRunner {
    private static final long SEED = 20261006L;
    private static final int REPETITIONS = 3;

    public static void main(String[] args) {
        int[] sizes = {10, 12, 13, 14, 15, 16, 20, 30};
        System.out.println("seed,repetitions,patients,mode,median_runtime_ms,scheduled,score,avg_wait_min,or_utilization_pct");
        for (int n : sizes) run(n);
    }

    private static void run(int n) {
        List<Long> times = new ArrayList<>();
        List<DPScheduler.Job> lastJobs = List.of();
        for (int repetition = 0; repetition < REPETITIONS; repetition++) {
            Random random = new Random(SEED + n * 31L + repetition);
            List<Patient> patients = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                int duration = 30 + random.nextInt(4) * 30;
                int arrival = 480 + random.nextInt(4) * 30;
                Patient.Priority priority = Patient.Priority.values()[random.nextInt(Patient.Priority.values().length)];
                patients.add(new Patient(i + 1, "P" + (i + 1), 35, priority,
                        Patient.SurgeryType.GENERAL, duration, arrival, 1, false));
            }
            OperatingRoom or = new OperatingRoom(1, 1, "OR", Arrays.asList(Patient.SurgeryType.GENERAL));
            Staff staff = new Staff(1, "Surgeon", Staff.Role.SURGEON, 1,
                    Arrays.asList(Patient.SurgeryType.GENERAL), 480, 1200);
            long start = System.nanoTime();
            lastJobs = DPScheduler.schedulePatients(patients, or, Arrays.asList(staff), 480, 1200);
            times.add(System.nanoTime() - start);
        }
        Collections.sort(times);
        long medianNs = times.get(times.size() / 2);
        int score = lastJobs.stream().mapToInt(job -> job.weight).sum();
        double avgWait = lastJobs.stream().mapToInt(job -> job.startTime - job.patient.getArrivalTime())
                .average().orElse(0.0);
        int scheduledMinutes = lastJobs.stream().mapToInt(job -> job.endTime - job.startTime).sum();
        double utilization = scheduledMinutes / 720.0 * 100.0;
        String mode = n <= DPScheduler.MAX_EXACT_DP_PATIENTS ? "exact-subset-dp" : "generated-interval-fallback";
        System.out.printf(java.util.Locale.ROOT, "%d,%d,%d,%s,%.3f,%d,%d,%.2f,%.2f%n",
                SEED, REPETITIONS, n, mode, medianNs / 1_000_000.0,
                lastJobs.size(), score, avgWait, utilization);
    }
}
