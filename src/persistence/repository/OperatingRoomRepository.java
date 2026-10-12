package persistence.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import persistence.entity.OperatingRoomEntity;
import java.util.List;
public interface OperatingRoomRepository extends JpaRepository<OperatingRoomEntity,Integer> {
    List<OperatingRoomEntity> findByHospitalId(Integer hospitalId);
}
