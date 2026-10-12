package persistence.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name="schedule_assignments", indexes={
        @Index(name="idx_schedule_patient",columnList="patient_id"),
        @Index(name="idx_schedule_room_time",columnList="operating_room_id,start_time,end_time"),
        @Index(name="idx_schedule_staff_time",columnList="staff_id,start_time,end_time")})
public class ScheduleEntity {
    public enum Status { ACTIVE, SUPERSEDED, COMPLETED, CANCELLED }
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="patient_id") private PatientEntity patient;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="hospital_id") private HospitalEntity hospital;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="operating_room_id") private OperatingRoomEntity operatingRoom;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="staff_id") private StaffEntity staff;
    private int startTime;
    private int endTime;
    @Enumerated(EnumType.STRING) @Column(nullable=false) private Status status;
    @Column(nullable=false) private String algorithmPath;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    protected ScheduleEntity() { }
    public ScheduleEntity(PatientEntity patient,HospitalEntity hospital,OperatingRoomEntity room,StaffEntity staff,
                          int startTime,int endTime,Status status,String algorithmPath){this.patient=patient;this.hospital=hospital;
        this.operatingRoom=room;this.staff=staff;this.startTime=startTime;this.endTime=endTime;this.status=status;
        this.algorithmPath=algorithmPath;this.createdAt=LocalDateTime.now();this.updatedAt=this.createdAt;}
    @PreUpdate void touch(){updatedAt=LocalDateTime.now();}
    public Long getId(){return id;} public PatientEntity getPatient(){return patient;} public HospitalEntity getHospital(){return hospital;}
    public OperatingRoomEntity getOperatingRoom(){return operatingRoom;} public StaffEntity getStaff(){return staff;}
    public int getStartTime(){return startTime;} public int getEndTime(){return endTime;} public Status getStatus(){return status;}
    public String getAlgorithmPath(){return algorithmPath;} public void setStatus(Status status){this.status=status;}
}
