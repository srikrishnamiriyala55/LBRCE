package com.web.sms.service;

import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
public class PaymentReceiptEmailService {
    private static final Logger log = LoggerFactory.getLogger(PaymentReceiptEmailService.class);
    private static final ZoneId INDIA = ZoneId.of("Asia/Kolkata");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");

    private final JavaMailSender mailSender;
    private final boolean enabled;
    private final String username;
    private final String from;
    private final String fromName;
    private final String contactEmail;
    private final String contactPhones;
    private final String collegeAddress;

    public PaymentReceiptEmailService(JavaMailSender mailSender,
                                      @Value("${btms.mail.enabled:false}") boolean enabled,
                                      @Value("${spring.mail.username:}") String username,
                                      @Value("${btms.mail.from:}") String from,
                                      @Value("${btms.mail.from-name:LBRCE BUS}") String fromName,
                                      @Value("${btms.transport.contact-email}") String contactEmail,
                                      @Value("${btms.transport.contact-phones}") String contactPhones,
                                      @Value("${btms.college.address}") String collegeAddress) {
        this.mailSender = mailSender;
        this.enabled = enabled;
        this.username = username;
        this.from = from;
        this.fromName = fromName;
        this.contactEmail = contactEmail;
        this.contactPhones = contactPhones;
        this.collegeAddress = collegeAddress;
    }

    @Async("mailTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void sendAfterCommit(PaymentReceiptEvent event) {
        if (!enabled) {
            log.debug("Payment receipt email is disabled for transaction {}", event.transactionId());
            return;
        }
        if (!StringUtils.hasText(event.recipientEmail())) {
            log.warn("Payment receipt email skipped because student {} has no email address", event.rollNumber());
            return;
        }

        Exception lastFailure = null;
        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                send(event);
                log.info("Payment receipt email sent for transaction {}", event.transactionId());
                return;
            } catch (Exception exception) {
                lastFailure = exception;
                log.warn("Payment receipt email attempt {} failed for transaction {}", attempt, event.transactionId());
                if (attempt < 3) {
                    try { Thread.sleep(attempt * 1000L); }
                    catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); return; }
                }
            }
        }
        log.error("Payment succeeded but receipt email could not be delivered for transaction {} after retries: {}",
                event.transactionId(), lastFailure == null ? "unknown error" : lastFailure.getMessage());
    }

    private void send(PaymentReceiptEvent event) throws Exception {
        String sender = StringUtils.hasText(from) ? from : username;
        if (!StringUtils.hasText(sender)) throw new IllegalStateException("MAIL_FROM or MAIL_USERNAME is not configured");

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
        helper.setFrom(new InternetAddress(sender, fromName, StandardCharsets.UTF_8.name()));
        helper.setTo(event.recipientEmail());
        helper.setSubject("LBRCE Bus Fee Payment Receipt - " + event.transactionId());
        helper.setText(html(event), true);
        ClassPathResource logo = new ClassPathResource("logo.jpg");
        if (logo.exists()) helper.addInline("lbrceLogo", logo, "image/jpeg");
        mailSender.send(message);
    }

    private String html(PaymentReceiptEvent event) {
        String paidAt = event.paidAt() == null ? "—" : event.paidAt().atZone(ZoneId.systemDefault()).withZoneSameInstant(INDIA).format(DATE_TIME) + " IST";
        return """
                <!doctype html><html><body style="margin:0;background:#f3f4f6;font-family:Arial,sans-serif;color:#1f2937">
                <div style="max-width:680px;margin:24px auto;background:#fff;border:1px solid #e5e7eb;border-radius:12px;overflow:hidden">
                  <div style="background:#1e40af;color:#fff;padding:22px;text-align:center">
                    <img src="cid:lbrceLogo" alt="LBRCE" width="70" height="70" style="background:#fff;border-radius:50%%;object-fit:contain;padding:5px">
                    <h2 style="margin:10px 0 2px">LBRCE BUS</h2><div>Transportation Fee Payment Receipt</div>
                  </div>
                  <div style="padding:24px">
                    <p>Dear <strong>%s</strong>,</p>
                    <p>Your transportation fee payment has been verified and recorded successfully.</p>
                    <table style="width:100%%;border-collapse:collapse;margin:18px 0">
                      %s
                    </table>
                    <div style="background:#ecfdf5;border:1px solid #a7f3d0;padding:14px;border-radius:8px">
                      <strong>Status: %s</strong><br>Pass eligibility: %s
                    </div>
                    <p style="margin-top:22px;font-size:13px;color:#4b5563">For assistance, contact %s or call %s.</p>
                    <p style="font-size:12px;color:#6b7280">%s</p>
                  </div>
                </div></body></html>
                """.formatted(
                escape(event.studentName()), rows(event, paidAt), escape(event.transactionStatus()),
                event.passEligible() ? "Eligible" : "Not yet eligible", escape(contactEmail), escape(contactPhones), escape(collegeAddress));
    }

    private String rows(PaymentReceiptEvent e, String paidAt) {
        return row("Student", e.studentName() + " (" + e.rollNumber() + ")")
                + row("Academic Year", e.academicYear()) + row("Bus / Boarding Point", value(e.busNumber()) + " / " + value(e.boardingPoint()))
                + row("Transaction Status", e.transactionStatus()) + row("Transaction ID", e.transactionId())
                + row("Order / Receipt ID", e.orderId()) + row("Payment Mode", e.paymentMode())
                + row("Amount Paid", money(e.amountPaid())) + row("Total Fee", money(e.totalFee()))
                + row("Total Paid", money(e.totalPaid())) + row("Pending Fee", money(e.pendingFee()))
                + row("Payment Date", paidAt);
    }

    private String row(String label, String value) {
        return "<tr><td style=\"border:1px solid #e5e7eb;background:#f9fafb;padding:10px;font-weight:bold;width:40%\">" + escape(label)
                + "</td><td style=\"border:1px solid #e5e7eb;padding:10px\">" + escape(value(value)) + "</td></tr>";
    }

    private String money(Long amount) { return "₹" + NumberFormat.getNumberInstance(new Locale("en", "IN")).format(amount == null ? 0 : amount); }
    private String value(String value) { return StringUtils.hasText(value) ? value : "—"; }
    private String escape(String value) { return value(value).replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;"); }
}
