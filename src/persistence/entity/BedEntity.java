package persistence.entity;

import jakarta.persistence.*;
import models.Bed;

@Entity
@Table(name="beds", indexes=@Index(name="idx_bed_hospital", columnList="hospital_id"))
public class BedEntity {
    @Id private Integer id;
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="hospital_id") private HospitalEntity hospital;
    @Enumerated(EnumType.STRING) @Column(nullable=false) private Bed.BedType type;
    protected BedEntity() { }
    public BedEntity(Integer id, HospitalEntity hospital, Bed.BedType type){this.id=id;this.hospital=hospital;this.type=type;}
    public Integer getId(){return id;} public HospitalEntity getHospital(){return hospital;} public Bed.BedType getType(){return type;}
}
