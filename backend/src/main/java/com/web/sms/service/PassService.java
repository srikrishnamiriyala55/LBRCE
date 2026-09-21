package com.web.sms.service;

import com.web.sms.dto.response.PassResponse;
import com.web.sms.entity.AcademicYear;
import com.web.sms.entity.BusPass;
import com.web.sms.enums.PassStatus;
import com.web.sms.exception.ResourceNotFoundException;
import com.web.sms.repository.AcademicYearRepository;
import com.web.sms.repository.BusPassRepository;
import com.web.sms.repository.StudentRepository;
import com.web.sms.entity.Student;
import com.web.sms.exception.ForbiddenException;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.qrcode.QRCodeWriter;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.util.Optional;

@Service
public class PassService {

    private final BusPassRepository passRepo;
    private final AcademicYearRepository academicYearRepo;
    private final StudentRepository studentRepo;

    @Value("${btms.pass.verification-url}")
    private String verificationUrl;

    public PassService(BusPassRepository passRepo, AcademicYearRepository academicYearRepo,
                       StudentRepository studentRepo) {
        this.passRepo = passRepo;
        this.academicYearRepo = academicYearRepo;
        this.studentRepo = studentRepo;
    }

    public PassResponse getStudentPass(Long studentId) {
        return PassResponse.fromBusPass(findActivePass(studentId));
    }

    private BusPass findActivePass(Long studentId) {
        Optional<AcademicYear> activeYear = academicYearRepo.findByActiveTrue();
        if (activeYear.isEmpty()) {
            throw new ResourceNotFoundException("No active academic year configured");
        }

        return passRepo
                .findByStudentIdAndAcademicYearIdAndStatus(studentId, activeYear.get().getId(), PassStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("No active bus pass found for current academic year"));
    }

    public byte[] generatePdf(Long studentId) {
        BusPass entity = findActivePass(studentId);
        return generatePdf(entity);
    }

    public PassResponse getPassForAdmin(Long passId) {
        return PassResponse.fromBusPass(findPass(passId));
    }

    public PassResponse getPassForIncharge(Long passId, Long busId) {
        return PassResponse.fromBusPass(requireBusAccess(findPass(passId), busId));
    }

    public byte[] generatePdfForAdmin(Long passId) {
        return generatePdf(findPass(passId));
    }

    public byte[] generatePdfForIncharge(Long passId, Long busId) {
        return generatePdf(requireBusAccess(findPass(passId), busId));
    }

    public ResponseEntity<byte[]> studentPhotoResponse(Long studentId) {
        Student student = studentRepo.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));
        return photoResponse(student);
    }

    public ResponseEntity<byte[]> passPhotoForAdmin(Long passId) {
        return photoResponse(findPass(passId).getStudent());
    }

    public ResponseEntity<byte[]> passPhotoForIncharge(Long passId, Long busId) {
        return photoResponse(requireBusAccess(findPass(passId), busId).getStudent());
    }

    private BusPass findPass(Long passId) {
        return passRepo.findById(passId).orElseThrow(() -> new ResourceNotFoundException("Bus pass not found"));
    }

    private BusPass requireBusAccess(BusPass pass, Long busId) {
        if (pass.getAllocation() == null || pass.getAllocation().getBus() == null
                || !pass.getAllocation().getBus().getId().equals(busId)) {
            throw new ForbiddenException("This bus pass does not belong to your assigned bus");
        }
        return pass;
    }

    private ResponseEntity<byte[]> photoResponse(Student student) {
        if (student == null || student.getPhotoData() == null || student.getPhotoData().length == 0) {
            throw new ResourceNotFoundException("Student photo not found");
        }
        MediaType type = MediaType.parseMediaType(
                student.getPhotoContentType() == null ? MediaType.IMAGE_JPEG_VALUE : student.getPhotoContentType());
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).contentType(type).body(student.getPhotoData());
    }

    private byte[] generatePdf(BusPass entity) {
        PassResponse pass = PassResponse.fromBusPass(entity);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A5, 28, 28, 24, 24);
        try {
            PdfWriter.getInstance(document, output);
            document.open();

            Paragraph college = new Paragraph("LAKIREDDY BALI REDDY COLLEGE OF ENGINEERING",
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, new Color(30, 64, 175)));
            college.setAlignment(Element.ALIGN_CENTER);
            document.add(college);
            Paragraph heading = new Paragraph("DIGITAL BUS PASS",
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11));
            heading.setAlignment(Element.ALIGN_CENTER);
            heading.setSpacingAfter(14);
            document.add(heading);

            if (entity.getStudent() != null && entity.getStudent().getPhotoData() != null
                    && entity.getStudent().getPhotoData().length > 0) {
                Image studentPhoto = Image.getInstance(entity.getStudent().getPhotoData());
                studentPhoto.scaleToFit(90, 105);
                studentPhoto.setAlignment(Element.ALIGN_CENTER);
                studentPhoto.setSpacingAfter(10);
                document.add(studentPhoto);
            }

            PdfPTable table = new PdfPTable(2);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{1.1f, 2.2f});
            addRow(table, "Pass Number", pass.getPassNumber());
            addRow(table, "Student Name", pass.getStudentName());
            addRow(table, "Roll Number", pass.getRollNumber());
            addRow(table, "Phone Number", pass.getPhoneNumber());
            addRow(table, "Branch", pass.getBranch());
            addRow(table, "Year / Semester", value(pass.getYear()) + " / " + value(pass.getSemester()));
            addRow(table, "Bus Number", pass.getBusNumber());
            addRow(table, "Route", pass.getRouteName());
            addRow(table, "Boarding Point", pass.getBoardingPoint());
            addRow(table, "Academic Year", pass.getAcademicYear());
            addRow(table, "Validity", value(pass.getValidFrom()) + " to " + value(pass.getValidUntil()));
            addRow(table, "Status", value(pass.getStatus()));
            document.add(table);

            ByteArrayOutputStream qrOutput = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(new QRCodeWriter().encode(
                    verificationUrl + entity.getVerificationToken(), BarcodeFormat.QR_CODE, 220, 220), "PNG", qrOutput);
            Image qr = Image.getInstance(qrOutput.toByteArray());
            qr.scaleToFit(105, 105);
            qr.setAlignment(Element.ALIGN_CENTER);
            qr.setSpacingBefore(12);
            document.add(qr);
            Paragraph verification = new Paragraph("Scan to verify this bus pass",
                    FontFactory.getFont(FontFactory.HELVETICA, 8, Color.DARK_GRAY));
            verification.setAlignment(Element.ALIGN_CENTER);
            document.add(verification);
            document.close();
            return output.toByteArray();
        } catch (Exception exception) {
            if (document.isOpen()) document.close();
            throw new IllegalStateException("Unable to generate bus pass PDF", exception);
        }
    }

    private void addRow(PdfPTable table, String label, Object rawValue) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label,
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9)));
        labelCell.setBackgroundColor(new Color(239, 246, 255));
        labelCell.setPadding(6);
        table.addCell(labelCell);
        PdfPCell valueCell = new PdfPCell(new Phrase(value(rawValue),
                FontFactory.getFont(FontFactory.HELVETICA, 9)));
        valueCell.setPadding(6);
        table.addCell(valueCell);
    }

    private String value(Object value) {
        return value == null ? "-" : String.valueOf(value);
    }

    public PassResponse verifyPass(String token) {
        BusPass pass = passRepo.findByVerificationToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Invalid or revoked bus pass verification token"));

        return PassResponse.fromBusPass(pass);
    }
}
