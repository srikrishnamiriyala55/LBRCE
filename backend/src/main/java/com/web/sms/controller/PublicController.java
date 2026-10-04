package com.web.sms.controller;

import com.web.sms.dto.response.PassResponse;
import com.web.sms.service.PassService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/public")
public class PublicController {

    private final PassService passService;

    public PublicController(PassService passService) {
        this.passService = passService;
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "UP", "service", "LBRCE BTMS");
    }

    @GetMapping("/verify-pass")
    public PassResponse verifyPass(@RequestParam String token) {
        PassResponse response = passService.verifyPass(token);
        // Phone numbers are included on the student's private pass, never in public verification.
        response.setPhoneNumber(null);
        return response;
    }

    /**
     * Returns the student photo for a given pass verification token.
     * This endpoint is public so that the QR-scan verification page can display the photo
     * without requiring the viewer to be logged in.
     */
    @GetMapping("/pass-photo")
    public ResponseEntity<byte[]> passPhotoByToken(@RequestParam String token) {
        return passService.passPhotoByToken(token);
    }
}
