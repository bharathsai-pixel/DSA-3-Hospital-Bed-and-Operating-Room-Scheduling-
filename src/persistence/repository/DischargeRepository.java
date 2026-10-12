package persistence.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import persistence.entity.DischargeEntity;
import java.util.List;
public interface DischargeRepository extends JpaRepository<DischargeEntity,Long> {
    List<DischargeEntity> findByPatientId(Integer patientId);
}
