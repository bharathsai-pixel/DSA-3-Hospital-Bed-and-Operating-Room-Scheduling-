package persistence.entity;

import jakarta.persistence.*;
import models.Patient;

@Entity
@Table(name="patients", indexes=@Index(name="idx_patient_status", columnList="status"))
public class PatientEntity {
    public enum Status { PENDING, ROUTED, SCHEDULED, DISCHARGED }
    @Id private Integer id;
    @Column(nullable=false) private String name;
    private int age;
    @Enumerated(EnumType.STRING) private Patient.Priority priority;
    @Enumerated(EnumType.STRING) private Patient.SurgeryType surgeryType;
    private int surgeryDuration;
    private int arrivalTime;
    private int preferredHospitalId;
    private boolean requiresIcu;
    private double latitude;
    private double longitude;
    @Enumerated(EnumType.STRING) @Column(nullable=false) private Status status=Status.PENDING;
    private int dischargeTime=-1;
    protected PatientEntity() { }
    public PatientEntity(Integer id,String name,int age,Patient.Priority priority,Patient.SurgeryType surgeryType,
                         int surgeryDuration,int arrivalTime,int preferredHospitalId,boolean requiresIcu,
                         double latitude,double longitude,Status status,int dischargeTime){this.id=id;this.name=name;this.age=age;
        this.priority=priority;this.surgeryType=surgeryType;this.surgeryDuration=surgeryDuration;this.arrivalTime=arrivalTime;
        this.preferredHospitalId=preferredHospitalId;this.requiresIcu=requiresIcu;this.latitude=latitude;this.longitude=longitude;this.status=status;this.dischargeTime=dischargeTime;}
    public Integer getId(){return id;} public String getName(){return name;} public int getAge(){return age;}
    public Patient.Priority getPriority(){return priority;} public Patient.SurgeryType getSurgeryType(){return surgeryType;}
    public int getSurgeryDuration(){return surgeryDuration;} public int getArrivalTime(){return arrivalTime;}
    public int getPreferredHospitalId(){return preferredHospitalId;} public boolean isRequiresIcu(){return requiresIcu;}
    public double getLatitude(){return latitude;} public double getLongitude(){return longitude;} public Status getStatus(){return status;}
    public int getDischargeTime(){return dischargeTime;} public void setStatus(Status status){this.status=status;}
    public void setDischargeTime(int dischargeTime){this.dischargeTime=dischargeTime;}
}
