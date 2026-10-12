package persistence.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import persistence.entity.HospitalEntity;
public interface HospitalRepository extends JpaRepository<HospitalEntity,Integer> { }
