package com.diagnostic.mammogram.dto.response;

import java.time.LocalDateTime;

public record ReportDto(
        Long id,
        String findings,
        String recommendations,
        Long mammogramId,
        Long createdById,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}