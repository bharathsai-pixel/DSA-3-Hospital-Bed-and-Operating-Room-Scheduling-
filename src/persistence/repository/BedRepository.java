package persistence.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import persistence.entity.BedEntity;
import java.util.List;
public interface BedRepository extends JpaRepository<BedEntity,Integer> {
    List<BedEntity> findByHospitalId(Integer hospitalId);
}
