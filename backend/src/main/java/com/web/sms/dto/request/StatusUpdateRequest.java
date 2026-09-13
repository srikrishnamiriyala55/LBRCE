package com.web.sms.dto.request;
import jakarta.validation.constraints.NotBlank;
public class StatusUpdateRequest { @NotBlank private String status; private String response; public String getStatus(){return status;} public void setStatus(String v){status=v;} public String getResponse(){return response;} public void setResponse(String v){response=v;} }
