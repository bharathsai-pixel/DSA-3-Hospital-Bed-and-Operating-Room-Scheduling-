package web;

import models.Patient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import jakarta.validation.Validator;
import jakarta.validation.ConstraintViolation;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.MethodOrderer;
import persistence.DemoDataInitializer;
import persistence.entity.*;
import persistence.repository.*;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties="spring.datasource.url=jdbc:h2:mem:persistence_integration;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PersistenceIntegrationTests {
    @Autowired PatientRepository patients;
    @Autowired HospitalRepository hospitals;
    @Autowired OperatingRoomRepository rooms;
    @Autowired BedRepository beds;
    @Autowired StaffRepository staff;
    @Autowired ScheduleRepository schedules;
    @Autowired BedReservationRepository reservations;
    @Autowired DischargeRepository discharges;
    @Autowired DemoDataInitializer initializer;
    @Autowired SchedulerService service;
    @Autowired Validator validator;
    @Autowired MockMvc mockMvc;

    @Test
    @Order(1)
    void emergencyIntakeValidatesRequiredAndOptionalFields() {
        try {
            mockMvc.perform(post("/api/emergencies/intake").contentType("application/json")
                            .content("{\"name\":\"\",\"age\":121,\"gender\":\"Invalid\",\"bloodGroup\":\"X+\",\"allergies\":\"\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors.name").exists())
                    .andExpect(jsonPath("$.fieldErrors.age").exists())
                    .andExpect(jsonPath("$.fieldErrors.surgeryType").exists());
        } catch (Exception exception) {
            throw new AssertionError("Emergency API should return field-level validation errors.", exception);
        }
        EmergencyIntakeRequest complete=new EmergencyIntakeRequest("Emergency validation patient",42,"Female","O+",
                "Penicillin", "GENERAL",60,"14:30",true);
        assertThat(validator.validate(complete)).isEmpty();
        EmergencyIntakeRequest optionalBlank=new EmergencyIntakeRequest("Emergency validation patient",0,"","", "",
                "ENT",30,"14:30",false);
        assertThat(validator.validate(optionalBlank)).isEmpty();

        EmergencyIntakeRequest missing=new EmergencyIntakeRequest(" ",null,"","X+","x".repeat(501),
                "",0,"25:61",null);
        assertThat(validator.validate(missing)).extracting(ConstraintViolation::getPropertyPath)
                .map(Object::toString).contains("name","age","bloodGroup","allergies","surgeryType",
                        "durationMinutes","arrivalTime","requiresICU");
        assertThat(validator.validate(new EmergencyIntakeRequest("Age low",-1,null,null,null,"GENERAL",1,"00:00",true)))
                .extracting(ConstraintViolation::getPropertyPath).map(Object::toString).contains("age");
        assertThat(validator.validate(new EmergencyIntakeRequest("Age high",121,null,null,null,"GENERAL",1,"00:00",true)))
                .extracting(ConstraintViolation::getPropertyPath).map(Object::toString).contains("age");
    }

    @Test
    @Order(6)
    void emergencyIntakePersistsDemographicsAndUsesEmergencyPriority() {
        EmergencyIntakeRequest request=new EmergencyIntakeRequest("Emergency form persistence",37,"Other","AB-",
                "Latex", "GENERAL",30,"14:30",false);
        Patient patient=service.addEmergency(request);
        PatientEntity saved=patients.findById(patient.getId()).orElseThrow();
        assertThat(saved.getAge()).isEqualTo(37);
        assertThat(saved.getGender()).isEqualTo("Other");
        assertThat(saved.getBloodGroup()).isEqualTo("AB-");
        assertThat(saved.getAllergies()).isEqualTo("Latex");
        assertThat(patient.getPriority()).isEqualTo(Patient.Priority.EMERGENCY);
        assertThat(saved.isRequiresIcu()).isFalse();
    }

    @Test
    @Order(7)
    void legacyEmergencyEndpointKeepsItsTransactionOpenWhileMappingHospitals() throws Exception {
        mockMvc.perform(post("/api/emergencies").contentType("application/json")
                        .content("{\"name\":\"Legacy endpoint regression\",\"surgeryType\":\"GENERAL\",\"durationMinutes\":30,\"requiresICU\":false,\"arrivalTime\":\"14:30\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Legacy endpoint regression"))
                .andExpect(jsonPath("$.age").value(40));
    }

    @Test
    @Order(3)
    void newlyCreatedPatientIsStoredWithStatus() {
        Patient patient=service.createPatient(new PatientIntakeRequest("Persistent test patient",31,"URGENT",
                "GENERAL",45,"11:00",1,false,17.385,78.4867));
        PatientEntity saved=patients.findById(patient.getId()).orElseThrow();
        assertThat(saved.getName()).isEqualTo("Persistent test patient");
        assertThat(saved.getStatus()).isEqualTo(PatientEntity.Status.PENDING);
    }

    @Test
    @Order(2)
    void demoSeedIsIdempotentAndPersistsAllBaseEntities() throws Exception {
        long hospitalCount=hospitals.count(), patientCount=patients.count();
        assertThat(hospitalCount).isGreaterThan(0);
        assertThat(patientCount).isGreaterThan(0);
        assertThat(rooms.count()).isGreaterThan(0);
        assertThat(beds.count()).isGreaterThan(0);
        assertThat(staff.count()).isGreaterThan(0);
        ApplicationArguments args=new DefaultApplicationArguments(new String[0]);
        initializer.run(args); initializer.run(args);
        assertThat(hospitals.count()).isEqualTo(hospitalCount);
        assertThat(patients.count()).isEqualTo(patientCount);
    }

    @Test
    @Order(5)
    void schedulesBedReservationsAndDischargesPersistAsHistory() {
        List<Patient> scheduled=service.runScheduling();
        Patient patient=scheduled.stream().filter(p->p.isScheduled()&&p.getAssignedBedId()>=0).findFirst().orElseThrow();
        ScheduleEntity assignment=schedules.findByStatus(ScheduleEntity.Status.ACTIVE).stream()
                .filter(item->item.getPatient().getId().equals(patient.getId())).findFirst().orElseThrow();
        BedReservationEntity reservation=reservations.findByStatus(BedReservationEntity.Status.ACTIVE).stream()
                .filter(item->item.getPatient().getId().equals(patient.getId())).findFirst().orElseThrow();
        assertThat(assignment.getOperatingRoom().getId()).isEqualTo(patient.getAssignedORId());
        assertThat(reservation.getBed().getId()).isEqualTo(patient.getAssignedBedId());
        int dischargeTime=patient.getScheduledStartTime()+patient.getSurgeryDuration();
        service.discharge(patient.getId(),String.format("%02d:%02d",dischargeTime/60,dischargeTime%60));
        assertThat(patients.findById(patient.getId()).orElseThrow().getStatus()).isEqualTo(PatientEntity.Status.DISCHARGED);
        assertThat(reservations.findById(reservation.getId()).orElseThrow().getStatus()).isEqualTo(BedReservationEntity.Status.ENDED);
        assertThat(schedules.findById(assignment.getId()).orElseThrow().getStatus()).isEqualTo(ScheduleEntity.Status.COMPLETED);
        assertThat(discharges.findByPatientId(patient.getId())).hasSize(1);
    }

    @Test
    @Order(4)
    void rerunsKeepOldAssignmentsAsSupersededHistory() {
        service.runScheduling();
        List<ScheduleEntity> firstRun=schedules.findByStatus(ScheduleEntity.Status.ACTIVE);
        assertThat(firstRun).isNotEmpty();
        service.runScheduling();
        assertThat(schedules.findByStatus(ScheduleEntity.Status.SUPERSEDED)).isNotEmpty();
        assertThat(schedules.findByStatus(ScheduleEntity.Status.ACTIVE)).isNotEmpty();
    }

    @Test
    @Order(1)
    @Transactional
    void failedBedAssignmentDoesNotPersistAnOrOrStaffReservationForEmergency() {
        PatientEntity anchorPatient=patients.findById(1).orElseThrow();
        HospitalEntity hospital=hospitals.findById(1).orElseThrow();
        OperatingRoomEntity room=rooms.findByHospitalId(1).stream()
                .filter(value->value.getSpecialties().contains(Patient.SurgeryType.GENERAL)).findFirst().orElseThrow();
        StaffEntity surgeon=staff.findByHospitalId(1).stream().filter(value->value.getSpecialties()
                .contains(Patient.SurgeryType.GENERAL)).findFirst().orElseThrow();
        ScheduleEntity anchor=schedules.saveAndFlush(new ScheduleEntity(anchorPatient,hospital,room,surgeon,
                480,510,ScheduleEntity.Status.COMPLETED,"TEST_HISTORY_ANCHOR"));
        for(HospitalEntity hospitalEntity:hospitals.findAll())
            for(BedEntity bed:beds.findByHospitalId(hospitalEntity.getId()))
                if(bed.getType()==models.Bed.BedType.ICU)
                    reservations.save(new BedReservationEntity(anchorPatient,bed,anchor,960,2400,
                            BedReservationEntity.Status.ACTIVE));
        reservations.flush();

        Patient emergency=service.addEmergency("Bed rollback test","GENERAL",30,true,"15:00");
        PatientEntity stored=patients.findById(emergency.getId()).orElseThrow();
        assertThat(stored.getStatus()).isEqualTo(PatientEntity.Status.PENDING);
        assertThat(schedules.findByPatientId(emergency.getId())).isEmpty();
        assertThat(reservations.findByPatientId(emergency.getId())).isEmpty();
        assertThat(schedules.findById(anchor.getId()).orElseThrow().getStatus()).isEqualTo(ScheduleEntity.Status.COMPLETED);
    }
}
