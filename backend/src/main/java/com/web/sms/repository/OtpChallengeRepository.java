package com.web.sms.repository;

import com.web.sms.entity.OtpChallenge;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface OtpChallengeRepository extends JpaRepository<OtpChallenge, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from OtpChallenge o where o.email=:email and o.purpose=:purpose")
    Optional<OtpChallenge> findForUpdate(String email, String purpose);
}
