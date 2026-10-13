package com.web.sms.service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class OfficialBrandingService {
    private static final String COLLEGE_NAME = "Lakireddy Bali Reddy College of Engineering (Autonomous)";
    private static final String SYSTEM_NAME = "Bus Transportation Management System";

    private final String publicBaseUrl;

    public OfficialBrandingService(@Value("${btms.public-base-url:https://lbrce-btms-web.onrender.com}") String publicBaseUrl) {
        this.publicBaseUrl = normalizeBaseUrl(publicBaseUrl);
    }

    public String emailHeader(String documentTitle) {
        return """
                <div style="background:#1e40af;color:#fff;padding:20px 18px;text-align:center">
                  <img src="%s" alt="LBRCE College Logo" width="76" height="76"
                       style="display:block;width:76px;height:76px;object-fit:contain;margin:0 auto 10px;background:#fff;border-radius:50%%;padding:4px">
                  <div style="font-size:20px;line-height:1.35;font-weight:700">%s</div>
                  <div style="font-size:14px;margin-top:5px">%s</div>
                  <div style="font-size:13px;margin-top:7px;color:#dbeafe">%s</div>
                </div>
                """.formatted(logoUrl(), COLLEGE_NAME, SYSTEM_NAME, escape(documentTitle));
    }

    public String emailFooter() {
        return """
                <div style="border-top:1px solid #e5e7eb;padding:14px 20px;text-align:center;color:#6b7280;font-size:11px;line-height:1.5">
                  This is an official automated communication from the LBRCE Bus Transportation Management System.
                </div>
                """;
    }

    private String logoUrl() {
        // Keep the path segment encoded and the URL absolute because email clients cannot resolve relative assets.
        return publicBaseUrl + "/" + URLEncoder.encode("logo.jpg", StandardCharsets.UTF_8).replace("+", "%20");
    }

    private String normalizeBaseUrl(String value) {
        String normalized = StringUtils.hasText(value) ? value.trim() : "https://lbrce-btms-web.onrender.com";
        while (normalized.endsWith("/")) normalized = normalized.substring(0, normalized.length() - 1);
        return normalized;
    }

    private String escape(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }
}
