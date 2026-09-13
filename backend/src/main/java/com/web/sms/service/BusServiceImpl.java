package com.web.sms.service;

import com.web.sms.dto.request.CreateBusRequest;
import com.web.sms.dto.response.BoardingPointResponse;
import com.web.sms.dto.response.BusResponse;
import com.web.sms.entity.Bus;
import com.web.sms.entity.Route;
import com.web.sms.enums.EntityStatus;
import com.web.sms.exception.ResourceNotFoundException;
import com.web.sms.exception.BadRequestException;
import com.web.sms.repository.BoardingPointsRepository;
import com.web.sms.repository.BusRepository;
import com.web.sms.repository.RouteRepository;
import com.web.sms.repository.TransportAllocationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class BusServiceImpl implements BusService {

    private final BusRepository busRepository;
    private final BoardingPointsRepository boardingPointsRepository;
    private final RouteRepository routeRepository;
    private final TransportAllocationRepository transportAllocationRepository;

    public BusServiceImpl(BusRepository busRepository,
                          BoardingPointsRepository boardingPointsRepository,
                          RouteRepository routeRepository,
                          TransportAllocationRepository transportAllocationRepository) {
        this.busRepository = busRepository;
        this.boardingPointsRepository = boardingPointsRepository;
        this.routeRepository = routeRepository;
        this.transportAllocationRepository = transportAllocationRepository;
    }

    @Override
    public List<BusResponse> getAllActiveBuses() {
        return busRepository.findByStatus(EntityStatus.ACTIVE).stream()
                .map(bus -> {
                    long occupied = transportAllocationRepository.countByBusIdAndStatus(bus.getId(), EntityStatus.ACTIVE);
                    return BusResponse.fromBus(bus, (int) occupied);
                })
                .collect(Collectors.toList());
    }

    @Override
    public BusResponse getBusById(Long busId) {
        Bus bus = busRepository.findById(busId)
                .orElseThrow(() -> new ResourceNotFoundException("Bus not found with id: " + busId));
        long occupied = transportAllocationRepository.countByBusIdAndStatus(bus.getId(), EntityStatus.ACTIVE);
        return BusResponse.fromBus(bus, (int) occupied);
    }

    @Override
    @Transactional
    public BusResponse createBus(CreateBusRequest req) {
        String busNumber=req.getBusNumber().trim().toUpperCase();
        if (busRepository.existsByBusNumber(busNumber)) {
            throw new BadRequestException("Bus number already exists");
        }
        Bus bus = new Bus();
        bus.setBusNumber(busNumber);
        bus.setTotalSeats(req.getTotalSeats());
        bus.setAvailableSeats(req.getTotalSeats());
        bus.setStartingPoint(req.getStartingPoint());
        bus.setEndingPoint(req.getEndingPoint());
        bus.setStatus(EntityStatus.ACTIVE);
        bus.setCreatedAt(LocalDateTime.now());

        if (req.getRouteId() != null) {
            Route route = routeRepository.findById(req.getRouteId())
                    .orElseThrow(() -> new ResourceNotFoundException("Route not found with id: " + req.getRouteId()));
            if (route.getStatus() != EntityStatus.ACTIVE) throw new BadRequestException("Only an active route can be assigned to a bus");
            bus.setRoute(route);
        }

        Bus saved = busRepository.save(bus);
        return BusResponse.fromBus(saved, 0);
    }

    @Override
    @Transactional
    public BusResponse updateBus(Long busId, CreateBusRequest req) {
        Bus bus = busRepository.findByIdForUpdate(busId)
                .orElseThrow(() -> new ResourceNotFoundException("Bus not found with id: " + busId));

        if (req.getBusNumber() != null && busRepository.findByBusNumber(req.getBusNumber().trim().toUpperCase())
                .filter(existing -> !existing.getId().equals(busId)).isPresent()) {
            throw new BadRequestException("Bus number already exists");
        }

        if (req.getBusNumber() != null) bus.setBusNumber(req.getBusNumber().trim().toUpperCase());
        if (req.getTotalSeats() != null) {
            long occupied = transportAllocationRepository.countByBusIdAndStatus(busId, EntityStatus.ACTIVE);
            if (req.getTotalSeats() < occupied) {
                throw new BadRequestException("Capacity cannot be lower than the current occupied seat count");
            }
            bus.setTotalSeats(req.getTotalSeats());
            bus.setAvailableSeats(req.getTotalSeats() - (int) occupied);
        }
        if (req.getStartingPoint() != null) bus.setStartingPoint(req.getStartingPoint());
        if (req.getEndingPoint() != null) bus.setEndingPoint(req.getEndingPoint());

        if (req.getRouteId() != null) {
            Route route = routeRepository.findById(req.getRouteId())
                    .orElseThrow(() -> new ResourceNotFoundException("Route not found with id: " + req.getRouteId()));
            if (route.getStatus() != EntityStatus.ACTIVE) throw new BadRequestException("Only an active route can be assigned to a bus");
            bus.setRoute(route);
        }

        bus.setUpdatedAt(LocalDateTime.now());
        Bus saved = busRepository.save(bus);
        long occupied = transportAllocationRepository.countByBusIdAndStatus(saved.getId(), EntityStatus.ACTIVE);
        return BusResponse.fromBus(saved, (int) occupied);
    }

    @Override
    public List<BoardingPointResponse> getBoardingPointsByBusId(Long busId) {
        return boardingPointsRepository.findByBusIdAndStatus(busId, EntityStatus.ACTIVE).stream()
                .map(BoardingPointResponse::fromBoardingPoint)
                .collect(Collectors.toList());
    }
}
