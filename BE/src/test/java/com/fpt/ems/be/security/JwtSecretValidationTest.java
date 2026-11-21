package com.fpt.ems.be.security;

import com.fpt.ems.be.util.JwtUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("SEC-UNIT: Kiểm thử chính sách hợp lệ hóa JWT_SECRET môi trường Non-Dev")
class JwtSecretValidationTest {

    @Test
    @DisplayName("Dev profile: Cho phép sử dụng secret mẫu ban đầu để phát triển thuận tiện")
    void devProfile_allowsSampleSecret() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("dev");

        JwtUtils jwtUtils = new JwtUtils(env);
        jwtUtils.setJwtSecret(JwtUtils.DEFAULT_SAMPLE_SECRET);

        assertThatCode(jwtUtils::validateSecretKey).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Non-Dev profile: Từ chối nếu JWT_SECRET dưới 32 bytes (256 bits)")
    void nonDevProfile_rejectsShortSecret() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("prod");

        JwtUtils jwtUtils = new JwtUtils(env);
        jwtUtils.setJwtSecret("short_secret_under_32_bytes!");

        assertThatThrownBy(jwtUtils::validateSecretKey)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("tối thiểu 32 byte");
    }

    @Test
    @DisplayName("Non-Dev profile: Từ chối nếu JWT_SECRET vẫn là giá trị mẫu mặc định của repo")
    void nonDevProfile_rejectsDefaultSampleSecret() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("prod");

        JwtUtils jwtUtils = new JwtUtils(env);
        jwtUtils.setJwtSecret(JwtUtils.DEFAULT_SAMPLE_SECRET);

        assertThatThrownBy(jwtUtils::validateSecretKey)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Không được sử dụng JWT_SECRET mặc định hoặc giá trị mẫu");
    }

    @Test
    @DisplayName("Non-Dev profile: Từ chối nếu JWT_SECRET chứa từ khóa 'example' hoặc 'sample'")
    void nonDevProfile_rejectsSampleKeywordsInSecret() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("prod");

        JwtUtils jwtUtils = new JwtUtils(env);
        jwtUtils.setJwtSecret("this_is_an_example_production_key_length_32_chars");

        assertThatThrownBy(jwtUtils::validateSecretKey)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Không được sử dụng JWT_SECRET mặc định hoặc giá trị mẫu");
    }

    @Test
    @DisplayName("Non-Dev profile: Chấp nhận khi JWT_SECRET có độ dài >= 32 byte và là bí mật thực sự")
    void nonDevProfile_acceptsSecureProductionSecret() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("prod");

        JwtUtils jwtUtils = new JwtUtils(env);
        jwtUtils.setJwtSecret("c43bf0865d2146e2978bb7ad428f7311_very_secure_strong_production_jwt_key_2026");

        assertThatCode(jwtUtils::validateSecretKey).doesNotThrowAnyException();
    }
}

