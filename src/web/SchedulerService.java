package web;

import jakarta.transaction.Transactional;
import models.*;
import org.springframework.stereotype.Service;
import persistence.DomainModelMapper;
import persistence.entity.*;
import persistence.repository.*;
import scheduling.SchedulingEngine;

import java.util.*;
import java.util.stream.Collectors;

/** Transactional persistence boundary. Scheduling algorithms receive only plain models. */
@Service
public class SchedulerService {
    private final HospitalRepository hospitalsRepo;
    private final PatientRepository patientsRepo;
    private final OperatingRoomRepository roomsRepo;
    private final BedRepository bedsRepo;
    private final StaffRepository staffRepo;
    private final ScheduleRepository schedulesRepo;
    private final BedReservationRepository reservationsRepo;
    private final DischargeRepository dischargesRepo;
    private final DomainModelMapper mapper;

    public SchedulerService(HospitalRepository hospitalsRepo, PatientRepository patientsRepo,
                            OperatingRoomRepository roomsRepo, BedRepository bedsRepo, StaffRepository staffRepo,
                            ScheduleRepository schedulesRepo, BedReservationRepository reservationsRepo,
                            DischargeRepository dischargesRepo, DomainModelMapper mapper) {
        this.hospitalsRepo=hospitalsRepo; this.patientsRepo=patientsRepo; this.roomsRepo=roomsRepo;
        this.bedsRepo=bedsRepo; this.staffRepo=staffRepo; this.schedulesRepo=schedulesRepo;
        this.reservationsRepo=reservationsRepo; this.dischargesRepo=dischargesRepo; this.mapper=mapper;
    }

    @Transactional
    public List<Hospital> getHospitals() { return load().hospitals; }

    @Transactional
    public List<Patient> getPatients() { return load().patients; }

    @Transactional
    public List<Patient> runScheduling() {
        State state=load();
        state.patients.stream().filter(patient -> !patient.isDischarged()).forEach(Patient::resetSchedule);
        clearReservations(state.hospitals);
        SchedulingEngine.runFullPipeline(state.patients, state.hospitals);
        validate(state, null, null);
        persistResults(state, false, null);
        return state.patients;
    }

    @Transactional
    public Patient addEmergency(EmergencyIntakeRequest request) {
        String name=request.name(), surgeryType=request.surgeryType(), arrivalTime=request.arrivalTime();
        int duration=request.durationMinutes();
        boolean requiresICU=request.requiresICU();
        Patient.SurgeryType type;
        try {
            type=Patient.SurgeryType.valueOf(surgeryType==null?"":surgeryType.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Choose one of the listed surgery types: CARDIAC, ORTHOPEDIC, NEUROLOGICAL, GENERAL, OPHTHALMIC, ENT.");
        }
        State state=load();
        Patient emergency=SchedulingEngine.processEmergency(state.patients,state.hospitals,name,type,duration,requiresICU,
                SchedulingEngine.parseClockTime(arrivalTime));
        emergency.setAge(request.age());
        emergency.setDemographics(request.gender()==null||request.gender().isBlank()?null:request.gender(),
                request.bloodGroup()==null||request.bloodGroup().isBlank()?null:request.bloodGroup(),
                request.allergies()==null||request.allergies().isBlank()?null:request.allergies().trim());
        int id=emergency.getId();
        validate(state, arrivalTime, id);
        persistResults(state,true,id);
        return state.patients.stream().filter(patient -> patient.getId()==id).findFirst().orElseThrow();
    }

    /** Compatibility overload for existing internal callers and integration tests. */
    @Transactional
    public Patient addEmergency(String name, String surgeryType, int duration, boolean requiresICU, String arrivalTime) {
        return addEmergency(new EmergencyIntakeRequest(name, 40, null, null, null, surgeryType,
                duration, arrivalTime, requiresICU));
    }

    @Transactional
    public Patient createPatient(PatientIntakeRequest request) {
        if (request==null || request.name()==null || request.name().isBlank())
            throw new IllegalArgumentException("Patient name cannot be empty.");
        if (request.durationMinutes()<=0 || request.age()<0)
            throw new IllegalArgumentException("Age and surgery duration must be valid.");
        if ((request.latitude()==null)!=(request.longitude()==null))
            throw new IllegalArgumentException("Provide both latitude and longitude, or neither.");
        Patient.SurgeryType type;
        Patient.Priority priority;
        try {
            type=Patient.SurgeryType.valueOf(request.surgeryType().trim().toUpperCase(Locale.ROOT));
            priority=Patient.Priority.valueOf(request.priority().trim().toUpperCase(Locale.ROOT));
        } catch (RuntimeException error) { throw new IllegalArgumentException("Invalid surgery type or priority."); }
        int id=patientsRepo.findAll().stream().mapToInt(PatientEntity::getId).max().orElse(0)+1;
        Patient patient=new Patient(id,request.name(),request.age(),priority,type,request.durationMinutes(),
                SchedulingEngine.parseClockTime(request.arrivalTime()),request.preferredHospitalId(),request.requiresICU());
        if (request.latitude()!=null && request.longitude()!=null) patient.setLocation(request.latitude(),request.longitude());
        patientsRepo.save(mapper.toEntity(patient,PatientEntity.Status.PENDING));
        return patient;
    }

    @Transactional
    public Patient discharge(int patientId, String dischargeTimeText) {
        State state=load();
        Patient patient=state.patients.stream().filter(p -> p.getId()==patientId).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No patient found with ID P"+patientId+"."));
        int time=SchedulingEngine.parseClockTime(dischargeTimeText);
        BedReservationEntity reservation=reservationsRepo.findByPatientId(patientId).stream()
                .filter(item -> item.getStatus()==BedReservationEntity.Status.ACTIVE).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Patient P"+patientId+" has no active bed reservation to discharge."));
        SchedulingEngine.dischargePatient(state.patients,state.hospitals,patientId,dischargeTimeText);
        reservation.setStatus(BedReservationEntity.Status.ENDED);
        reservation.setEndTime(time);
        schedulesRepo.findByPatientId(patientId).stream().filter(item -> item.getStatus()==ScheduleEntity.Status.ACTIVE)
                .forEach(item -> item.setStatus(ScheduleEntity.Status.COMPLETED));
        PatientEntity patientEntity=patientsRepo.findById(patientId).orElseThrow();
        patientEntity.setStatus(PatientEntity.Status.DISCHARGED); patientEntity.setDischargeTime(time);
        patientsRepo.save(patientEntity);
        dischargesRepo.save(new DischargeEntity(patientEntity,reservation,time));
        return patient;
    }

    @Transactional
    public List<ScheduleSnapshot> getScheduleHistory() {
        return schedulesRepo.findAll().stream().sorted(Comparator.comparing(ScheduleEntity::getId))
                .map(item -> new ScheduleSnapshot(item.getId(),item.getPatient().getId(),item.getHospital().getId(),
                        item.getOperatingRoom().getId(),item.getStaff().getId(),item.getStartTime(),item.getEndTime(),
                        item.getStatus().name(),item.getAlgorithmPath())).toList();
    }

    @Transactional
    public List<DischargeSnapshot> getDischargeHistory() {
        return dischargesRepo.findAll().stream().map(item -> new DischargeSnapshot(item.getId(),
                item.getPatient().getId(),item.getBedReservation().getBed().getId(),item.getDischargeTime())).toList();
    }

    private State load() {
        List<HospitalEntity> hospitalEntities=hospitalsRepo.findAll();
        List<PatientEntity> patientEntities=patientsRepo.findAll();
        List<Hospital> hospitals=hospitalEntities.stream().map(mapper::toDomain).collect(Collectors.toCollection(ArrayList::new));
        List<Patient> patients=patientEntities.stream().map(mapper::toDomain).collect(Collectors.toCollection(ArrayList::new));
        List<ScheduleEntity> activeSchedules=schedulesRepo.findByStatus(ScheduleEntity.Status.ACTIVE);
        mapper.applyActiveState(hospitals,patients,activeSchedules,
                reservationsRepo.findByStatus(BedReservationEntity.Status.ACTIVE));
        mapper.applyCompletedHistory(patients,schedulesRepo.findByStatus(ScheduleEntity.Status.COMPLETED));
        return new State(hospitals,patients,hospitalEntities,patientEntities,activeSchedules);
    }

    private void clearReservations(List<Hospital> hospitals) {
        hospitals.forEach(hospital -> {
            hospital.getOperatingRooms().forEach(OperatingRoom::clearSchedule);
            hospital.getStaffMembers().forEach(Staff::clearAssignments);
            hospital.getBeds().forEach(Bed::release);
        });
    }

    private void validate(State state, String emergencyArrival, Integer emergencyId) {
        Map<Integer,Patient> patients=state.patients.stream().collect(Collectors.toMap(Patient::getId,p->p));
        List<AssignedInterval> intervals=new ArrayList<>();
        for (Patient patient : state.patients) {
            if (!patient.isScheduled() || patient.isDischarged()) continue;
            if (patient.getScheduledStartTime()<patient.getArrivalTime())
                throw new IllegalStateException("Schedule begins before patient arrival: P"+patient.getId());
            Hospital hospital=state.hospitals.stream().filter(h->h.getId()==patient.getAssignedHospitalId()).findFirst()
                    .orElseThrow(()->new IllegalStateException("Scheduled patient's hospital is missing."));
            OperatingRoom room=hospital.getOperatingRooms().stream().filter(or->or.getId()==patient.getAssignedORId()).findFirst()
                    .orElseThrow(()->new IllegalStateException("Scheduled patient's OR is missing."));
            Staff staff=hospital.getStaffMembers().stream().filter(member->member.getId()==patient.getAssignedStaffId()).findFirst()
                    .orElseThrow(()->new IllegalStateException("Scheduled patient's surgeon is missing."));
            int start=patient.getScheduledStartTime(), end=start+patient.getSurgeryDuration();
            if (!room.supportsSurgery(patient.getSurgeryType()) || start<480 || end>1200 || !staff.canPerformSurgery(patient.getSurgeryType())
                    || start<staff.getShiftStart() || end>staff.getShiftEnd())
                throw new IllegalStateException("Schedule violates OR or staff constraints for P"+patient.getId());
            Bed bed=hospital.getBeds().stream().filter(value->value.getId()==patient.getAssignedBedId()).findFirst()
                    .orElseThrow(()->new IllegalStateException("Scheduled patient has no bed: P"+patient.getId()));
            if (patient.requiresICU() && bed.getType()!=Bed.BedType.ICU)
                throw new IllegalStateException("ICU patient assigned to a non-ICU bed.");
            if (emergencyArrival!=null) {
                int emergencyMinute=SchedulingEngine.parseClockTime(emergencyArrival);
                boolean wasActive=state.activeSchedules.stream().anyMatch(old->old.getPatient().getId().equals(patient.getId()));
                boolean unchanged=state.activeSchedules.stream().anyMatch(old->old.getPatient().getId().equals(patient.getId())
                        && old.getStartTime()==start && old.getOperatingRoom().getId().equals(room.getId()));
                if (wasActive && !unchanged && start<emergencyMinute)
                    throw new IllegalStateException("Emergency rescheduling placed a pending procedure before arrival.");
            }
            intervals.add(new AssignedInterval(patient,hospital,room,staff,bed,start,end,end+1440));
        }
        for (int i=0;i<intervals.size();i++) for (int j=i+1;j<intervals.size();j++) {
            AssignedInterval a=intervals.get(i), b=intervals.get(j);
            if (a.hospital.getId()!=b.hospital.getId()) continue;
            if (a.room.getId()==b.room.getId() && overlaps(a.start,a.end,b.start,b.end))
                throw new IllegalStateException("Two active schedules overlap in one OR.");
            if (a.staff.getId()==b.staff.getId() && overlaps(a.start,a.end,b.start,b.end))
                throw new IllegalStateException("Two active schedules overlap for one staff member.");
            if (a.bed.getId()==b.bed.getId() && overlaps(a.start,a.bedEnd,b.start,b.bedEnd))
                throw new IllegalStateException("Overlapping bed reservations.");
        }
    }

    private boolean overlaps(int aStart,int aEnd,int bStart,int bEnd){return aStart<bEnd && bStart<aEnd;}

    private void persistResults(State state, boolean emergency, Integer emergencyPatientId) {
        Map<Integer,HospitalEntity> hospitalEntities=state.hospitalEntities.stream().collect(Collectors.toMap(HospitalEntity::getId,h->h));
        Map<Integer,PatientEntity> patientEntities=patientsRepo.findAll().stream().collect(Collectors.toMap(PatientEntity::getId,p->p));
        Map<Integer,OperatingRoomEntity> roomEntities=roomsRepo.findAll().stream().collect(Collectors.toMap(OperatingRoomEntity::getId,r->r));
        Map<Integer,StaffEntity> staffEntities=staffRepo.findAll().stream().collect(Collectors.toMap(StaffEntity::getId,s->s));
        Map<Integer,BedEntity> bedEntities=bedsRepo.findAll().stream().collect(Collectors.toMap(BedEntity::getId,b->b));
        Map<Integer,ScheduleEntity> unchanged=new HashMap<>();
        for (ScheduleEntity old : schedulesRepo.findByStatus(ScheduleEntity.Status.ACTIVE)) {
            Patient current=state.patients.stream().filter(p->p.getId()==old.getPatient().getId()).findFirst().orElse(null);
            boolean same=emergency && current!=null && current.isScheduled() && !current.isDischarged()
                    && current.getScheduledStartTime()==old.getStartTime()
                    && current.getAssignedORId()==old.getOperatingRoom().getId()
                    && current.getAssignedStaffId()==old.getStaff().getId();
            if (same) unchanged.put(current.getId(),old);
            else {
                old.setStatus(ScheduleEntity.Status.SUPERSEDED);
            reservationsRepo.findFirstByScheduleId(old.getId()).ifPresent(res->res.setStatus(BedReservationEntity.Status.SUPERSEDED));
            }
        }
        for (Patient patient : state.patients) {
            PatientEntity.Status status=patient.isDischarged()?PatientEntity.Status.DISCHARGED:
                    patient.isScheduled()?PatientEntity.Status.SCHEDULED:
                            patient.getAssignedHospitalId()>=0?PatientEntity.Status.ROUTED:PatientEntity.Status.PENDING;
            PatientEntity entity=patientEntities.get(patient.getId());
            if (entity==null) entity=patientsRepo.saveAndFlush(mapper.toEntity(patient,status));
            else entity.setStatus(status);
            if (patient.isDischarged()) entity.setDischargeTime(patient.getDischargeTime());
            patientEntities.put(patient.getId(),entity);
            if (!patient.isScheduled() || patient.isDischarged() || unchanged.containsKey(patient.getId())) continue;
            HospitalEntity hospital=hospitalEntities.get(patient.getAssignedHospitalId());
            OperatingRoomEntity room=roomEntities.get(patient.getAssignedORId());
            StaffEntity staff=staffEntities.get(patient.getAssignedStaffId());
            BedEntity bed=bedEntities.get(patient.getAssignedBedId());
            if (hospital==null||room==null||staff==null||bed==null) throw new IllegalStateException("Incomplete assignment cannot be persisted.");
            int end=patient.getScheduledStartTime()+patient.getSurgeryDuration();
            long roomLoad=state.patients.stream().filter(p->p.isScheduled()&&p.getAssignedORId()==room.getId()).count();
            String path="MIN_COST_FLOW+LPT+"+(roomLoad<=algorithms.DPScheduler.MAX_EXACT_DP_PATIENTS
                    ?"EXACT_SUBSET_DP":"WEIGHTED_INTERVAL_FALLBACK")+"+HUNGARIAN";
            if (emergency) path+="+EMERGENCY_RESCHEDULE";
            ScheduleEntity schedule=schedulesRepo.save(new ScheduleEntity(entity,hospital,room,staff,
                    patient.getScheduledStartTime(),end,ScheduleEntity.Status.ACTIVE,path));
            reservationsRepo.save(new BedReservationEntity(entity,bed,schedule,patient.getScheduledStartTime(),
                    end+1440,BedReservationEntity.Status.ACTIVE));
        }
        patientsRepo.saveAll(patientEntities.values());
    }

    private static final class State {
        final List<Hospital> hospitals; final List<Patient> patients;
        final List<HospitalEntity> hospitalEntities; final List<PatientEntity> patientEntities;
        final List<ScheduleEntity> activeSchedules;
        State(List<Hospital> hospitals,List<Patient> patients,List<HospitalEntity> hospitalEntities,
              List<PatientEntity> patientEntities,List<ScheduleEntity> activeSchedules){
            this.hospitals=hospitals;this.patients=patients;this.hospitalEntities=hospitalEntities;this.patientEntities=patientEntities;
            this.activeSchedules=activeSchedules;
        }
    }
    private record AssignedInterval(Patient patient,Hospital hospital,OperatingRoom room,Staff staff,Bed bed,
                                    int start,int end,int bedEnd) { }
    public record ScheduleSnapshot(Long id,Integer patientId,Integer hospitalId,Integer operatingRoomId,Integer staffId,
                                   int startTime,int endTime,String status,String algorithmPath) { }
    public record DischargeSnapshot(Long id,Integer patientId,Integer bedId,int dischargeTime) { }
}
