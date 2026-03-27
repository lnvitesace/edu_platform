package com.edu.course.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 课时进度响应 DTO。
 *
 * 返回单个课时的观看进度信息，用于恢复播放位置和显示完成状态。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LessonProgressResponse {

    private Long lessonId;
    private Integer watchedSeconds;
    private Boolean completed;
    private LocalDateTime lastWatchedAt;
    private Integer totalDuration;
}
