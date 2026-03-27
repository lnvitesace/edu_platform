package com.edu.course.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 进度上报请求 DTO。
 *
 * 前端视频播放器定时上报观看进度。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProgressRequest {

    @NotNull(message = "lessonId is required")
    private Long lessonId;

    @NotNull(message = "watchedSeconds is required")
    @Min(value = 0, message = "watchedSeconds must be >= 0")
    private Integer watchedSeconds;
}
