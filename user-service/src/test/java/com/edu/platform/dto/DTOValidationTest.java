package com.edu.platform.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for DTO validation
 */
@DisplayName("DTO Validation Tests")
class DTOValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    // ==========================================
    // RegisterRequest Validation Tests
    // ==========================================

    @Test
    @DisplayName("RegisterRequest - Valid request passes validation")
    void registerRequest_Valid() {
        RegisterRequest request = RegisterRequest.builder()
                .username("validuser")
                .email("valid@example.com")
                .password("ValidPass123")
                .firstName("John")
                .lastName("Doe")
                .phone("+1234567890")
                .role("STUDENT")
                .build();

        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("RegisterRequest - Username cannot be blank")
    void registerRequest_UsernameBlank() {
        RegisterRequest request = RegisterRequest.builder()
                .username("")
                .email("valid@example.com")
                .password("ValidPass123")
                .firstName("John")
                .lastName("Doe")
                .build();

        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("username"));
    }

    @Test
    @DisplayName("RegisterRequest - Email must be valid format")
    void registerRequest_InvalidEmail() {
        RegisterRequest request = RegisterRequest.builder()
                .username("validuser")
                .email("invalid-email")
                .password("ValidPass123")
                .firstName("John")
                .lastName("Doe")
                .build();

        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("email"));
    }

    @Test
    @DisplayName("RegisterRequest - Password cannot be blank")
    void registerRequest_PasswordBlank() {
        RegisterRequest request = RegisterRequest.builder()
                .username("validuser")
                .email("valid@example.com")
                .password("")
                .firstName("John")
                .lastName("Doe")
                .build();

        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("password"));
    }

    // ==========================================
    // LoginRequest Validation Tests
    // ==========================================

    @Test
    @DisplayName("LoginRequest - Valid request passes validation")
    void loginRequest_Valid() {
        LoginRequest request = LoginRequest.builder()
                .usernameOrEmail("testuser")
                .password("Password123")
                .build();

        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("LoginRequest - UsernameOrEmail cannot be blank")
    void loginRequest_UsernameOrEmailBlank() {
        LoginRequest request = LoginRequest.builder()
                .usernameOrEmail("")
                .password("Password123")
                .build();

        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("usernameOrEmail"));
    }

    @Test
    @DisplayName("LoginRequest - Password cannot be blank")
    void loginRequest_PasswordBlank() {
        LoginRequest request = LoginRequest.builder()
                .usernameOrEmail("testuser")
                .password("")
                .build();

        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("password"));
    }

    // ==========================================
    // UpdateProfileRequest Validation Tests
    // ==========================================

    @Test
    @DisplayName("UpdateProfileRequest - All fields are optional")
    void updateProfileRequest_AllFieldsOptional() {
        UpdateProfileRequest request = UpdateProfileRequest.builder().build();

        Set<ConstraintViolation<UpdateProfileRequest>> violations = validator.validate(request);
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("UpdateProfileRequest - Can update single field")
    void updateProfileRequest_SingleField() {
        UpdateProfileRequest request = UpdateProfileRequest.builder()
                .firstName("UpdatedName")
                .build();

        Set<ConstraintViolation<UpdateProfileRequest>> violations = validator.validate(request);
        assertThat(violations).isEmpty();
    }

    // Length constraint tests for UpdateProfileRequest

    @Test
    @DisplayName("UpdateProfileRequest - Country exactly at max length (50) should pass")
    void updateProfileRequest_CountryAtMaxLength() {
        String countryAtMax = "a".repeat(50); // Exactly 50 characters
        UpdateProfileRequest request = UpdateProfileRequest.builder()
                .country(countryAtMax)
                .build();

        Set<ConstraintViolation<UpdateProfileRequest>> violations = validator.validate(request);
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("UpdateProfileRequest - Country exceeding max length (51) should fail")
    void updateProfileRequest_CountryExceedsMaxLength() {
        String countryTooLong = "a".repeat(51); // 51 characters - exceeds limit
        UpdateProfileRequest request = UpdateProfileRequest.builder()
                .country(countryTooLong)
                .build();

        Set<ConstraintViolation<UpdateProfileRequest>> violations = validator.validate(request);
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("country"));
    }

    @Test
    @DisplayName("UpdateProfileRequest - City exactly at max length (50) should pass")
    void updateProfileRequest_CityAtMaxLength() {
        String cityAtMax = "a".repeat(50);
        UpdateProfileRequest request = UpdateProfileRequest.builder()
                .city(cityAtMax)
                .build();

        Set<ConstraintViolation<UpdateProfileRequest>> violations = validator.validate(request);
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("UpdateProfileRequest - City exceeding max length (51) should fail")
    void updateProfileRequest_CityExceedsMaxLength() {
        String cityTooLong = "a".repeat(51);
        UpdateProfileRequest request = UpdateProfileRequest.builder()
                .city(cityTooLong)
                .build();

        Set<ConstraintViolation<UpdateProfileRequest>> violations = validator.validate(request);
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("city"));
    }

    @Test
    @DisplayName("UpdateProfileRequest - FirstName exactly at max length (50) should pass")
    void updateProfileRequest_FirstNameAtMaxLength() {
        String firstNameAtMax = "a".repeat(50);
        UpdateProfileRequest request = UpdateProfileRequest.builder()
                .firstName(firstNameAtMax)
                .build();

        Set<ConstraintViolation<UpdateProfileRequest>> violations = validator.validate(request);
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("UpdateProfileRequest - FirstName exceeding max length (51) should fail")
    void updateProfileRequest_FirstNameExceedsMaxLength() {
        String firstNameTooLong = "a".repeat(51);
        UpdateProfileRequest request = UpdateProfileRequest.builder()
                .firstName(firstNameTooLong)
                .build();

        Set<ConstraintViolation<UpdateProfileRequest>> violations = validator.validate(request);
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("firstName"));
    }

    @Test
    @DisplayName("UpdateProfileRequest - LastName exactly at max length (50) should pass")
    void updateProfileRequest_LastNameAtMaxLength() {
        String lastNameAtMax = "a".repeat(50);
        UpdateProfileRequest request = UpdateProfileRequest.builder()
                .lastName(lastNameAtMax)
                .build();

        Set<ConstraintViolation<UpdateProfileRequest>> violations = validator.validate(request);
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("UpdateProfileRequest - LastName exceeding max length (51) should fail")
    void updateProfileRequest_LastNameExceedsMaxLength() {
        String lastNameTooLong = "a".repeat(51);
        UpdateProfileRequest request = UpdateProfileRequest.builder()
                .lastName(lastNameTooLong)
                .build();

        Set<ConstraintViolation<UpdateProfileRequest>> violations = validator.validate(request);
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("lastName"));
    }

    @Test
    @DisplayName("UpdateProfileRequest - Phone exactly at max length (20) should pass")
    void updateProfileRequest_PhoneAtMaxLength() {
        String phoneAtMax = "+1234567890123456789"; // 20 characters
        UpdateProfileRequest request = UpdateProfileRequest.builder()
                .phone(phoneAtMax)
                .build();

        Set<ConstraintViolation<UpdateProfileRequest>> violations = validator.validate(request);
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("UpdateProfileRequest - Phone exceeding max length (21) should fail")
    void updateProfileRequest_PhoneExceedsMaxLength() {
        String phoneTooLong = "+12345678901234567890"; // 21 characters
        UpdateProfileRequest request = UpdateProfileRequest.builder()
                .phone(phoneTooLong)
                .build();

        Set<ConstraintViolation<UpdateProfileRequest>> violations = validator.validate(request);
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("phone"));
    }

    @Test
    @DisplayName("UpdateProfileRequest - Phone with invalid characters should fail")
    void updateProfileRequest_PhoneInvalidFormat() {
        UpdateProfileRequest request = UpdateProfileRequest.builder()
                .phone("123-ABC-7890")
                .build();

        Set<ConstraintViolation<UpdateProfileRequest>> violations = validator.validate(request);
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("phone"));
    }

    @Test
    @DisplayName("UpdateProfileRequest - Phone with valid format should pass")
    void updateProfileRequest_PhoneValidFormat() {
        UpdateProfileRequest request = UpdateProfileRequest.builder()
                .phone("+1 (234) 567-8900")
                .build();

        Set<ConstraintViolation<UpdateProfileRequest>> violations = validator.validate(request);
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("UpdateProfileRequest - Address exactly at max length (255) should pass")
    void updateProfileRequest_AddressAtMaxLength() {
        String addressAtMax = "a".repeat(255);
        UpdateProfileRequest request = UpdateProfileRequest.builder()
                .address(addressAtMax)
                .build();

        Set<ConstraintViolation<UpdateProfileRequest>> violations = validator.validate(request);
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("UpdateProfileRequest - Address exceeding max length (256) should fail")
    void updateProfileRequest_AddressExceedsMaxLength() {
        String addressTooLong = "a".repeat(256);
        UpdateProfileRequest request = UpdateProfileRequest.builder()
                .address(addressTooLong)
                .build();

        Set<ConstraintViolation<UpdateProfileRequest>> violations = validator.validate(request);
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("address"));
    }

    @Test
    @DisplayName("UpdateProfileRequest - PostalCode exactly at max length (20) should pass")
    void updateProfileRequest_PostalCodeAtMaxLength() {
        String postalCodeAtMax = "a".repeat(20);
        UpdateProfileRequest request = UpdateProfileRequest.builder()
                .postalCode(postalCodeAtMax)
                .build();

        Set<ConstraintViolation<UpdateProfileRequest>> violations = validator.validate(request);
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("UpdateProfileRequest - PostalCode exceeding max length (21) should fail")
    void updateProfileRequest_PostalCodeExceedsMaxLength() {
        String postalCodeTooLong = "a".repeat(21);
        UpdateProfileRequest request = UpdateProfileRequest.builder()
                .postalCode(postalCodeTooLong)
                .build();

        Set<ConstraintViolation<UpdateProfileRequest>> violations = validator.validate(request);
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("postalCode"));
    }

    @Test
    @DisplayName("UpdateProfileRequest - EducationLevel exactly at max length (50) should pass")
    void updateProfileRequest_EducationLevelAtMaxLength() {
        String educationLevelAtMax = "a".repeat(50);
        UpdateProfileRequest request = UpdateProfileRequest.builder()
                .educationLevel(educationLevelAtMax)
                .build();

        Set<ConstraintViolation<UpdateProfileRequest>> violations = validator.validate(request);
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("UpdateProfileRequest - EducationLevel exceeding max length (51) should fail")
    void updateProfileRequest_EducationLevelExceedsMaxLength() {
        String educationLevelTooLong = "a".repeat(51);
        UpdateProfileRequest request = UpdateProfileRequest.builder()
                .educationLevel(educationLevelTooLong)
                .build();

        Set<ConstraintViolation<UpdateProfileRequest>> violations = validator.validate(request);
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("educationLevel"));
    }

    @Test
    @DisplayName("UpdateProfileRequest - Gender exactly at max length (10) should pass")
    void updateProfileRequest_GenderAtMaxLength() {
        String genderAtMax = "a".repeat(10);
        UpdateProfileRequest request = UpdateProfileRequest.builder()
                .gender(genderAtMax)
                .build();

        Set<ConstraintViolation<UpdateProfileRequest>> violations = validator.validate(request);
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("UpdateProfileRequest - Gender exceeding max length (11) should fail")
    void updateProfileRequest_GenderExceedsMaxLength() {
        String genderTooLong = "a".repeat(11);
        UpdateProfileRequest request = UpdateProfileRequest.builder()
                .gender(genderTooLong)
                .build();

        Set<ConstraintViolation<UpdateProfileRequest>> violations = validator.validate(request);
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("gender"));
    }

    @Test
    @DisplayName("UpdateProfileRequest - DateOfBirth in future should fail")
    void updateProfileRequest_DateOfBirthInFuture() {
        UpdateProfileRequest request = UpdateProfileRequest.builder()
                .dateOfBirth(java.time.LocalDate.now().plusDays(1))
                .build();

        Set<ConstraintViolation<UpdateProfileRequest>> violations = validator.validate(request);
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("dateOfBirth"));
    }

    @Test
    @DisplayName("UpdateProfileRequest - DateOfBirth today should pass")
    void updateProfileRequest_DateOfBirthToday() {
        UpdateProfileRequest request = UpdateProfileRequest.builder()
                .dateOfBirth(java.time.LocalDate.now())
                .build();

        Set<ConstraintViolation<UpdateProfileRequest>> violations = validator.validate(request);
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("UpdateProfileRequest - DateOfBirth in past should pass")
    void updateProfileRequest_DateOfBirthInPast() {
        UpdateProfileRequest request = UpdateProfileRequest.builder()
                .dateOfBirth(java.time.LocalDate.of(1990, 1, 1))
                .build();

        Set<ConstraintViolation<UpdateProfileRequest>> violations = validator.validate(request);
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("UpdateProfileRequest - Multiple fields exceeding limits should produce multiple violations")
    void updateProfileRequest_MultipleViolations() {
        UpdateProfileRequest request = UpdateProfileRequest.builder()
                .country("a".repeat(51))
                .city("b".repeat(51))
                .phone("invalid-phone-with-letters")
                .dateOfBirth(java.time.LocalDate.now().plusYears(1))
                .build();

        Set<ConstraintViolation<UpdateProfileRequest>> violations = validator.validate(request);
        assertThat(violations).hasSizeGreaterThanOrEqualTo(4);
    }

    // ==========================================
    // AuthResponse Tests
    // ==========================================

    @Test
    @DisplayName("AuthResponse - Builder creates valid response")
    void authResponse_BuilderWorks() {
        UserResponse userResponse = UserResponse.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .role("STUDENT")
                .build();

        AuthResponse response = AuthResponse.builder()
                .accessToken("access.token.jwt")
                .refreshToken("refresh.token.jwt")
                .tokenType("Bearer")
                .expiresIn(86400000L)
                .user(userResponse)
                .build();

        assertThat(response.getAccessToken()).isEqualTo("access.token.jwt");
        assertThat(response.getRefreshToken()).isEqualTo("refresh.token.jwt");
        assertThat(response.getTokenType()).isEqualTo("Bearer");
        assertThat(response.getExpiresIn()).isEqualTo(86400000L);
        assertThat(response.getUser()).isNotNull();
        assertThat(response.getUser().getUsername()).isEqualTo("testuser");
    }

    // ==========================================
    // UserResponse Tests
    // ==========================================

    @Test
    @DisplayName("UserResponse - Builder creates valid response with nested profile")
    void userResponse_WithProfile() {
        UserProfileResponse profileResponse = UserProfileResponse.builder()
                .id(1L)
                .bio("Test bio")
                .country("USA")
                .city("New York")
                .build();

        UserResponse response = UserResponse.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .firstName("Test")
                .lastName("User")
                .role("STUDENT")
                .status("ACTIVE")
                .emailVerified(true)
                .profile(profileResponse)
                .build();

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getUsername()).isEqualTo("testuser");
        assertThat(response.getProfile()).isNotNull();
        assertThat(response.getProfile().getBio()).isEqualTo("Test bio");
        assertThat(response.getProfile().getCity()).isEqualTo("New York");
    }
}
