package persistence.entity;

import jakarta.persistence.*;
import models.Patient;
import models.Staff;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name="staff", indexes=@Index(name="idx_staff_hospital", columnList="hospital_id"))
public class StaffEntity {
    @Id private Integer id;
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="hospital_id") private HospitalEntity hospital;
    @Column(nullable=false) private String name;
    @Enumerated(EnumType.STRING) private Staff.Role role;
    private int shiftStart;
    private int shiftEnd;
    @ElementCollection(targetClass=Patient.SurgeryType.class, fetch=FetchType.EAGER)
    @Enumerated(EnumType.STRING) @CollectionTable(name="staff_specialties", joinColumns=@JoinColumn(name="staff_id"))
    @Column(name="surgery_type") private List<Patient.SurgeryType> specialties = new ArrayList<>();
    protected StaffEntity() { }
    public StaffEntity(Integer id, HospitalEntity hospital, String name, Staff.Role role, int shiftStart, int shiftEnd,
                       List<Patient.SurgeryType> specialties){this.id=id;this.hospital=hospital;this.name=name;this.role=role;
        this.shiftStart=shiftStart;this.shiftEnd=shiftEnd;this.specialties=new ArrayList<>(specialties);}
    public Integer getId(){return id;} public HospitalEntity getHospital(){return hospital;} public String getName(){return name;}
    public Staff.Role getRole(){return role;} public int getShiftStart(){return shiftStart;} public int getShiftEnd(){return shiftEnd;}
    public List<Patient.SurgeryType> getSpecialties(){return specialties;}
}
