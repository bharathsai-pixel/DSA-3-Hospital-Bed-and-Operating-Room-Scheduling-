package persistence.entity;

import jakarta.persistence.*;

@Entity
@Table(name="discharge_records", indexes=@Index(name="idx_discharge_patient",columnList="patient_id"))
public class DischargeEntity {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="patient_id") private PatientEntity patient;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="bed_reservation_id") private BedReservationEntity bedReservation;
    private int dischargeTime;
    protected DischargeEntity() { }
    public DischargeEntity(PatientEntity patient,BedReservationEntity reservation,int dischargeTime){this.patient=patient;this.bedReservation=reservation;this.dischargeTime=dischargeTime;}
    public Long getId(){return id;} public PatientEntity getPatient(){return patient;}
    public BedReservationEntity getBedReservation(){return bedReservation;} public int getDischargeTime(){return dischargeTime;}
}
