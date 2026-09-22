package com.web.sms.service;

import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.web.sms.dto.request.TransportReportFilter;
import com.web.sms.dto.response.TransportReportRow;
import com.web.sms.exception.BadRequestException;
import com.web.sms.repository.StudentRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class TransportReportService {
    private static final int EXPORT_LIMIT=10_000;
    private static final String[] HEADERS={"Roll Number","Student Name","Department","Year","Semester","Account","Bus Number","Boarding Point","Total Fee","Paid","Remaining","Payment","Application","Transfer"};
    private final StudentRepository students;
    public TransportReportService(StudentRepository students){this.students=students;}

    public Page<TransportReportRow> report(Long scopeBusId,TransportReportFilter f,Pageable pageable){
        validate(f);return query(scopeBusId,f,pageable);
    }

    public byte[] excel(Long scopeBusId,TransportReportFilter f,String title){
        List<TransportReportRow> rows=exportRows(scopeBusId,f);
        try(SXSSFWorkbook wb=new SXSSFWorkbook(100);ByteArrayOutputStream out=new ByteArrayOutputStream()){
            Sheet sheet=wb.createSheet("Transport Report");sheet.createFreezePane(0,2);sheet.setAutoFilter(new org.apache.poi.ss.util.CellRangeAddress(1,Math.max(1,rows.size()+1),0,HEADERS.length-1));
            CellStyle titleStyle=wb.createCellStyle();titleStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());titleStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);org.apache.poi.ss.usermodel.Font tf=wb.createFont();tf.setBold(true);tf.setColor(IndexedColors.WHITE.getIndex());tf.setFontHeightInPoints((short)14);titleStyle.setFont(tf);
            org.apache.poi.ss.usermodel.Row titleRow=sheet.createRow(0);Cell titleCell=titleRow.createCell(0);titleCell.setCellValue(title+" - "+LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm")));titleCell.setCellStyle(titleStyle);sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0,0,0,HEADERS.length-1));
            CellStyle header=wb.createCellStyle();header.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());header.setFillPattern(FillPatternType.SOLID_FOREGROUND);org.apache.poi.ss.usermodel.Font hf=wb.createFont();hf.setBold(true);header.setFont(hf);header.setBorderBottom(BorderStyle.THIN);
            org.apache.poi.ss.usermodel.Row hr=sheet.createRow(1);for(int i=0;i<HEADERS.length;i++){Cell c=hr.createCell(i);c.setCellValue(HEADERS[i]);c.setCellStyle(header);}
            int n=2;for(TransportReportRow r:rows){org.apache.poi.ss.usermodel.Row row=sheet.createRow(n++);Object[] values=values(r);for(int i=0;i<values.length;i++){Cell c=row.createCell(i);Object v=values[i];if(v instanceof Number number)c.setCellValue(number.doubleValue());else c.setCellValue(text(v));}}
            int[] widths={18,26,16,8,10,13,14,22,14,14,14,18,20,24};for(int i=0;i<widths.length;i++)sheet.setColumnWidth(i,widths[i]*256);
            wb.write(out);wb.dispose();return out.toByteArray();
        }catch(Exception e){throw new IllegalStateException("Unable to generate Excel report",e);}
    }

    public byte[] pdf(Long scopeBusId,TransportReportFilter f,String title){
        List<TransportReportRow> rows=exportRows(scopeBusId,f);ByteArrayOutputStream out=new ByteArrayOutputStream();
        Document doc=new Document(PageSize.A3.rotate(),24,24,28,28);
        try{PdfWriter.getInstance(doc,out);doc.open();com.lowagie.text.Font heading=FontFactory.getFont(FontFactory.HELVETICA_BOLD,15,new Color(20,60,120));doc.add(new Paragraph(title,heading));doc.add(new Paragraph("Generated: "+LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm"))+" | Records: "+rows.size(),FontFactory.getFont(FontFactory.HELVETICA,9)));doc.add(Chunk.NEWLINE);
            PdfPTable table=new PdfPTable(HEADERS.length);table.setWidthPercentage(100);table.setHeaderRows(1);for(String h:HEADERS){PdfPCell c=new PdfPCell(new Phrase(h,FontFactory.getFont(FontFactory.HELVETICA_BOLD,6,Color.WHITE)));c.setBackgroundColor(new Color(20,60,120));c.setPadding(4);table.addCell(c);}for(TransportReportRow r:rows)for(Object v:values(r)){PdfPCell c=new PdfPCell(new Phrase(text(v),FontFactory.getFont(FontFactory.HELVETICA,6)));c.setPadding(3);table.addCell(c);}doc.add(table);doc.close();return out.toByteArray();
        }catch(Exception e){if(doc.isOpen())doc.close();throw new IllegalStateException("Unable to generate PDF report",e);}
    }

    private List<TransportReportRow> exportRows(Long scopeBusId,TransportReportFilter f){validate(f);Page<TransportReportRow> page=query(scopeBusId,f,PageRequest.of(0,EXPORT_LIMIT));if(page.getTotalElements()>EXPORT_LIMIT)throw new BadRequestException("Export exceeds 10,000 records. Apply more filters and try again");return page.getContent();}
    private Page<TransportReportRow> query(Long scopeBusId,TransportReportFilter f,Pageable p){return students.transportReport(scopeBusId,clean(f.getBusNumber()),clean(f.getName()),clean(f.getRollNumber()),f.getYear(),f.getSemester(),upper(f.getPaymentStatus()),upper(f.getAccountStatus()),upper(f.getApplicationStatus()),upper(f.getTransferStatus()),clean(f.getDepartment()),f.getMinimumRemaining(),p);}
    private void validate(TransportReportFilter f){if(f.getYear()!=null&&(f.getYear()<1||f.getYear()>4))throw new BadRequestException("Year must be between 1 and 4");if(f.getSemester()!=null&&(f.getSemester()<1||f.getSemester()>8))throw new BadRequestException("Semester must be between 1 and 8");if(f.getMinimumRemaining()!=null&&f.getMinimumRemaining()<0)throw new BadRequestException("Minimum remaining amount cannot be negative");}
    private String clean(String s){return s==null||s.isBlank()?null:s.trim();}private String upper(String s){String v=clean(s);return v==null?null:v.toUpperCase();}private String text(Object o){return o==null?"-":String.valueOf(o);}
    private Object[] values(TransportReportRow r){return new Object[]{r.getRollNumber(),r.getStudentName(),r.getDepartment(),r.getStudyYear(),r.getSemester(),r.getAccountStatus(),r.getBusNumber(),r.getBoardingPoint(),r.getTotalAmount(),r.getPaidAmount(),r.getRemainingAmount(),r.getPaymentStatus(),r.getApplicationStatus(),r.getTransferStatus()};}
}
