package com.web.sms.dto.request;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
public class SeatAssignmentRequest { @NotNull @Min(1) private Integer seatNumber; public Integer getSeatNumber(){return seatNumber;} public void setSeatNumber(Integer v){seatNumber=v;} }
