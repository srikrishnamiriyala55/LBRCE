package com.web.sms.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class OfficialBrandingServiceTest {
    @Test
    void createsAbsoluteOfficialHeaderAndEscapesDocumentTitle() {
        OfficialBrandingService service = new OfficialBrandingService("https://btms.example.edu/");

        String header = service.emailHeader("Receipt <Verified>");

        assertTrue(header.contains("https://btms.example.edu/logo.jpg"));
        assertTrue(header.contains("Lakireddy Bali Reddy College of Engineering (Autonomous)"));
        assertTrue(header.contains("Receipt &lt;Verified&gt;"));
        assertFalse(header.contains("Receipt <Verified>"));
    }
}
