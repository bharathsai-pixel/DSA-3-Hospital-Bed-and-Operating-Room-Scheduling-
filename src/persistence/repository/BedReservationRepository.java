package persistence.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import persistence.entity.BedReservationEntity;
import java.util.List;
import java.util.Optional;
public interface BedReservationRepository extends JpaRepository<BedReservationEntity,Long> {
    List<BedReservationEntity> findByStatus(BedReservationEntity.Status status);
    List<BedReservationEntity> findByPatientId(Integer patientId);
    List<BedReservationEntity> findByBedIdOrderByStartTime(Integer bedId);
    Optional<BedReservationEntity> findFirstByScheduleId(Long scheduleId);
}
