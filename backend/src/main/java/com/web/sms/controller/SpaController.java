package com.web.sms.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** Forwards client-side routes to React while leaving /api and static assets untouched. */
@Controller
public class SpaController {

    @GetMapping({
            "/login",
            "/student", "/student/**",
            "/incharge", "/incharge/**",
            "/admin", "/admin/**"
    })
    public String forwardSpaRoutes() {
        return "forward:/index.html";
    }
}
