package com.web.sms.controller;

import com.web.sms.dto.response.PassResponse;
import com.web.sms.service.PassService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public")
public class PublicController {

    private final PassService passService;

    public PublicController(PassService passService) {
        this.passService = passService;
    }

    @GetMapping("/verify-pass")
    public PassResponse verifyPass(@RequestParam String token) {
        PassResponse response = passService.verifyPass(token);
        // Phone numbers are included on the student's private pass, never in public verification.
        response.setPhoneNumber(null);
        return response;
    }
}
