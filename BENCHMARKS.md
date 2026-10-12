# Recorded threshold benchmark

Command: `java -cp target/manual-classes BenchmarkRunner` after the direct `javac` command in the README.

Environment: OpenJDK 21.0.8, Windows; fixed seed base `20261006`; three deterministic datasets per size; median of three measured runs; one OR, 08:00-20:00 shift. Dataset seeds are `20261006 + 31*n + repetition`. Durations are 30-120 minutes and arrivals are 08:00-10:00 in 30-minute increments. No clinical interpretation is encoded. Times are environment/JVM specific, with no warm-up phase, so treat small differences as indicative only.

| n | Mode | Median ms | Scheduled | Existing score | Average wait min | OR utilization % |
| ---: | --- | ---: | ---: | ---: | ---: | ---: |
| 10 | exact subset DP | 9.935 | 9 | 99,400 | 213.33 | 91.67 |
| 12 | exact subset DP | 11.103 | 10 | 110,900 | 162.00 | 87.50 |
| 13 | exact subset DP | 23.963 | 11 | 117,900 | 201.82 | 100.00 |
| 14 | exact subset DP | 32.671 | 12 | 130,150 | 197.50 | 100.00 |
| 15 | generated-interval fallback | 0.142 | 2 | 26,000 | 0.00 | 25.00 |
| 16 | generated-interval fallback | 0.073 | 3 | 38,000 | 0.00 | 25.00 |
| 20 | generated-interval fallback | 0.135 | 4 | 50,000 | 0.00 | 25.00 |
| 30 | generated-interval fallback | 0.105 | 4 | 50,000 | 0.00 | 25.00 |

This measures only the OR sequencing primitive around the threshold, not routing, multi-room LPT, bed assignment, emergency delay, memory, or end-to-end scenarios. Fallback candidates are generated at each patient's earliest feasible time; the observed lower case counts illustrate that candidate-generation limitation and are not evidence of better schedule quality. Use the benchmark command on the target machine before presenting timings.
