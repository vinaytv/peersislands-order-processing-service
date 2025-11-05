package com.pi.orders.web.dto;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

@Builder
@Data
public class ErrorResponse implements Serializable {

    private String status;
    private String details;

}
