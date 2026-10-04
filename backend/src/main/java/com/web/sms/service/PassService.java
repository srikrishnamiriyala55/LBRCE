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
import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
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
import java.io.InputStream;
import java.util.Optional;

@Service
public class PassService {

    private final BusPassRepository passRepo;
    private final AcademicYearRepository academicYearRepo;
    private final StudentRepository studentRepo;

    @Value("${btms.pass.verification-url}")
    private String verificationUrl;

    @Value("${btms.pass.frontend-verify-url}")
    private String frontendVerifyUrl;

    public PassService(BusPassRepository passRepo, AcademicYearRepository academicYearRepo,
                       StudentRepository studentRepo) {
        this.passRepo = passRepo;
        this.academicYearRepo = academicYearRepo;
        this.studentRepo = studentRepo;
    }

    public PassResponse getStudentPass(Long studentId) {
        return toPassResponse(findActivePass(studentId));
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
        return toPassResponse(findPass(passId));
    }

    public PassResponse getPassForIncharge(Long passId, Long busId) {
        return toPassResponse(requireBusAccess(findPass(passId), busId));
    }

    private PassResponse toPassResponse(BusPass pass) {
        PassResponse r = PassResponse.fromBusPass(pass);
        if (pass.getVerificationToken() != null) {
            r.setVerificationUrl(frontendVerifyUrl + pass.getVerificationToken());
        }
        return r;
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

    private Image loadHeaderImage() {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("header_image.png")) {
            if (in == null) return null;
            byte[] bytes = in.readAllBytes();
            Image img = Image.getInstance(bytes);
            img.setAlignment(Element.ALIGN_CENTER);
            img.setSpacingAfter(6);
            return img;
        } catch (Exception e) {
            return null;
        }
    }

    private Image loadLogoImage() {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("logo.jpg")) {
            if (in == null) return null;
            byte[] bytes = in.readAllBytes();
            return Image.getInstance(bytes);
        } catch (Exception e) {
            return null;
        }
    }

    private byte[] generatePdf(BusPass entity) {
        PassResponse pass = PassResponse.fromBusPass(entity);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        // A5 portrait: 420 x 595 pt. Margins 18pt all around => 384 pt usable width, 559 pt usable height.
        Document document = new Document(PageSize.A5, 18, 18, 18, 18);
        try {
            PdfWriter.getInstance(document, output);
            document.open();

            // Outer card table with 1 column, wrapped with a border matching the on-screen preview
            PdfPTable card = new PdfPTable(1);
            card.setWidthPercentage(100);

            // ── 1. HEADER (Deep Blue, logo + college name + subtitle) ────────────────
            PdfPTable headerTable = new PdfPTable(new float[]{46f, 338f});
            headerTable.setWidthPercentage(100);

            // Logo
            PdfPCell logoCell = new PdfPCell();
            logoCell.setBackgroundColor(new Color(30, 58, 138)); // #1e3a8a
            logoCell.setBorder(Rectangle.NO_BORDER);
            logoCell.setPadding(8f);
            logoCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            Image logo = loadLogoImage();
            if (logo != null) {
                logo.scaleToFit(38, 38);
                logoCell.addElement(logo);
            }
            headerTable.addCell(logoCell);

            // Title text
            PdfPCell titleCell = new PdfPCell();
            titleCell.setBackgroundColor(new Color(30, 58, 138));
            titleCell.setBorder(Rectangle.NO_BORDER);
            titleCell.setPadding(8f);
            titleCell.setPaddingLeft(2f);
            titleCell.setVerticalAlignment(Element.ALIGN_MIDDLE);

            Paragraph titlePara = new Paragraph();
            titlePara.setLeading(13f);
            Chunk collegeChunk = new Chunk("LAKIREDDY BALI REDDY COLLEGE OF ENGINEERING\n",
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10.5f, Color.WHITE));
            Chunk subChunk = new Chunk("DIGITAL BUS PASS",
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8f, new Color(147, 197, 253)));
            titlePara.add(collegeChunk);
            titlePara.add(subChunk);
            titleCell.addElement(titlePara);
            headerTable.addCell(titleCell);

            PdfPCell headerContainerCell = new PdfPCell(headerTable);
            headerContainerCell.setBorder(Rectangle.TOP | Rectangle.LEFT | Rectangle.RIGHT);
            headerContainerCell.setBorderColor(new Color(191, 219, 254));
            headerContainerCell.setBorderWidth(1.5f);
            headerContainerCell.setPadding(0);
            card.addCell(headerContainerCell);

            // ── 2. TOP SECTION (Photo on left, Pass No + Status + QR on right) ────────
            PdfPTable topTable = new PdfPTable(new float[]{110f, 274f});
            topTable.setWidthPercentage(100);

            // Student Photo
            PdfPCell photoCell = new PdfPCell();
            photoCell.setBorder(Rectangle.NO_BORDER);
            photoCell.setPadding(10f);
            photoCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            photoCell.setVerticalAlignment(Element.ALIGN_MIDDLE);

            if (entity.getStudent() != null && entity.getStudent().getPhotoData() != null
                    && entity.getStudent().getPhotoData().length > 0) {
                try {
                    Image photo = Image.getInstance(entity.getStudent().getPhotoData());
                    photo.scaleToFit(92, 108);
                    photo.setAlignment(Element.ALIGN_CENTER);
                    photoCell.addElement(photo);
                } catch (Exception ex) {
                    Paragraph noPhoto = new Paragraph("Photo\nunavailable",
                            FontFactory.getFont(FontFactory.HELVETICA, 8, Color.GRAY));
                    noPhoto.setAlignment(Element.ALIGN_CENTER);
                    photoCell.addElement(noPhoto);
                }
            } else {
                Paragraph noPhoto = new Paragraph("Photo\nunavailable",
                        FontFactory.getFont(FontFactory.HELVETICA, 8, Color.GRAY));
                noPhoto.setAlignment(Element.ALIGN_CENTER);
                photoCell.addElement(noPhoto);
            }
            topTable.addCell(photoCell);

            // Right side: Pass No, Status, QR Code
            PdfPCell infoCell = new PdfPCell();
            infoCell.setBorder(Rectangle.NO_BORDER);
            infoCell.setPadding(8f);
            infoCell.setPaddingRight(12f);

            PdfPTable rightStack = new PdfPTable(new float[]{160f, 100f});
            rightStack.setWidthPercentage(100);

            // Text on left of QR
            PdfPCell passNumCell = new PdfPCell();
            passNumCell.setBorder(Rectangle.NO_BORDER);
            passNumCell.setPadding(2f);

            Paragraph pNumPara = new Paragraph();
            pNumPara.setLeading(12f);
            pNumPara.add(new Chunk("PASS NUMBER\n",
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7.5f, new Color(100, 116, 139))));
            pNumPara.add(new Chunk(pass.getPassNumber() + "\n\n",
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12f, new Color(30, 58, 138))));
            pNumPara.add(new Chunk("STATUS: ",
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8f, new Color(100, 116, 139))));
            String status = value(pass.getStatus());
            Color statusColor = "ACTIVE".equalsIgnoreCase(status) ? new Color(22, 163, 74) : new Color(220, 38, 38);
            pNumPara.add(new Chunk(status,
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9f, statusColor)));
            passNumCell.addElement(pNumPara);
            rightStack.addCell(passNumCell);

            // QR Code
            PdfPCell qrCell = new PdfPCell();
            qrCell.setBorder(Rectangle.NO_BORDER);
            qrCell.setPadding(0);
            qrCell.setHorizontalAlignment(Element.ALIGN_CENTER);

            try {
                ByteArrayOutputStream qrOutput = new ByteArrayOutputStream();
                MatrixToImageWriter.writeToStream(new QRCodeWriter().encode(
                        frontendVerifyUrl + entity.getVerificationToken(), BarcodeFormat.QR_CODE, 180, 180), "PNG", qrOutput);
                Image qr = Image.getInstance(qrOutput.toByteArray());
                qr.scaleToFit(76, 76);
                qr.setAlignment(Element.ALIGN_CENTER);
                qrCell.addElement(qr);

                Paragraph scanPara = new Paragraph("Scan to verify",
                        FontFactory.getFont(FontFactory.HELVETICA_BOLD, 6.5f, new Color(100, 116, 139)));
                scanPara.setAlignment(Element.ALIGN_CENTER);
                qrCell.addElement(scanPara);
            } catch (Exception qrEx) {
                // If QR fails, leave cell blank
            }
            rightStack.addCell(qrCell);

            infoCell.addElement(rightStack);
            topTable.addCell(infoCell);

            PdfPCell topContainerCell = new PdfPCell(topTable);
            topContainerCell.setBorder(Rectangle.LEFT | Rectangle.RIGHT);
            topContainerCell.setBorderColor(new Color(191, 219, 254));
            topContainerCell.setBorderWidth(1.5f);
            topContainerCell.setPadding(0);
            topContainerCell.setPaddingBottom(6f);
            card.addCell(topContainerCell);

            // ── 3. DETAILS GRID (2 columns of items matching preview) ───────────────
            PdfPTable detailsTable = new PdfPTable(2);
            detailsTable.setWidthPercentage(100);
            detailsTable.setWidths(new float[]{192f, 192f});

            addDetailItem(detailsTable, "Student Name", pass.getStudentName());
            addDetailItem(detailsTable, "Roll Number", pass.getRollNumber());
            addDetailItem(detailsTable, "Branch", pass.getBranch());
            addDetailItem(detailsTable, "Year / Semester", value(pass.getYear()) + " / " + value(pass.getSemester()));
            addDetailItem(detailsTable, "Phone Number", pass.getPhoneNumber());
            addDetailItem(detailsTable, "Bus Number", pass.getBusNumber());
            addDetailItem(detailsTable, "Starting Point", pass.getStartingPoint());
            addDetailItem(detailsTable, "Ending Point", pass.getEndingPoint());
            addDetailItem(detailsTable, "Boarding Point", pass.getBoardingPoint());
            addDetailItem(detailsTable, "Academic Year", pass.getAcademicYear());
            addDetailItem(detailsTable, "Valid From", value(pass.getValidFrom()));
            addDetailItem(detailsTable, "Valid Until", value(pass.getValidUntil()));

            PdfPCell detailsContainerCell = new PdfPCell(detailsTable);
            detailsContainerCell.setBorder(Rectangle.LEFT | Rectangle.RIGHT);
            detailsContainerCell.setBorderColor(new Color(191, 219, 254));
            detailsContainerCell.setBorderWidth(1.5f);
            detailsContainerCell.setPadding(6f);
            detailsContainerCell.setPaddingTop(0);
            card.addCell(detailsContainerCell);

            // ── 4. FOOTER ────────────────────────────────────────────────────────────
            PdfPCell footerCell = new PdfPCell();
            footerCell.setBackgroundColor(new Color(248, 250, 252));
            footerCell.setBorder(Rectangle.TOP | Rectangle.BOTTOM | Rectangle.LEFT | Rectangle.RIGHT);
            footerCell.setBorderColor(new Color(191, 219, 254));
            footerCell.setBorderWidth(1.5f);
            footerCell.setPadding(6f);
            Paragraph footerPara = new Paragraph(
                    "System-generated digital bus pass. Verify authenticity by scanning the QR code.",
                    FontFactory.getFont(FontFactory.HELVETICA, 7f, new Color(148, 163, 184)));
            footerPara.setAlignment(Element.ALIGN_CENTER);
            footerCell.addElement(footerPara);
            card.addCell(footerCell);

            document.add(card);
            document.close();
            return output.toByteArray();
        } catch (Exception exception) {
            if (document.isOpen()) document.close();
            throw new IllegalStateException("Unable to generate bus pass PDF", exception);
        }
    }

    private void addDetailItem(PdfPTable table, String label, Object rawValue) {
        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(new Color(248, 250, 252));
        cell.setBorderColor(new Color(226, 232, 240));
        cell.setBorderWidth(1f);
        cell.setPadding(4f);
        cell.setPaddingLeft(6f);
        cell.setPaddingBottom(5f);

        Paragraph p = new Paragraph();
        p.setLeading(10f);
        Chunk labelChunk = new Chunk(label.toUpperCase() + "\n",
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 6.5f, new Color(100, 116, 139)));
        Chunk valChunk = new Chunk(value(rawValue),
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8.5f, new Color(15, 23, 42)));
        p.add(labelChunk);
        p.add(valChunk);
        cell.addElement(p);
        table.addCell(cell);
    }

    private String value(Object value) {
        return value == null ? "-" : String.valueOf(value);
    }

    public PassResponse verifyPass(String token) {
        BusPass pass = passRepo.findByVerificationToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Invalid or revoked bus pass verification token"));

        return PassResponse.fromBusPass(pass);
    }

    /**
     * Returns the student photo identified by a public verification token.
     * No authentication required – used by the QR-scan verification page.
     */
    public ResponseEntity<byte[]> passPhotoByToken(String token) {
        BusPass pass = passRepo.findByVerificationToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Invalid or revoked bus pass verification token"));
        return photoResponse(pass.getStudent());
    }
}
