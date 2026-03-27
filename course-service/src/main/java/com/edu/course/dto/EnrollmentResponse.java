package com.edu.course.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EnrollmentResponse {

    private Long id;
    private Long courseId;
    private String courseTitle;
    private String courseCoverImage;
    private Long instructorId;
    private String status;
    private LocalDateTime enrolledAt;
}
