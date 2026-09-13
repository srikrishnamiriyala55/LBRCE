package com.web.sms.service;

import com.web.sms.dto.response.DashboardResponse;
import com.web.sms.dto.response.StudentProfileResponse;

public interface StudentService {
    StudentProfileResponse getProfile(Long studentId);
    DashboardResponse getDashboard(Long studentId);
    StudentProfileResponse updateProfile(Long studentId, StudentProfileResponse dto);
}
