package com.web.sms.service;

import com.web.sms.dto.request.CreateInchargeRequest;
import com.web.sms.entity.Bus;
import com.web.sms.entity.Incharge;
import java.util.List;

public interface InchargeService {
    Incharge getInchargeById(long id);
    Incharge getInchargeByTeacherId(String teacherId);
    List<Incharge> getAllIncharges();
    Incharge createIncharge(CreateInchargeRequest req);
    Incharge updateIncharge(Long id, CreateInchargeRequest req);
    Bus getAssignedBus(Long inchargeId);
}
