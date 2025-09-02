package com.company.feedbackbot.repo;
import com.company.feedbackbot.domain.StaffProfile;
import lombok.NonNull;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface StaffProfileRepository extends JpaRepository<StaffProfile, Long> {
  Optional<StaffProfile> findByTelegramUserId(Long telegramUserId);
}
