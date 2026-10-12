# Scheduling algorithms and objectives

The application is a staged optimization pipeline, not one unified optimizer. Each stage uses its existing local objective; a good local result does not prove a globally optimal hospital schedule.

## Active pipeline

| Stage | Purpose and input/output | Objective and constraints | Actual complexity and limitations |
| --- | --- | --- | --- |
| Hospital routing | Patients and candidate hospitals become hospital patient groups. | Successive shortest augmenting path min-cost max-flow: maximize routed flow first, then minimize preference plus distance cost. A general patient incurs `6,000,000` if not placed at their preferred hospital, and `round(distanceKm * 100)`; assigning a general patient to an ICU bed adds `3,000,000`. ICU-required patients may use ICU beds only. | Graph has `V=P+2H+2` vertices and `E=O(PH+H)` forward edges (plus residual edges). Each augmentation runs Bellman-Ford in `O(VE)`; total `O(FVE)` for flow F, storage `O(V+E)`. Distances/costs are integer approximations. The large penalty constants encode preferences and ICU preservation, rather than hard prohibitions.
| OR grouping | Patients are assigned to a compatible room by LPT. | Sort descending by duration, then assign each patient to the compatible room with least assigned duration. | Sorting expected `O(P log P)`, worst `O(P²)`; assignment `O(PR)`. Compatibility is surgery type. LPT is a practical workload heuristic; heterogeneous rooms and downstream constraints invalidate a blanket classical approximation guarantee.
| OR sequencing, n <= 14 | Eligible procedures for one OR produce a sequence of feasible start/end jobs. | Subset DP maximizes the sum of `10,000 + (4-priorityLevel)*1,000 - waitingMinutes*5`; applies arrival, OR booking, surgeon specialty/availability, shift end, and duration constraints. `MAX_EXACT_DP_PATIENTS=14` is an engineering threshold, not a proof-derived boundary. | Uses up to `2^n` masks and stores potentially multiple end-time states per mask. With T distinct end times and slot search cost Q: `O(2^n * T * n * Q)` time and `O(2^n * T)` space. The exponential subset dimension makes this suitable only for small n. Validate the threshold using benchmarks before changing it.
| OR sequencing, n > 14 | Creates one earliest feasible candidate interval per eligible patient, then selects a compatible set. | Weighted interval DP selects the generated non-overlapping candidates by the same score. | Candidate construction depends on OR gaps, surgeon count/availability, and the bounded shift search; sorting candidates is `O(n log n)`, predecessor search is implemented as a backwards scan `O(n²)`, and DP is `O(n)`. This is a polynomial fallback over generated candidates and does not guarantee the global optimum because candidate generation considers only one start per patient.
| Bed assignment | Scheduled patients and hospital beds produce bed reservations. | Hungarian assignment minimizes `max(0, priorityLevel*100 - waitingMinutes + (general patient assigned ICU bed ? 1000 : 0))`; ICU mismatch and overlapping reservation interval are forbidden. The interval is surgery start through surgery end plus 1,440 minutes. | The implementation pads a rectangular matrix to size `P+B` and runs cubic Hungarian: `O((P+B)^3)` time and `O((P+B)^2)` space. Unassigned dummy columns permit patients to remain unassigned; a later pipeline step rolls back their OR and staff reservations.

## Algorithm details

### Randomized three-way quicksort

Input is the mutable patient list; output is descending surgery duration. Each iteration chooses a uniformly random pivot and partitions into greater/equal/less duration regions. Expected time is `O(n log n)`, worst case `O(n²)`; equal keys are processed in one partition. Smaller-side recursion keeps auxiliary stack space `O(log n)`; list storage is in place. Randomization reduces sensitivity to input order but does not eliminate worst-case complexity. It is used to order LPT input.

### Bellman-Ford in min-cost max-flow

The router's residual shortest path routine performs up to `V-1` full relaxations over adjacency lists, with early stop when no distance changes. Per augmentation it is `O(VE)` time and `O(V)` distance/predecessor space. It handles negative reverse-edge costs. Edmonds-Karp in `NetworkFlow` is an additional implementation, not the active router.

### Emergency rescheduling

An emergency planning start bounds routing bed availability and the earliest start in every OR sequencing mode. A candidate starts no earlier than `max(patient arrival, planning start)`. Existing surgeries with start strictly before emergency arrival remain reserved and unchanged. A surgery starting exactly at the emergency minute has not started yet and is eligible for rescheduling. Normal initial planning uses 08:00 as the OR opening lower bound and still honors each patient's arrival. This is an availability constraint, not an objective weight.

## Cost constants in source

- Router: preference penalty `6,000,000`; general-patient ICU preservation penalty `3,000,000`; travel cost `round(distanceKm * 100)`.
- Exact/fallback schedule score: base `10,000`; priority contribution `(4-level)*1,000`; waiting penalty `5` per minute.
- Bed assignment: priority contribution `level*100`; subtract waiting minutes; add `1,000` for a general patient using ICU. Negative totals are clamped to zero.

These independently defined values are retained as implemented. The stages do not share a calibrated common unit or unified objective.

## DSA-3 mapping and viva boundaries

- **CO1:** classifies constrained routing, matching, string search, and NP-hard scheduling, then selects the existing flow, Hungarian, KMP, exact-DP, and heuristic strategies. Threshold benchmarking is a complexity-aware strategy decision.
- **CO2:** KMP is present for case-insensitive patient/staff name substring search, with `O(N+M)` time and `O(M)` LPS storage. It supports the project's search feature, not OR scheduling.
- **CO3:** subset masks model procedure subsets, with exponential state growth.
- **CO4:** min-cost max-flow routes under capacities; Hungarian assignment allocates beds.
- **CO5:** LPT and generated-interval fallback address hard scheduling constraints with practical heuristics.
- **CO6:** randomized pivot quicksort orders LPT input; expected `O(n log n)`, worst `O(n²)`.

Do not claim global optimality of the complete schedule, a classical LPT approximation ratio for these compatibility-constrained rooms, global optimality of the large-workload fallback, randomized quicksort worst-case elimination, or Edmonds-Karp as the active router. The small-workload DP is exact only relative to its modeled transitions and score for that OR workload; it is not a proof of system-wide optimality.
