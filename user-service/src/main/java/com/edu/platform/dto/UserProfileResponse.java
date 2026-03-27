package com.edu.platform.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 用户资料响应 DTO
 * 返回用户扩展资料，空字段不序列化
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserProfileResponse {

    /** 资料 ID */
    private Long id;

    /** 个人简介 */
    private String bio;

    /** 出生日期 */
    private LocalDate dateOfBirth;

    /** 性别 */
    private String gender;

    /** 国家 */
    private String country;

    /** 城市 */
    private String city;

    /** 详细地址 */
    private String address;

    /** 邮政编码 */
    private String postalCode;

    /** 学历水平 */
    private String educationLevel;

    /** 兴趣爱好 */
    private String interests;
}
