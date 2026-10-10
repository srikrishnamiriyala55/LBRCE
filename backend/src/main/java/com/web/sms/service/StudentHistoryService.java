package com.web.sms.service;

import com.web.sms.dto.response.*;
import com.web.sms.entity.AcademicYear;
import com.web.sms.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Comparator;
import java.util.List;

@Service
public class StudentHistoryService {
    private final AcademicYearRepository years; private final StudentAcademicDetailsRepository academics;
    private final BusApplicationRepository applications; private final TransportAllocationRepository allocations;
    private final FeeRepository fees; private final PaymentRepository payments; private final BusPassRepository passes;
    private final TransferRequestRepository transfers;
    public StudentHistoryService(AcademicYearRepository y,StudentAcademicDetailsRepository ad,BusApplicationRepository a,TransportAllocationRepository al,FeeRepository f,PaymentRepository p,BusPassRepository bp,TransferRequestRepository t){years=y;academics=ad;applications=a;allocations=al;fees=f;payments=p;passes=bp;transfers=t;}

    @Transactional(readOnly=true)
    public List<StudentYearHistoryResponse> history(Long studentId){
        return years.findAll().stream().sorted(Comparator.comparing(AcademicYear::getStartDate,Comparator.nullsLast(Comparator.reverseOrder())))
                .map(year->build(studentId,year)).filter(this::hasHistory).toList();
    }
    public List<StudentYearHistoryResponse> history(Long studentId,java.util.Set<Long> allowedAcademicYearIds){return history(studentId).stream().filter(item->allowedAcademicYearIds.contains(item.getAcademicYearId())).toList();}
    private StudentYearHistoryResponse build(Long studentId,AcademicYear year){
        StudentYearHistoryResponse r=new StudentYearHistoryResponse();r.setAcademicYearId(year.getId());r.setAcademicYear(year.getYearName());r.setActive(year.isActive());
        academics.findByStudentIdAndAcademicYearId(studentId,year.getId()).ifPresent(d->{r.setBranch(d.getBranch());r.setYear(d.getYear());r.setSemester(d.getSemester());r.setAcademicStatus(d.getStatus());});
        r.setApplications(applications.findByStudentIdAndAcademicYearId(studentId,year.getId()).stream().map(ApplicationResponse::fromApplication).toList());
        r.setAllocations(allocations.findByStudentId(studentId).stream().filter(a->a.getAcademicYear()!=null&&year.getId().equals(a.getAcademicYear().getId())).sorted(Comparator.comparing(com.web.sms.entity.TransportAllocation::getAllocatedAt,Comparator.nullsLast(Comparator.naturalOrder()))).map(AllocationHistoryResponse::from).toList());
        r.setFees(fees.findByStudentId(studentId).stream().filter(f->f.getAcademicYear()!=null&&year.getId().equals(f.getAcademicYear().getId())).map(FeeResponse::fromFee).toList());
        r.setPayments(payments.findByStudentIdOrderByCreatedAtDesc(studentId).stream().filter(p->p.getFee()!=null&&p.getFee().getAcademicYear()!=null&&year.getId().equals(p.getFee().getAcademicYear().getId())).map(PaymentResponse::fromPayment).toList());
        r.setPasses(passes.findByStudentId(studentId).stream().filter(p->p.getAcademicYear()!=null&&year.getId().equals(p.getAcademicYear().getId())).map(PassResponse::fromBusPass).toList());
        r.setTransfers(transfers.findByStudentId(studentId).stream().filter(t->t.getAcademicYear()!=null&&year.getId().equals(t.getAcademicYear().getId())).map(TransferResponse::fromTransfer).toList());return r;
    }
    private boolean hasHistory(StudentYearHistoryResponse r){return r.getBranch()!=null||!r.getApplications().isEmpty()||!r.getAllocations().isEmpty()||!r.getFees().isEmpty()||!r.getPayments().isEmpty()||!r.getPasses().isEmpty()||!r.getTransfers().isEmpty();}
}
