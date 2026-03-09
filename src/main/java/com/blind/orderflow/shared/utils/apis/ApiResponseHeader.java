package com.blind.orderflow.shared.utils.apis;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Builder
@AllArgsConstructor
@Getter
@Setter
public class ApiResponseHeader {
    private String requestRefId;
    private int responseCode;
    private String responseMessage;
    private String customerMessage;
    private LocalDateTime timestamp;
}
