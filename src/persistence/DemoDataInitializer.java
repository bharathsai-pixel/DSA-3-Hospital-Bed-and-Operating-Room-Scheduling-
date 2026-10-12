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
        var existingHospitals=hospitals.findAll();
        var existingPatients=patients.findAll();
        var samplePatients=SampleData.getPatients();
        if(existingHospitals.isEmpty() && existingPatients.isEmpty()) {
            hospitals.saveAll(SampleData.getHospitals().stream().map(mapper::toEntity).toList());
            patients.saveAll(samplePatients.stream()
                .map(patient->mapper.toEntity(patient,PatientEntity.Status.PENDING)).toList());
            return;
        }

        // Upgrade only the untouched original 12-patient demo database; never append fixtures to user data.
        boolean originalDemoPatients=existingHospitals.size()>0 && existingPatients.size()==12
                && existingPatients.stream().allMatch(saved -> saved.getId()>=1 && saved.getId()<=12
                && samplePatients.get(saved.getId()-1).getName().equals(saved.getName()));
        if(originalDemoPatients) patients.saveAll(samplePatients.subList(12,samplePatients.size()).stream()
                .map(patient->mapper.toEntity(patient,PatientEntity.Status.PENDING)).toList());
    }
}
