package com.edu.course.dto;

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
public class ChapterRequest {

    @NotBlank(message = "Chapter title is required")
    @Size(max = 200, message = "Chapter title must not exceed 200 characters")
    private String title;

    private Integer sortOrder;
}
