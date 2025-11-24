package com.educode.educodeApi.DTO.homework;

import java.time.LocalDateTime;

public record HomeworkViewDTO(
        Long id,
        String title,
        String content,
        Long groupId,
        String groupTitle,
        Long puzzleId,
        String puzzleTitle,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime deadline,
        boolean hasSubmitted,
        long submissionsCount,
        boolean isExpired
) {}
