package persistence.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import persistence.entity.StaffEntity;
import java.util.List;
public interface StaffRepository extends JpaRepository<StaffEntity,Integer> {
    List<StaffEntity> findByHospitalId(Integer hospitalId);
}
