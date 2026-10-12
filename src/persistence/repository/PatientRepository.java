package persistence.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import persistence.entity.PatientEntity;
import java.util.List;
public interface PatientRepository extends JpaRepository<PatientEntity,Integer> {
    List<PatientEntity> findByStatus(PatientEntity.Status status);
}
