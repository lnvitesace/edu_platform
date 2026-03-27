package com.edu.platform.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 用户资料实体类
 * 存储用户的扩展信息，与 User 实体一对一关联
 */
@Entity
@Table(name = "user_profiles")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfile {

    /** 主键 ID，自增 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 关联的用户，一对一关系 */
    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    /** 个人简介 */
    @Column(columnDefinition = "TEXT")
    private String bio;

    /** 出生日期 */
    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    /** 性别 */
    @Column(length = 10)
    private String gender;

    /** 国家 */
    @Column(length = 50)
    private String country;

    /** 城市 */
    @Column(length = 50)
    private String city;

    /** 详细地址 */
    @Column(length = 255)
    private String address;

    /** 邮政编码 */
    @Column(name = "postal_code", length = 20)
    private String postalCode;

    /** 学历水平 */
    @Column(name = "education_level", length = 50)
    private String educationLevel;

    /** 兴趣爱好 */
    @Column(columnDefinition = "TEXT")
    private String interests;

    /** 创建时间，自动生成 */
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    /** 更新时间，自动更新 */
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
