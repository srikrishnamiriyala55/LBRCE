package com.web.sms.service;

import com.web.sms.dto.request.CreateBusRequest;
import com.web.sms.dto.response.BoardingPointResponse;
import com.web.sms.dto.response.BusResponse;
import java.util.List;

public interface BusService {
    List<BusResponse> getAllActiveBuses();
    BusResponse getBusById(Long busId);
    BusResponse createBus(CreateBusRequest req);
    BusResponse updateBus(Long busId, CreateBusRequest req);
    List<BoardingPointResponse> getBoardingPointsByBusId(Long busId);
}
