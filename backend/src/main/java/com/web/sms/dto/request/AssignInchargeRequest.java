package com.web.sms.dto.request;
import jakarta.validation.constraints.NotNull;
public class AssignInchargeRequest { @NotNull private Long inchargeId; public Long getInchargeId(){return inchargeId;} public void setInchargeId(Long v){inchargeId=v;} }
