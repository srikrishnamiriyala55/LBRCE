package com.web.sms.repository;
import com.web.sms.entity.Route;
import com.web.sms.enums.EntityStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface RouteRepository extends JpaRepository<Route, Long> {
    Optional<Route> findByRouteName(String routeName);
    List<Route> findByStatus(EntityStatus status);
    boolean existsByRouteName(String routeName);
}
