package com.web.sms.service;

import com.web.sms.dto.request.TransferRequestDto;
import com.web.sms.dto.response.TransferResponse;
import com.web.sms.entity.*;
import com.web.sms.enums.*;
import com.web.sms.exception.*;
import com.web.sms.repository.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class TransferService {
 private static final List<TransferStatus> ACTIVE=List.of(TransferStatus.TRANSFER_REQUESTED,TransferStatus.OLD_INCHARGE_APPROVED,TransferStatus.NEW_INCHARGE_APPROVED);
 private final TransferRequestRepository transfers; private final TransportAllocationRepository allocations;
 private final BusRepository buses; private final BoardingPointsRepository points; private final AcademicYearRepository years;
 private final BusPassRepository passes; private final FeeRepository fees; private final NotificationService notifications; private final AuditService audits;
 public TransferService(TransferRequestRepository t,TransportAllocationRepository a,BusRepository b,BoardingPointsRepository p,AcademicYearRepository y,BusPassRepository bp,FeeRepository f,NotificationService n,AuditService au){transfers=t;allocations=a;buses=b;points=p;years=y;passes=bp;fees=f;notifications=n;audits=au;}

 @Transactional public TransferResponse submitTransfer(Long studentId,TransferRequestDto req){
  AcademicYear year=years.findByActiveTrue().orElseThrow(()->new BadRequestException("No active academic year found"));
  TransportAllocation current=allocations.findActiveForUpdate(studentId,year.getId()).orElseThrow(()->new BadRequestException("Transfer requires an active approved bus assignment"));
  if(!"ACTIVE".equalsIgnoreCase(current.getStudent().getStatus()))throw new BadRequestException("Only active students can request a transfer");
  if(!transfers.findByStudentIdAndAcademicYearIdAndStatusIn(studentId,year.getId(),ACTIVE).isEmpty()) throw new BadRequestException("You already have an active transfer request");
  long usedTransfers=transfers.countByStudentIdAndAcademicYearIdAndStatusNot(studentId,year.getId(),TransferStatus.CANCELLED);
  if(usedTransfers>=2) throw new BadRequestException("You have reached the maximum of 2 bus transfer requests allowed per academic year");
  Bus requested=buses.findById(req.getRequestedBusId()).orElseThrow(()->new ResourceNotFoundException("Requested bus not found"));
  if(current.getBus().getId().equals(requested.getId())) throw new BadRequestException("Requested bus is your current bus");
  if(current.getBus().getIncharge()==null||requested.getIncharge()==null) throw new BadRequestException("Both buses must have an in-charge");
  ensureCapacity(requested);
  BoardingPoints point=points.findById(req.getRequestedBoardingPointId()).orElseThrow(()->new ResourceNotFoundException("Requested boarding point not found"));
  if(point.getBus()==null||!point.getBus().getId().equals(requested.getId())) throw new BadRequestException("Requested boarding point does not belong to the requested bus");
  LocalDateTime now=LocalDateTime.now(); TransferRequest t=new TransferRequest();
  t.setStudent(current.getStudent());t.setCurrentBus(current.getBus());t.setCurrentBoardingPoint(current.getBoardingPoint());t.setRequestedBus(requested);t.setRequestedBoardingPoint(point);t.setAcademicYear(year);
  t.setOldIncharge(current.getBus().getIncharge());t.setNewIncharge(requested.getIncharge());t.setReason(req.getReason().trim());t.setStatus(TransferStatus.TRANSFER_REQUESTED);t.setRequestedAt(now);t.setCreatedAt(now);
  try{t=transfers.saveAndFlush(t);}catch(DataIntegrityViolationException e){throw new BadRequestException("You already have an active transfer request");}
  notify(t,"Transfer Request Submitted","Your bus transfer request has been submitted. Waiting for approval from your current bus In-Charge."); audit(t,"STUDENT",t.getStudent().getRollNumber(),null,t.getStatus()); return TransferResponse.fromTransfer(t);
  
 }
 public List<TransferResponse> getStudentTransfers(Long id){return transfers.findByStudentId(id).stream().map(TransferResponse::fromTransfer).toList();}
 public TransferResponse getStudentTransfer(Long sid,Long id){TransferRequest t=transfers.findById(id).orElseThrow(()->new ResourceNotFoundException("Transfer request not found"));if(!t.getStudent().getId().equals(sid))throw new ForbiddenException("This transfer request is not yours");return TransferResponse.fromTransfer(t);}
 public Page<TransferResponse> getTransfersByBus(Long id,Pageable p){return transfers.findByCurrentBusIdOrRequestedBusId(id,id,p).map(TransferResponse::fromTransfer);}
 public Page<TransferResponse> getPendingRelease(Long id,Pageable p){return transfers.findByCurrentBusIdAndStatus(id,TransferStatus.TRANSFER_REQUESTED,p).map(TransferResponse::fromTransfer);}
 public Page<TransferResponse> getPendingAcceptance(Long id,Pageable p){return transfers.findByRequestedBusIdAndStatus(id,TransferStatus.OLD_INCHARGE_APPROVED,p).map(TransferResponse::fromTransfer);}

 @Transactional public TransferResponse approveRelease(Long id,Long inchargeId,String actor){TransferRequest t=locked(id);stage(t,TransferStatus.TRANSFER_REQUESTED);owner(t.getOldIncharge(),inchargeId,"current bus");TransferStatus old=t.getStatus();t.setStatus(TransferStatus.OLD_INCHARGE_APPROVED);t.setOldInchargeDecisionAt(LocalDateTime.now());t.setUpdatedAt(LocalDateTime.now());transfers.save(t);notify(t,"Current In-Charge Approved","Your current bus In-Charge has approved your transfer request. The request has been sent to the new bus In-Charge.");notifications.createNotification(t.getNewIncharge().getTeacherId(),"Transfer Acceptance Required","A transfer request approved by the old In-Charge is waiting for your decision.","TRANSFER");audit(t,"INCHARGE",actor,old,t.getStatus());return TransferResponse.fromTransfer(t);}
 @Transactional public TransferResponse rejectRelease(Long id,Long iid,String actor,String remarks){TransferRequest t=locked(id);stage(t,TransferStatus.TRANSFER_REQUESTED);owner(t.getOldIncharge(),iid,"current bus");TransferStatus old=t.getStatus();t.setStatus(TransferStatus.OLD_INCHARGE_REJECTED);t.setOldInchargeDecisionAt(LocalDateTime.now());t.setOldInchargeRemarks(remarks);finishDecision(t,remarks);notify(t,"Transfer Request Rejected","Your bus transfer request was rejected by your current bus In-Charge.");audit(t,"INCHARGE",actor,old,t.getStatus());return TransferResponse.fromTransfer(t);}
 @Transactional public TransferResponse rejectAcceptance(Long id,Long iid,String actor,String remarks){TransferRequest t=locked(id);stage(t,TransferStatus.OLD_INCHARGE_APPROVED);owner(t.getNewIncharge(),iid,"requested bus");TransferStatus old=t.getStatus();t.setStatus(TransferStatus.NEW_INCHARGE_REJECTED);t.setNewInchargeDecisionAt(LocalDateTime.now());t.setNewInchargeRemarks(remarks);finishDecision(t,remarks);notify(t,"Transfer Request Rejected","Your requested bus transfer was rejected by the new bus In-Charge. Your existing bus assignment remains unchanged.");audit(t,"INCHARGE",actor,old,t.getStatus());return TransferResponse.fromTransfer(t);}

 @Transactional public TransferResponse acceptTransfer(Long id,Long iid,String actor){TransferRequest t=locked(id);if(t.getStatus()==TransferStatus.TRANSFER_COMPLETED)return TransferResponse.fromTransfer(t);stage(t,TransferStatus.OLD_INCHARGE_APPROVED);owner(t.getNewIncharge(),iid,"requested bus");return completeTransfer(t,actor,false);}

 @Transactional public TransferResponse approveByAdmin(Long id,String actor){TransferRequest t=locked(id);if(t.getStatus()==TransferStatus.TRANSFER_COMPLETED)return TransferResponse.fromTransfer(t);if(t.getStatus()!=TransferStatus.TRANSFER_REQUESTED&&t.getStatus()!=TransferStatus.OLD_INCHARGE_APPROVED)throw new BadRequestException("Only an active transfer request can be approved directly");return completeTransfer(t,actor,true);}

 private TransferResponse completeTransfer(TransferRequest t,String actor,boolean adminApproval){
  TransferStatus startingStatus=t.getStatus();
  TransportAllocation old=allocations.findActiveForUpdate(t.getStudent().getId(),t.getAcademicYear().getId()).orElseThrow(()->new BadRequestException("Original active assignment no longer exists"));
  if(!old.getBus().getId().equals(t.getCurrentBus().getId()))throw new BadRequestException("Student assignment changed after submission");
  if(t.getRequestedBoardingPoint().getBus()==null||!t.getRequestedBoardingPoint().getBus().getId().equals(t.getRequestedBus().getId())||t.getRequestedBoardingPoint().getStatus()!=EntityStatus.ACTIVE)throw new BadRequestException("Requested boarding point is no longer valid");Bus requestedLocked=buses.findByIdForUpdate(t.getRequestedBus().getId()).orElseThrow(()->new ResourceNotFoundException("Requested bus not found"));ensureCapacity(requestedLocked);
  LocalDateTime now=LocalDateTime.now();t.setStatus(TransferStatus.NEW_INCHARGE_APPROVED);t.setNewInchargeDecisionAt(now);if(adminApproval){if(t.getOldInchargeDecisionAt()==null)t.setOldInchargeDecisionAt(now);t.setRemarks("Approved directly by Administrator after vacancy verification");}transfers.save(t);if(!adminApproval)audit(t,"INCHARGE",actor,TransferStatus.OLD_INCHARGE_APPROVED,t.getStatus());
  old.setStatus(EntityStatus.INACTIVE);old.setDeactivatedAt(now);allocations.saveAndFlush(old);TransportAllocation next=new TransportAllocation();next.setStudent(t.getStudent());next.setBus(requestedLocked);next.setBoardingPoint(t.getRequestedBoardingPoint());next.setAcademicYear(t.getAcademicYear());next.setStatus(EntityStatus.ACTIVE);next.setAllocatedAt(now);next.setCreatedAt(now);captureSnapshot(next,old,t,requestedLocked);allocations.saveAndFlush(next);
  recalc(old.getBus());recalc(t.getRequestedBus());
  Fee fee=fees.findByStudentIdAndAcademicYearId(t.getStudent().getId(),t.getAcademicYear().getId()).orElseThrow(()->new BadRequestException("Student fee record was not found"));long total=t.getRequestedBoardingPoint().getFeeAmount()==null?0L:t.getRequestedBoardingPoint().getFeeAmount();if(total<=0)throw new BadRequestException("Requested boarding point does not have a valid fee");long paid=fee.getPaidAmount()==null?0L:fee.getPaidAmount();fee.setAllocation(next);fee.setTotalAmount(total);fee.setStatus(paid>=total?"PAID":paid>0?"PARTIALLY_PAID":"PENDING");fee.setPassEligible(paid*2>=total);fee.setUpdatedAt(now);fees.save(fee);
  Optional<BusPass> previousPass=passes.findByStudentIdAndAcademicYearIdAndStatus(t.getStudent().getId(),t.getAcademicYear().getId(),PassStatus.ACTIVE);previousPass.ifPresent(p->{p.setStatus(PassStatus.REVOKED);p.setRevokedAt(now);p.setUpdatedAt(now);passes.save(p);});
  if(fee.isPassEligible()){
   BusPass replacement=new BusPass();
   replacement.setStudent(t.getStudent());
   replacement.setAllocation(next);
   replacement.setAcademicYear(t.getAcademicYear());
   // FIX: Always generate a fresh pass number. Appending to old pass number caused chained suffixes
   // (e.g. PASS-T5-T12-T23) which violate the UNIQUE constraint on pass_number after multiple transfers.
   String yearName=t.getAcademicYear().getYearName().replace("-","");
   replacement.setPassNumber("LBRCE-PASS-"+yearName+"-"+t.getStudent().getRollNumber()+"-T"+t.getId());
   replacement.setVerificationToken(UUID.randomUUID().toString());
   replacement.setStatus(PassStatus.ACTIVE);
   replacement.setValidFrom(previousPass.map(BusPass::getValidFrom).orElse(LocalDate.now()));
   replacement.setValidUntil(previousPass.map(BusPass::getValidUntil).orElse(t.getAcademicYear().getEndDate()!=null?t.getAcademicYear().getEndDate():LocalDate.now().plusMonths(10)));
   replacement.setGeneratedAt(now);
   replacement.setCreatedAt(now);
   passes.save(replacement);
  }
  t.setStatus(TransferStatus.TRANSFER_COMPLETED);t.setCompletedAt(now);t.setProcessedAt(now);t.setUpdatedAt(now);transfers.save(t);audit(t,adminApproval?"ADMIN":"SYSTEM",actor,adminApproval?startingStatus:TransferStatus.NEW_INCHARGE_APPROVED,t.getStatus());notify(t,"Bus Transfer Completed",adminApproval?"Your bus transfer has been approved directly by the Transport Administrator after verifying vacancy.":"Your bus transfer has been approved. Your transportation assignment has been changed successfully.");return TransferResponse.fromTransfer(t);
 }
 @Transactional public void cancel(Long sid,Long id){TransferRequest t=locked(id);if(!t.getStudent().getId().equals(sid))throw new ForbiddenException("This transfer request is not yours");stage(t,TransferStatus.TRANSFER_REQUESTED);TransferStatus old=t.getStatus();t.setStatus(TransferStatus.CANCELLED);finishDecision(t,"Cancelled by student");audit(t,"STUDENT",t.getStudent().getRollNumber(),old,t.getStatus());}
 private TransferRequest locked(Long id){
  TransferRequest request=transfers.findByIdForUpdate(id).orElseThrow(()->new ResourceNotFoundException("Transfer request not found"));
  AcademicYear active=years.findByActiveTrue().orElseThrow(()->new BadRequestException("No active academic year is configured"));
  if(request.getAcademicYear()==null||!request.getAcademicYear().getId().equals(active.getId()))throw new BadRequestException("Historical transfer requests are read-only. Create a request for the active academic year.");
  return request;
 }
 private void stage(TransferRequest t,TransferStatus s){if(t.getStatus()!=s)throw new BadRequestException("Invalid transfer transition from "+t.getStatus());}
 private void owner(Incharge expected,Long actual,String label){if(expected==null||!expected.getId().equals(actual))throw new ForbiddenException("Only the "+label+" in-charge can perform this action");}
 private void ensureCapacity(Bus b){AcademicYear active=years.findByActiveTrue().orElseThrow(()->new BadRequestException("No active academic year is configured"));if(b.getStatus()!=EntityStatus.ACTIVE||allocations.countByBusIdAndAcademicYearIdAndStatus(b.getId(),active.getId(),EntityStatus.ACTIVE)>=b.getTotalSeats())throw new BadRequestException("Requested bus has no available seats");}
 private void recalc(Bus b){b.setAvailableSeats(Math.max(0,b.getTotalSeats()-(int)allocations.countByBusIdAndStatus(b.getId(),EntityStatus.ACTIVE)));buses.save(b);}
 private void captureSnapshot(TransportAllocation next,TransportAllocation old,TransferRequest t,Bus bus){
  Student s=t.getStudent();BoardingPoints p=t.getRequestedBoardingPoint();
  next.setStudentNameSnapshot(s.getName());next.setRollNumberSnapshot(s.getRollNumber());next.setPhoneNumberSnapshot(s.getPhoneNumber());
  next.setBranchSnapshot(old.getBranchSnapshot()!=null?old.getBranchSnapshot():s.getBranch());next.setStudyYearSnapshot(old.getStudyYearSnapshot()!=null?old.getStudyYearSnapshot():s.getYear());next.setSemesterSnapshot(old.getSemesterSnapshot()!=null?old.getSemesterSnapshot():s.getSemester());
  next.setBusNumberSnapshot(bus.getBusNumber());next.setStartingPointSnapshot(bus.getStartingPoint());next.setEndingPointSnapshot(bus.getEndingPoint());next.setBoardingPointSnapshot(p.getStationName());next.setAnnualFeeSnapshot(p.getFeeAmount());
  if(bus.getIncharge()!=null){next.setInchargeNameSnapshot(bus.getIncharge().getName());next.setInchargeTeacherIdSnapshot(bus.getIncharge().getTeacherId());}
 }
 private void finishDecision(TransferRequest t,String remarks){t.setRemarks(remarks);t.setProcessedAt(LocalDateTime.now());t.setUpdatedAt(LocalDateTime.now());transfers.save(t);}
 private void notify(TransferRequest t,String title,String msg){notifications.createNotification(t.getStudent().getRollNumber(),title,msg,"TRANSFER");}
 private void audit(TransferRequest t,String role,String actor,TransferStatus old,TransferStatus next){audits.log(actor,role,next.name(),"TRANSFER_REQUEST",String.valueOf(t.getId()),old==null?null:old.name(),next.name(),null);}
}
