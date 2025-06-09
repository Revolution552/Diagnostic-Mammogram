package com.diagnostic.mammogram.dto.request;


import lombok.Data;

@Data
public class ReportRequest {
    private String findings;
    private String recommendations;

}