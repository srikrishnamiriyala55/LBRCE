package com.web.sms.controller;

import com.web.sms.repository.AcademicYearRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/** Render readiness endpoint. A 200 means both the process and database work. */
@RestController
@RequestMapping("/api/public")
public class PublicHealthController {
    private final JdbcTemplate jdbc;
    private final AcademicYearRepository years;
    public PublicHealthController(JdbcTemplate jdbc,AcademicYearRepository years){this.jdbc=jdbc;this.years=years;}

    @GetMapping("/health")
    public ResponseEntity<Map<String,Object>> health(){
        Map<String,Object> body=new LinkedHashMap<>();
        body.put("timestamp",Instant.now().toString());
        try{
            jdbc.queryForObject("select 1",Integer.class);
            body.put("status","UP");
            body.put("database","UP");
            body.put("activeAcademicYear",years.findByActiveTrue().map(y->y.getYearName()).orElse("NOT_CONFIGURED"));
            return ResponseEntity.ok(body);
        }catch(Exception failure){
            body.put("status","DOWN");body.put("database","DOWN");
            return ResponseEntity.status(503).body(body);
        }
    }
}
