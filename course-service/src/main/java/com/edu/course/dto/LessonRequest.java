package com.edu.course.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LessonRequest {

    @NotBlank(message = "Lesson title is required")
    @Size(max = 200, message = "Lesson title must not exceed 200 characters")
    private String title;

    private String videoUrl;

    @Min(value = 0, message = "Duration must be non-negative")
    private Integer duration;

    private Boolean isFree;

    private Integer sortOrder;
}
