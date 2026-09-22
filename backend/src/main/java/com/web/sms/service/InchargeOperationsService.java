package com.web.sms.service;
import com.web.sms.dto.response.*;
import com.web.sms.entity.*;
import com.web.sms.enums.*;
import com.web.sms.repository.*;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

@Service
public class InchargeOperationsService {
 private final TransportAllocationRepository allocations; private final FeeRepository fees; private final BusPassRepository passes; private final BusApplicationRepository applications; private final TransferRequestRepository transfers; private final BoardingPointsRepository points;
 public InchargeOperationsService(TransportAllocationRepository a,FeeRepository f,BusPassRepository p,BusApplicationRepository apps,TransferRequestRepository t,BoardingPointsRepository bp){allocations=a;fees=f;passes=p;applications=apps;transfers=t;points=bp;}
 public DashboardResponse dashboard(Bus bus){DashboardResponse r=new DashboardResponse();long students=allocations.countByBusIdAndStatus(bus.getId(),EntityStatus.ACTIVE);r.put("busId",bus.getId());r.put("busNumber",bus.getBusNumber());r.put("startingPoint",bus.getStartingPoint());r.put("endingPoint",bus.getEndingPoint());r.put("busStatus",bus.getStatus());r.put("totalCapacity",bus.getTotalSeats());r.put("totalStudents",students);r.put("approvedStudents",applications.findByBusIdAndStatus(bus.getId(),ApplicationStatus.APPROVED).size());r.put("pendingApplications",applications.findByBusIdAndStatus(bus.getId(),ApplicationStatus.PENDING).size());r.put("rejectedApplications",applications.findByBusIdAndStatus(bus.getId(),ApplicationStatus.REJECTED).size());r.put("pendingTransferReleases",transfers.findByCurrentBusIdAndStatus(bus.getId(),TransferStatus.TRANSFER_REQUESTED,PageRequest.of(0,1)).getTotalElements());r.put("pendingTransferAcceptances",transfers.findByRequestedBusIdAndStatus(bus.getId(),TransferStatus.OLD_INCHARGE_APPROVED,PageRequest.of(0,1)).getTotalElements());r.put("occupancy",students);r.put("vacancy",Math.max(0,bus.getTotalSeats()-students));r.put("boardingPoints",points.findByBusIdOrderByOrderIndexAscStationNameAsc(bus.getId()).stream().map(BoardingPointResponse::fromBoardingPoint).toList());long required=fees.sumRequiredForActiveBus(bus.getId()),paid=fees.sumPaidForActiveBus(bus.getId());r.put("requiredFee",required);r.put("collectedFee",paid);r.put("remainingFee",required-paid);r.put("activePasses",passes.countByAllocationBusIdAndAllocationStatusAndStatus(bus.getId(),EntityStatus.ACTIVE,PassStatus.ACTIVE));return r;}
 public BusResponse bus(Bus b){return BusResponse.fromBus(b,(int)allocations.countByBusIdAndStatus(b.getId(),EntityStatus.ACTIVE));}
 public Page<InchargeStudentResponse> students(Bus bus,String search,String branch,Integer year,Long point,String payment,String pass,Pageable pageable){String s=blank(search),br=blank(branch),pay=blank(payment),ps=blank(pass);return allocations.searchForIncharge(bus.getId(),s,br,year,point,pay,ps,pageable).map(a->{Fee f=fees.findByStudentIdAndAcademicYearId(a.getStudent().getId(),a.getAcademicYear().getId()).orElse(null);BusPass p=passes.findByStudentIdAndAcademicYearIdAndStatus(a.getStudent().getId(),a.getAcademicYear().getId(),PassStatus.ACTIVE).orElse(null);return InchargeStudentResponse.from(a,f,p);});}
 private String blank(String s){return s==null||s.isBlank()?null:s.trim();}
}
