package com.bubbletea.user.integration;

import com.bubbletea.user.domain.member.Member;
import com.bubbletea.user.infrastructure.persistence.member.MemberJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class MemberApiIntegrationTest {

    private static final String EMAIL =
            "integration@example.com";

    private static final String NICKNAME =
            "통합테스터";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MemberJpaRepository memberJpaRepository;

    @BeforeEach
    void setUp() {
        memberJpaRepository.deleteAll();
    }

    @Test
    @DisplayName("내부 회원 생성 API 호출 시 회원을 DB에 저장한다")
    void createMemberSuccess() throws Exception {
        // when & then
        mockMvc.perform(
                        post("/internal/members")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "email": "integration@example.com",
                                          "nickname": "통합테스터"
                                        }
                                        """)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.memberId").isNumber())
                .andExpect(jsonPath("$.email").value(EMAIL))
                .andExpect(jsonPath("$.nickname").value(NICKNAME))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        Member savedMember =
                memberJpaRepository.findByEmail(EMAIL)
                        .orElseThrow();

        assertThat(savedMember.getEmail())
                .isEqualTo(EMAIL);

        assertThat(savedMember.getNickname())
                .isEqualTo(NICKNAME);

        assertThat(savedMember.getRole().name())
                .isEqualTo("USER");

        assertThat(savedMember.getStatus().name())
                .isEqualTo("ACTIVE");
    }

    @Test
    @DisplayName("이미 사용 중인 이메일로 회원을 생성하면 실패하고 기존 회원만 유지한다")
    void createMemberFailsWhenEmailAlreadyExists() {
        // given
        Member existingMember =
                memberJpaRepository.save(
                        Member.createUser(
                                EMAIL,
                                "기존회원"
                        )
                );

        // when & then
        assertThatThrownBy(
                () -> mockMvc.perform(
                        post("/internal/members")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "email": "integration@example.com",
                                          "nickname": "새회원"
                                        }
                                        """)
                )
        )
                .hasRootCauseInstanceOf(
                        IllegalArgumentException.class
                );

        assertThat(memberJpaRepository.count())
                .isEqualTo(1);

        Member remainingMember =
                memberJpaRepository.findByEmail(EMAIL)
                        .orElseThrow();

        assertThat(remainingMember.getId())
                .isEqualTo(existingMember.getId());

        assertThat(remainingMember.getNickname())
                .isEqualTo("기존회원");
    }

    @Test
    @DisplayName("탈퇴 회원의 인증 정보 조회 시 로그인이 불가능한 상태를 반환한다")
    void getAuthInfoReturnsUnavailableForWithdrawnMember()
            throws Exception {

        // given
        Member withdrawnMember =
                Member.createUser(
                        "withdrawn@example.com",
                        "탈퇴회원"
                );

        withdrawnMember.withdraw();

        Member savedMember =
                memberJpaRepository.save(withdrawnMember);

        // when & then
        mockMvc.perform(
                        get(
                                "/internal/members/{memberId}/auth-info",
                                savedMember.getId()
                        )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.memberId")
                                .value(savedMember.getId())
                )
                .andExpect(
                        jsonPath("$.email")
                                .value("withdrawn@example.com")
                )
                .andExpect(
                        jsonPath("$.nickname")
                                .value("탈퇴회원")
                )
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(
                        jsonPath("$.status")
                                .value("WITHDRAWN")
                )
                .andExpect(
                        jsonPath("$.loginAvailable")
                                .value(false)
                );
    }
}