package persistence;

import models.*;
import persistence.entity.*;
import java.util.*;
import org.springframework.stereotype.Component;

/** Converts durable records to plain scheduling models. The DSA packages remain persistence-free. */
@Component
public class DomainModelMapper {
    public HospitalEntity toEntity(Hospital hospital) {
        HospitalEntity entity = new HospitalEntity(hospital.getId(), hospital.getName(), hospital.getLocation(),
                hospital.getLatitude(), hospital.getLongitude());
        for (OperatingRoom room : hospital.getOperatingRooms())
            entity.getOperatingRooms().add(new OperatingRoomEntity(room.getId(), entity, room.getName(), 480, 1200,
                    room.getSupportedSurgeries()));
        for (Bed bed : hospital.getBeds()) entity.getBeds().add(new BedEntity(bed.getId(), entity, bed.getType()));
        for (Staff staff : hospital.getStaffMembers())
            entity.getStaff().add(new StaffEntity(staff.getId(), entity, staff.getName(), staff.getRole(),
                    staff.getShiftStart(), staff.getShiftEnd(), staff.getSpecializations()));
        return entity;
    }

    public Hospital toDomain(HospitalEntity entity) {
        Hospital hospital = new Hospital(entity.getId(), entity.getName(), entity.getLocation(),
                entity.getLatitude(), entity.getLongitude());
        for (BedEntity bed : entity.getBeds()) hospital.addBed(new Bed(bed.getId(), entity.getId(), bed.getType()));
        for (OperatingRoomEntity room : entity.getOperatingRooms()) hospital.addOperatingRoom(
                new OperatingRoom(room.getId(), entity.getId(), room.getName(), new ArrayList<>(room.getSpecialties())));
        for (StaffEntity staff : entity.getStaff()) hospital.addStaff(new Staff(staff.getId(), staff.getName(),
                staff.getRole(), entity.getId(), new ArrayList<>(staff.getSpecialties()), staff.getShiftStart(), staff.getShiftEnd()));
        return hospital;
    }

    public PatientEntity toEntity(Patient patient, PatientEntity.Status status) {
        return new PatientEntity(patient.getId(), patient.getName(), patient.getAge(), patient.getPriority(),
                patient.getSurgeryType(), patient.getSurgeryDuration(), patient.getArrivalTime(),
                patient.getPreferredHospitalId(), patient.requiresICU(), patient.getLatitude(), patient.getLongitude(),
                status, patient.getDischargeTime());
    }

    public Patient toDomain(PatientEntity entity) {
        Patient patient = new Patient(entity.getId(), entity.getName(), entity.getAge(), entity.getPriority(),
                entity.getSurgeryType(), entity.getSurgeryDuration(), entity.getArrivalTime(),
                entity.getPreferredHospitalId(), entity.isRequiresIcu());
        patient.setLocation(entity.getLatitude(), entity.getLongitude());
        if (entity.getStatus() == PatientEntity.Status.DISCHARGED)
            patient.discharge(Math.max(0, entity.getDischargeTime()));
        return patient;
    }

    public void applyActiveState(List<Hospital> hospitals, List<Patient> patients,
                                 List<ScheduleEntity> activeSchedules, List<BedReservationEntity> activeReservations) {
        Map<Integer, Patient> patientById = new HashMap<>();
        patients.forEach(patient -> patientById.put(patient.getId(), patient));
        Map<Integer, Hospital> hospitalById = new HashMap<>();
        hospitals.forEach(hospital -> hospitalById.put(hospital.getId(), hospital));
        for (BedReservationEntity reservation : activeReservations) {
            Hospital hospital = hospitalById.get(reservation.getBed().getHospital().getId());
            if (hospital == null) continue;
            hospital.getBeds().stream().filter(bed -> bed.getId() == reservation.getBed().getId()).findFirst()
                    .ifPresent(bed -> bed.assignPatient(reservation.getPatient().getId(),
                            reservation.getStartTime(), reservation.getEndTime()));
        }
        for (ScheduleEntity assignment : activeSchedules) {
            Patient patient = patientById.get(assignment.getPatient().getId());
            Hospital hospital = hospitalById.get(assignment.getHospital().getId());
            if (patient == null || hospital == null) continue;
            OperatingRoom room = hospital.getOperatingRooms().stream()
                    .filter(or -> or.getId() == assignment.getOperatingRoom().getId()).findFirst().orElse(null);
            Staff staff = hospital.getStaffMembers().stream()
                    .filter(member -> member.getId() == assignment.getStaff().getId()).findFirst().orElse(null);
            if (room == null || staff == null) continue;
            room.scheduleSlot(assignment.getStartTime(), assignment.getEndTime(), patient.getId());
            staff.assignSlot(assignment.getStartTime(), assignment.getEndTime(), patient.getId());
            patient.setScheduled(true); patient.setAssignedHospitalId(hospital.getId());
            patient.setAssignedORId(room.getId()); patient.setAssignedStaffId(staff.getId());
            patient.setScheduledStartTime(assignment.getStartTime());
            patient.setWaitingTime(Math.max(0, assignment.getStartTime() - patient.getArrivalTime()));
            activeReservations.stream().filter(res -> res.getSchedule().getId().equals(assignment.getId()))
                    .findFirst().ifPresent(res -> patient.setAssignedBedId(res.getBed().getId()));
        }
    }

    /** Restores the historical procedure fields exposed by the existing patient API, without reserving resources. */
    public void applyCompletedHistory(List<Patient> patients, List<ScheduleEntity> completedSchedules) {
        Map<Integer,Patient> byId=new HashMap<>(); patients.forEach(patient->byId.put(patient.getId(),patient));
        completedSchedules.stream().sorted(Comparator.comparing(ScheduleEntity::getId)).forEach(assignment->{
            Patient patient=byId.get(assignment.getPatient().getId());
            if(patient==null || !patient.isDischarged()) return;
            patient.setScheduled(true); patient.setAssignedHospitalId(assignment.getHospital().getId());
            patient.setAssignedORId(assignment.getOperatingRoom().getId()); patient.setAssignedStaffId(assignment.getStaff().getId());
            patient.setScheduledStartTime(assignment.getStartTime());
            patient.setWaitingTime(Math.max(0,assignment.getStartTime()-patient.getArrivalTime()));
        });
    }
}
