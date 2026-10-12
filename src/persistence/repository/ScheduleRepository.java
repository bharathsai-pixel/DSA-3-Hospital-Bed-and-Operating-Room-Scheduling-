package persistence.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import persistence.entity.ScheduleEntity;
import java.util.List;
public interface ScheduleRepository extends JpaRepository<ScheduleEntity,Long> {
    List<ScheduleEntity> findByStatus(ScheduleEntity.Status status);
    List<ScheduleEntity> findByOperatingRoomIdOrderByStartTime(Integer operatingRoomId);
    List<ScheduleEntity> findByPatientId(Integer patientId);
}
