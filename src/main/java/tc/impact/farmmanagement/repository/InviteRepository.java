package tc.impact.farmmanagement.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tc.impact.farmmanagement.entity.Invite;

import java.util.List;
import java.util.Optional;

@Repository
public interface InviteRepository extends JpaRepository<Invite, Long> {

    Optional<Invite> findByCode(String code);

    boolean existsByCode(String code);

    List<Invite> findByFarmIdOrderByCreatedAtDesc(Long farmId);

    List<Invite> findByFarmIdAndStatus(Long farmId, Invite.InviteStatus status);
}