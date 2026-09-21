package com.web.sms.repository;

import com.web.sms.entity.OtpChallenge;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface OtpChallengeRepository extends JpaRepository<OtpChallenge, String> {
    Optional<OtpChallenge> findTopByPrincipalIdAndPurposeAndConsumedFalseOrderByCreatedAtDesc(String principalId, String purpose);
}
