package web;

import models.Hospital;
import models.Patient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class SchedulerController {
    private final SchedulerService schedulerService;

    public SchedulerController(SchedulerService schedulerService) {
        this.schedulerService = schedulerService;
    }

    @GetMapping("/hospitals")
    public List<Hospital> hospitals() {
        return schedulerService.getHospitals();
    }

    @GetMapping("/patients")
    public List<Patient> patients() {
        return schedulerService.getPatients();
    }

    @PostMapping("/patients")
    public Patient createPatient(@RequestBody PatientIntakeRequest request) {
        return schedulerService.createPatient(request);
    }

    @GetMapping("/schedules")
    public List<SchedulerService.ScheduleSnapshot> schedules() {
        return schedulerService.getScheduleHistory();
    }

    @GetMapping("/discharges")
    public List<SchedulerService.DischargeSnapshot> discharges() {
        return schedulerService.getDischargeHistory();
    }

    @PostMapping("/schedules/run")
    public List<Patient> runScheduling() {
        return schedulerService.runScheduling();
    }

    @PostMapping("/emergencies")
    public Patient addEmergency(@RequestBody EmergencyIntakeRequest request) {
        return schedulerService.addEmergency(request.name(), request.surgeryType(),
                request.durationMinutes(), request.requiresICU(), request.arrivalTime());
    }

    @PostMapping("/patients/{id}/discharge")
    public Patient discharge(@PathVariable("id") int id, @RequestBody DischargeRequest request) {
        return schedulerService.discharge(id, request.dischargeTime());
    }
}
