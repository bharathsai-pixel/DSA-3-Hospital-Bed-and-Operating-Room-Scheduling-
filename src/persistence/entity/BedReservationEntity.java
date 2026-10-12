package persistence.entity;

import jakarta.persistence.*;

@Entity
@Table(name="bed_reservations", indexes={@Index(name="idx_reservation_bed_time",columnList="bed_id,start_time,end_time"),
        @Index(name="idx_reservation_patient",columnList="patient_id")})
public class BedReservationEntity {
    public enum Status { ACTIVE, ENDED, CANCELLED, SUPERSEDED }
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="patient_id") private PatientEntity patient;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="bed_id") private BedEntity bed;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="schedule_id") private ScheduleEntity schedule;
    private int startTime;
    private int endTime;
    @Enumerated(EnumType.STRING) private Status status;
    protected BedReservationEntity() { }
    public BedReservationEntity(PatientEntity patient,BedEntity bed,ScheduleEntity schedule,int startTime,int endTime,Status status){
        this.patient=patient;this.bed=bed;this.schedule=schedule;this.startTime=startTime;this.endTime=endTime;this.status=status;}
    public Long getId(){return id;} public PatientEntity getPatient(){return patient;} public BedEntity getBed(){return bed;}
    public ScheduleEntity getSchedule(){return schedule;} public int getStartTime(){return startTime;} public int getEndTime(){return endTime;}
    public Status getStatus(){return status;} public void setStatus(Status status){this.status=status;} public void setEndTime(int endTime){this.endTime=endTime;}
}
