# Hospital Bed and Operating Room Scheduler

This repository includes the original Java command-line program, a Spring Boot REST API, and a browser dashboard that reuses its scheduling engine.

## Run the web API

Requirements: JDK 21 and Maven 3.6.3 or newer.

From the project directory:

```sh
mvn spring-boot:run
```

After starting the app, open **http://localhost:8080** for the browser dashboard. It displays hospital capacity and patient schedules, and provides controls to run scheduling, add an emergency patient, search/filter patients, and discharge an assigned patient.

The API is available at `http://localhost:8080`:

| Method | Endpoint | Purpose |
| --- | --- | --- |
| `GET` | `/api/hospitals` | List hospitals, capacity, rooms, beds, and staff |
| `GET` | `/api/patients` | List patients and their current schedule status |
| `POST` | `/api/schedules/run` | Rebuild and run the full schedule, then return patients |
| `POST` | `/api/emergencies` | Add and schedule an emergency patient |
| `POST` | `/api/patients/{id}/discharge` | Discharge a scheduled patient and release their bed |

Emergency request JSON: `{"name":"Sam Lee","surgeryType":"GENERAL","durationMinutes":90,"requiresICU":false,"arrivalTime":"14:30"}`.
Discharge request JSON: `{"dischargeTime":"16:45"}`. Times use 24-hour `HH:mm` format. Invalid input and unavailable discharge operations return HTTP 400 with an `error` message.

The web application seeds 16 demo patients into a persistent file-based database on first launch. An existing database containing exactly the original 12 demo patients is upgraded with the four additional sample patients; databases with added or customized patient records are left unchanged. The original `Main` command-line entry point remains available for algorithm-only use.

The system decomposes hospital scheduling into network-flow routing, heuristic room balancing, bounded exact dynamic programming, a scalable candidate-interval fallback, and minimum-cost bed assignment. It produces feasible schedules subject to its modeled constraints; it does not claim global optimality across the complete pipeline. See [ALGORITHMS.md](ALGORITHMS.md) for the actual stage objectives, source constants, complexity, limitations, and DSA-3 mapping.

Emergency rescheduling preserves procedures whose start is before the emergency arrival. A case whose start is exactly the arrival minute is considered not yet started and may be rescheduled. All replanned OR starts are bounded below by both the patient's arrival and the emergency planning start.

## Algorithm regression and threshold benchmark

The existing plain Java regression harness can be compiled and run without Maven/Spring dependencies:

```sh
javac -d target/manual-classes src/models/*.java src/algorithms/*.java src/scheduling/*.java src/data/*.java tests/RegressionTests.java tests/BenchmarkRunner.java
java -cp target/manual-classes RegressionTests
java -cp target/manual-classes BenchmarkRunner
```

`BenchmarkRunner` uses a fixed seed (`20261006`), three repetitions, and reports median OR-sequencing time and schedule metrics for workloads around the 14-procedure boundary. It measures the current single-OR exact/fallback sequencing primitive; it is not a full-pipeline benchmark or a comparison of all five pipeline variants. Results are machine/JVM dependent and should be regenerated on the demo machine.

The actual recorded run and its environment/limitations are in [BENCHMARKS.md](BENCHMARKS.md).

## Persistent Hospital State

The REST application uses Spring Data JPA, Hibernate, and file-based H2. The database is stored under `./data/hospital_scheduler` relative to the application working directory and survives application and IDE restarts. The `data/` directory is generated at runtime and should not be committed.

```text
REST API
   ↓
Transactional SchedulerService
   ↓
Repositories and JPA entities
   ↓
DomainModelMapper → plain scheduling models
   ↓
DSA SchedulingEngine (flow, LPT, DP/fallback, Hungarian)
   ↓
Scheduling result and validation
   ↓
Repositories → file-based H2
```

JPA records live in `persistence.entity`; repositories live in `persistence.repository`. `HospitalEntity` owns OR, bed, and staff records. `PatientEntity` stores the surgery request and status. Each `ScheduleEntity` links its patient, hospital, room, and surgeon, and records its scheduling path and active/history status. `BedReservationEntity` stores the bed interval; current bed occupancy is derived from active reservations. `DischargeEntity` preserves discharge history. The current model represents surgery requests as patient surgery fields, so no separate surgery-request table duplicates those values.

`SchedulerService` owns transaction boundaries. It converts database rows into ordinary `models.*` values, invokes `SchedulingEngine`, validates the returned assignments, then writes schedule/bed reservation history and patient status in one transaction. The algorithms have no Spring or persistence dependency. Reruns supersede active assignments instead of deleting history; discharge ends the active bed reservation and marks the schedule completed. Emergency intake uses the existing planning-start behavior and preserves already-started work.

Demo initialization runs only when both the hospital and patient tables are empty. `GET /api/schedules` exposes schedule history and algorithm paths; `GET /api/discharges` exposes discharge records. `POST /api/patients` accepts a patient request in addition to the existing emergency endpoint.

### Run and inspect the database

1. Start the API with `mvn spring-boot:run` from the project directory.
2. Open `http://localhost:8080/h2-console` for the development H2 console.
3. Connect using JDBC URL `jdbc:h2:file:./data/hospital_scheduler;AUTO_SERVER=TRUE`, username `sa`, and an empty password.
4. Stop the application before resetting development data. Remove `data/hospital_scheduler.mv.db` (and any adjacent H2 lock/trace files); the next startup recreates the demo rows.

To verify persistence, call `POST /api/schedules/run`, then inspect `GET /api/patients` and `GET /api/schedules`. Stop and restart the backend, then fetch those endpoints again. Patient OR/bed assignments and schedule paths should remain. After discharging an assigned patient, `GET /api/discharges` retains the discharge record across the same restart.

The existing list and scheduling endpoints retain their domain-model response shape. Additional persistence endpoints expose history without returning lazy JPA entities.
