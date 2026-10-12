package persistence.entity;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "hospitals")
public class HospitalEntity {
    @Id private Integer id;
    @Column(nullable = false) private String name;
    private String location;
    private double latitude;
    private double longitude;
    @OneToMany(mappedBy = "hospital", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OperatingRoomEntity> operatingRooms = new ArrayList<>();
    @OneToMany(mappedBy = "hospital", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BedEntity> beds = new ArrayList<>();
    @OneToMany(mappedBy = "hospital", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<StaffEntity> staff = new ArrayList<>();

    protected HospitalEntity() { }
    public HospitalEntity(Integer id, String name, String location, double latitude, double longitude) {
        this.id=id; this.name=name; this.location=location; this.latitude=latitude; this.longitude=longitude;
    }
    public Integer getId(){return id;} public String getName(){return name;} public String getLocation(){return location;}
    public double getLatitude(){return latitude;} public double getLongitude(){return longitude;}
    public List<OperatingRoomEntity> getOperatingRooms(){return operatingRooms;}
    public List<BedEntity> getBeds(){return beds;} public List<StaffEntity> getStaff(){return staff;}
}
