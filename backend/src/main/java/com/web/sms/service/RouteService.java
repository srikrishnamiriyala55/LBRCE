package com.web.sms.service;

import com.web.sms.dto.request.CreateRouteRequest;
import com.web.sms.enums.EntityStatus;
import com.web.sms.entity.Route;
import com.web.sms.exception.BadRequestException;
import com.web.sms.exception.ResourceNotFoundException;
import com.web.sms.repository.RouteRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class RouteService {

    private final RouteRepository routeRepo;

    public RouteService(RouteRepository routeRepo) {
        this.routeRepo = routeRepo;
    }

    public List<Route> getAllActiveRoutes() {
        return routeRepo.findByStatus(EntityStatus.ACTIVE);
    }

    public Route createRoute(CreateRouteRequest req) {
        if (routeRepo.existsByRouteName(req.getRouteName())) {
            throw new BadRequestException("Route with name " + req.getRouteName() + " already exists");
        }

        Route r = new Route();
        r.setRouteName(req.getRouteName().trim());
        r.setStartingPoint(req.getStartingPoint().trim());
        r.setEndingPoint(req.getEndingPoint().trim());
        r.setDescription(req.getDescription());
        r.setStatus(EntityStatus.ACTIVE);
        r.setCreatedAt(LocalDateTime.now());
        return routeRepo.save(r);
    }

    public Route updateRoute(Long id, CreateRouteRequest req) {
        Route r = routeRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Route not found with id: " + id));

        routeRepo.findByRouteName(req.getRouteName().trim())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> { throw new BadRequestException("Route name already exists"); });

        r.setRouteName(req.getRouteName().trim());
        r.setStartingPoint(req.getStartingPoint().trim());
        r.setEndingPoint(req.getEndingPoint().trim());
        r.setDescription(req.getDescription());
        r.setUpdatedAt(LocalDateTime.now());
        return routeRepo.save(r);
    }
}
