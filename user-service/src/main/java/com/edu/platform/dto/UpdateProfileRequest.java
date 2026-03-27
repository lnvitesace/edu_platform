package com.edu.platform.dto;

import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 更新用户资料请求 DTO
 * 用于修改用户基本信息和扩展资料
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateProfileRequest {

    /** 名 */
    @Size(max = 50, message = "First name must not exceed 50 characters")
    private String firstName;

    /** 姓 */
    @Size(max = 50, message = "Last name must not exceed 50 characters")
    private String lastName;

    /** 手机号码 */
    @Size(max = 20, message = "Phone number must not exceed 20 characters")
    @Pattern(regexp = "^[0-9+\\-()\\s]*$", message = "Phone number format is invalid")
    private String phone;

    /** 头像 URL */
    private String avatarUrl;

    /** 个人简介 */
    private String bio;

    /** 出生日期，不能是未来日期 */
    @PastOrPresent(message = "Date of birth cannot be in the future")
    private LocalDate dateOfBirth;

    /** 性别 */
    @Size(max = 10, message = "Gender must not exceed 10 characters")
    private String gender;

    /** 国家 */
    @Size(max = 50, message = "Country must not exceed 50 characters")
    private String country;

    /** 城市 */
    @Size(max = 50, message = "City must not exceed 50 characters")
    private String city;

    /** 详细地址 */
    @Size(max = 255, message = "Address must not exceed 255 characters")
    private String address;

    /** 邮政编码 */
    @Size(max = 20, message = "Postal code must not exceed 20 characters")
    private String postalCode;

    /** 学历水平 */
    @Size(max = 50, message = "Education level must not exceed 50 characters")
    private String educationLevel;

    /** 兴趣爱好 */
    private String interests;
}
