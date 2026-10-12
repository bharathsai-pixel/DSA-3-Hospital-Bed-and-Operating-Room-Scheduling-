package persistence;

import data.SampleData;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import persistence.entity.PatientEntity;
import persistence.repository.HospitalRepository;
import persistence.repository.PatientRepository;

/** Idempotent small development dataset: existing state is never reseeded or overwritten. */
@Component
public class DemoDataInitializer implements ApplicationRunner {
    private final HospitalRepository hospitals;
    private final PatientRepository patients;
    private final DomainModelMapper mapper;
    public DemoDataInitializer(HospitalRepository hospitals,PatientRepository patients,DomainModelMapper mapper){
        this.hospitals=hospitals;this.patients=patients;this.mapper=mapper;
    }
    @Override @Transactional
    public void run(ApplicationArguments args){
        if(hospitals.count()!=0 || patients.count()!=0) return;
        hospitals.saveAll(SampleData.getHospitals().stream().map(mapper::toEntity).toList());
        patients.saveAll(SampleData.getPatients().stream()
                .map(patient->mapper.toEntity(patient,PatientEntity.Status.PENDING)).toList());
    }
}
