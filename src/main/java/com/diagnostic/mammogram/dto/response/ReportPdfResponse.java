package com.diagnostic.mammogram.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class ReportPdfResponse {
    private Long reportId;
    private String filename;
    private long fileSize;
    private byte[] content;

}