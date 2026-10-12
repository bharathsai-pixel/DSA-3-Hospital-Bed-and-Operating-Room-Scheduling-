package persistence.entity;

import jakarta.persistence.*;
import models.Patient;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name="operating_rooms", indexes=@Index(name="idx_or_hospital", columnList="hospital_id"))
public class OperatingRoomEntity {
    @Id private Integer id;
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="hospital_id") private HospitalEntity hospital;
    @Column(nullable=false) private String name;
    private int openingTime;
    private int closingTime;
    @ElementCollection(targetClass=Patient.SurgeryType.class, fetch=FetchType.EAGER)
    @Enumerated(EnumType.STRING) @CollectionTable(name="operating_room_specialties", joinColumns=@JoinColumn(name="room_id"))
    @Column(name="surgery_type") private List<Patient.SurgeryType> specialties = new ArrayList<>();
    protected OperatingRoomEntity() { }
    public OperatingRoomEntity(Integer id, HospitalEntity hospital, String name, int openingTime, int closingTime,
                               List<Patient.SurgeryType> specialties) {
        this.id=id; this.hospital=hospital; this.name=name; this.openingTime=openingTime; this.closingTime=closingTime;
        this.specialties=new ArrayList<>(specialties);
    }
    public Integer getId(){return id;} public HospitalEntity getHospital(){return hospital;} public String getName(){return name;}
    public int getOpeningTime(){return openingTime;} public int getClosingTime(){return closingTime;}
    public List<Patient.SurgeryType> getSpecialties(){return specialties;}
}
