package com.web.sms.service;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.web.sms.dto.request.PaymentRequest;
import com.web.sms.dto.request.RazorpayVerificationRequest;
import com.web.sms.dto.response.PaymentResponse;
import com.web.sms.dto.response.RazorpayOrderResponse;
import com.web.sms.entity.Fee;
import com.web.sms.entity.Payment;
import com.web.sms.entity.Student;
import com.web.sms.enums.PaymentStatus;
import com.web.sms.enums.Role;
import com.web.sms.exception.BadRequestException;
import com.web.sms.exception.ForbiddenException;
import com.web.sms.exception.ResourceNotFoundException;
import com.web.sms.repository.FeeRepository;
import com.web.sms.repository.PaymentRepository;
import com.web.sms.repository.BusInchargeAssignmentRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;

@Service
public class RazorpayPaymentService {
    private static final String CURRENCY = "INR";
    private final FeeRepository feeRepository;
    private final PaymentRepository paymentRepository;
    private final FeeService feeService;
    private final BusInchargeAssignmentRepository busInchargeAssignmentRepository;
    private final ObjectMapper objectMapper;
    private final RestClient razorpayClient;
    private final String keyId;
    private final String keySecret;
    private final String webhookSecret;

    public RazorpayPaymentService(FeeRepository feeRepository,
                                  PaymentRepository paymentRepository,
                                  FeeService feeService,
                                  BusInchargeAssignmentRepository busInchargeAssignmentRepository,
                                  ObjectMapper objectMapper,
                                  @Value("${razorpay.base-url}") String baseUrl,
                                  @Value("${razorpay.key-id}") String keyId,
                                  @Value("${razorpay.key-secret}") String keySecret,
                                  @Value("${razorpay.webhook-secret}") String webhookSecret) {
        this.feeRepository = feeRepository;
        this.paymentRepository = paymentRepository;
        this.feeService = feeService;
        this.busInchargeAssignmentRepository = busInchargeAssignmentRepository;
        this.objectMapper = objectMapper;
        this.keyId = keyId;
        this.keySecret = keySecret;
        this.webhookSecret = webhookSecret;
        this.razorpayClient = RestClient.builder().baseUrl(baseUrl).build();
    }

    @Transactional
    public RazorpayOrderResponse createOrder(Long studentId, PaymentRequest request) {
        requireApiCredentials();
        Fee fee = feeRepository.findByIdForUpdate(request.getFeeId())
                .orElseThrow(() -> new ResourceNotFoundException("Fee record not found"));
        if (!fee.getStudent().getId().equals(studentId)) throw new ForbiddenException("Unauthorized fee payment attempt");
        return createOrReuseOrder(fee, request.getAmount());
    }

    private RazorpayOrderResponse createNewOrder(Fee fee, Long amount) {
        long amountPaise = toPaise(amount);
        String receipt = "BTMS_" + fee.getId() + "_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        JsonNode order;
        try {
            order = razorpayClient.post().uri("/v1/orders")
                    .headers(headers -> headers.setBasicAuth(keyId, keySecret))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "amount", amountPaise,
                            "currency", CURRENCY,
                            "receipt", receipt,
                            "notes", Map.of(
                                    "fee_id", String.valueOf(fee.getId()),
                                    "student_roll_number", fee.getStudent().getRollNumber(),
                                    "academic_year", fee.getAcademicYear().getYearName())))
                    .retrieve().body(JsonNode.class);
        } catch (RestClientException exception) {
            throw new BadRequestException("Unable to create the Razorpay order. Please try again shortly");
        }
        if (order == null || !StringUtils.hasText(order.path("id").asText())) {
            throw new BadRequestException("Razorpay did not return a valid order");
        }

        Payment payment = new Payment();
        payment.setFee(fee);
        payment.setStudent(fee.getStudent());
        payment.setAmount(amount); // Persist rupees only.
        payment.setOrderId(order.path("id").asText());
        payment.setStatus(PaymentStatus.INITIATED);
        payment.setGatewayName("RAZORPAY");
        payment.setGatewayResponse(order.toString());
        payment.setCreatedAt(LocalDateTime.now());
        payment.setUpdatedAt(LocalDateTime.now());
        return checkoutResponse(paymentRepository.save(payment), fee.getStudent(), fee);
    }

    @Transactional
    public RazorpayOrderResponse createStaffOrder(Long actorId, Role role, PaymentRequest request) {
        requireApiCredentials();
        if (role != Role.ADMIN && role != Role.INCHARGE) throw new ForbiddenException("Staff payment access is required");
        Fee fee = feeRepository.findByIdForUpdate(request.getFeeId())
                .orElseThrow(() -> new ResourceNotFoundException("Fee record not found"));
        requireStaffFeeAccess(actorId, role, fee);
        return createOrReuseOrder(fee, request.getAmount());
    }

    @Transactional
    public PaymentResponse verifyCheckout(Long studentId, RazorpayVerificationRequest request) {
        requireApiCredentials();
        Payment payment = paymentRepository.findByOrderIdForUpdate(request.getRazorpayOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Payment order not found"));
        if (!payment.getStudent().getId().equals(studentId)) throw new ForbiddenException("Unauthorized payment verification attempt");
        if (payment.getStatus() == PaymentStatus.SUCCESS) return PaymentResponse.fromPayment(payment);

        return verifyPayment(payment, request);
    }

    @Transactional
    public PaymentResponse verifyStaffCheckout(Long actorId, Role role, RazorpayVerificationRequest request) {
        requireApiCredentials();
        if (role != Role.ADMIN && role != Role.INCHARGE) throw new ForbiddenException("Staff payment access is required");
        Payment payment = paymentRepository.findByOrderIdForUpdate(request.getRazorpayOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Payment order not found"));
        requireStaffFeeAccess(actorId, role, payment.getFee());
        if (payment.getStatus() == PaymentStatus.SUCCESS) return PaymentResponse.fromPayment(payment);
        return verifyPayment(payment, request);
    }

    private RazorpayOrderResponse createOrReuseOrder(Fee fee, Long amount) {
        validateAmount(fee, amount);
        var existing = paymentRepository.findFirstByFeeIdAndStatusOrderByCreatedAtDesc(fee.getId(), PaymentStatus.INITIATED);
        if (existing.isPresent()) {
            Payment previous = existing.get();
            if (previous.getAmount().equals(amount)) return checkoutResponse(previous, fee.getStudent(), fee);
            throw new BadRequestException("A payment of ₹" + previous.getAmount()
                    + " is already in progress for this fee. Complete that payment before choosing a different amount");
        }
        return createNewOrder(fee, amount);
    }

    private PaymentResponse verifyPayment(Payment payment, RazorpayVerificationRequest request) {
        String signedPayload = payment.getOrderId() + "|" + request.getRazorpayPaymentId();
        if (!verifyHmac(signedPayload, request.getRazorpaySignature(), keySecret)) {
            throw new BadRequestException("Payment signature verification failed");
        }

        JsonNode gatewayPayment = fetchPayment(request.getRazorpayPaymentId());
        verifyGatewayPayment(payment, gatewayPayment);
        if ("authorized".equalsIgnoreCase(gatewayPayment.path("status").asText())) {
            gatewayPayment = capturePayment(request.getRazorpayPaymentId(), toPaise(payment.getAmount()));
        }
        if (!"captured".equalsIgnoreCase(gatewayPayment.path("status").asText())) {
            throw new BadRequestException("Payment has not been captured by Razorpay");
        }

        feeService.confirmPayment(payment.getOrderId(), request.getRazorpayPaymentId(), gatewayPayment.toString());
        return PaymentResponse.fromPayment(paymentRepository.findByOrderId(payment.getOrderId()).orElse(payment));
    }

    private void requireStaffFeeAccess(Long actorId, Role role, Fee fee) {
        if (role == Role.ADMIN) return;
        if (fee.getAllocation() == null || fee.getAllocation().getBus() == null || fee.getAcademicYear() == null
                || busInchargeAssignmentRepository.findByBusIdAndInchargeIdAndAcademicYearId(
                        fee.getAllocation().getBus().getId(), actorId, fee.getAcademicYear().getId()).isEmpty()) {
            throw new ForbiddenException("This fee does not belong to a bus you managed in that academic year");
        }
    }

    @Transactional
    public void processWebhook(String rawBody, String signature) {
        if (!StringUtils.hasText(webhookSecret)) throw new IllegalStateException("Razorpay webhook secret is not configured");
        if (!verifyHmac(rawBody, signature, webhookSecret)) throw new ForbiddenException("Invalid Razorpay webhook signature");
        try {
            JsonNode event = objectMapper.readTree(rawBody);
            String eventType = event.path("event").asText();
            JsonNode gatewayPayment = event.path("payload").path("payment").path("entity");
            if (gatewayPayment.isMissingNode() || gatewayPayment.isEmpty()) return;
            String orderId = gatewayPayment.path("order_id").asText();
            if (!StringUtils.hasText(orderId)) return;
            Payment payment = paymentRepository.findByOrderIdForUpdate(orderId).orElse(null);
            if (payment == null) return; // Ignore unrelated Razorpay account traffic.
            verifyGatewayPayment(payment, gatewayPayment);
            if ("payment.captured".equals(eventType) || "order.paid".equals(eventType)) {
                feeService.confirmPayment(orderId, gatewayPayment.path("id").asText(), rawBody);
            } else if ("payment.failed".equals(eventType) && payment.getStatus() != PaymentStatus.SUCCESS) {
                markFailed(payment, rawBody);
            }
        } catch (BadRequestException | ForbiddenException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BadRequestException("Invalid Razorpay webhook payload");
        }
    }

    private JsonNode fetchPayment(String paymentId) {
        try {
            return razorpayClient.get().uri("/v1/payments/{id}", paymentId)
                    .headers(headers -> headers.setBasicAuth(keyId, keySecret))
                    .retrieve().body(JsonNode.class);
        } catch (RestClientException exception) {
            throw new BadRequestException("Unable to verify payment status with Razorpay");
        }
    }

    private JsonNode capturePayment(String paymentId, long amountPaise) {
        try {
            JsonNode result = razorpayClient.post().uri("/v1/payments/{id}/capture", paymentId)
                    .headers(headers -> headers.setBasicAuth(keyId, keySecret))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("amount", amountPaise, "currency", CURRENCY))
                    .retrieve().body(JsonNode.class);
            if (result == null) throw new BadRequestException("Razorpay returned an empty capture response");
            return result;
        } catch (RestClientException exception) {
            throw new BadRequestException("Razorpay could not capture the payment");
        }
    }

    private void verifyGatewayPayment(Payment local, JsonNode gateway) {
        if (gateway == null
                || !local.getOrderId().equals(gateway.path("order_id").asText())
                || toPaise(local.getAmount()) != gateway.path("amount").asLong()
                || !CURRENCY.equalsIgnoreCase(gateway.path("currency").asText())) {
            throw new BadRequestException("Razorpay payment details do not match the local order");
        }
    }

    private void validateAmount(Fee fee, Long amountRupees) {
        if (fee.getAllocation() == null || fee.getAcademicYear() == null) {
            throw new BadRequestException("This fee is not linked to a valid academic-year allocation");
        }
        if (amountRupees == null || amountRupees <= 0) throw new BadRequestException("Payment amount must be greater than zero");
        long remaining = fee.getTotalAmount() - (fee.getPaidAmount() == null ? 0L : fee.getPaidAmount());
        if (remaining <= 0 || "PAID".equalsIgnoreCase(fee.getStatus())) throw new BadRequestException("This fee has already been paid in full");
        if (amountRupees > remaining) throw new BadRequestException("Payment amount cannot exceed remaining balance of ₹" + remaining);
    }

    private RazorpayOrderResponse checkoutResponse(Payment payment, Student student, Fee fee) {
        RazorpayOrderResponse response = new RazorpayOrderResponse();
        response.setKeyId(keyId);
        response.setOrderId(payment.getOrderId());
        response.setAmount(payment.getAmount());
        response.setAmountPaise(toPaise(payment.getAmount()));
        response.setCurrency(CURRENCY);
        response.setName("LBRCE Bus Transport");
        response.setDescription("Transport fee - " + fee.getAcademicYear().getYearName());
        response.setPrefillName(student.getName());
        response.setPrefillEmail(student.getEmail());
        response.setPrefillContact(student.getPhoneNumber());
        return response;
    }

    private void markFailed(Payment payment, String response) {
        if (payment.getStatus() == PaymentStatus.SUCCESS) return;
        payment.setStatus(PaymentStatus.FAILED);
        payment.setGatewayResponse(response);
        payment.setUpdatedAt(LocalDateTime.now());
        paymentRepository.save(payment);
    }

    private long toPaise(Long rupees) {
        try { return Math.multiplyExact(rupees, 100L); }
        catch (ArithmeticException exception) { throw new BadRequestException("Payment amount is too large"); }
    }

    private boolean verifyHmac(String payload, String suppliedSignature, String secret) {
        if (!StringUtils.hasText(payload) || !StringUtils.hasText(suppliedSignature) || !StringUtils.hasText(secret)) return false;
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] expected = HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8))).getBytes(StandardCharsets.US_ASCII);
            return MessageDigest.isEqual(expected, suppliedSignature.trim().toLowerCase().getBytes(StandardCharsets.US_ASCII));
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to verify Razorpay signature", exception);
        }
    }

    private void requireApiCredentials() {
        if (!StringUtils.hasText(keyId) || !StringUtils.hasText(keySecret)) {
            throw new IllegalStateException("Razorpay API credentials are not configured");
        }
    }
}
