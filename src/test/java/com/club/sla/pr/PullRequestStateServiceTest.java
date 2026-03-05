package com.club.sla.pr;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
class PullRequestStateServiceTest {

  @Container
  static PostgreSQLContainer<?> postgres =
      new PostgreSQLContainer<>("postgres:16-alpine")
          .withDatabaseName("club_sla")
          .withUsername("club_sla")
          .withPassword("club_sla");

  @DynamicPropertySource
  static void overrideProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
    registry.add("spring.flyway.enabled", () -> true);
  }

  @Autowired private PullRequestStateService pullRequestStateService;

  @Autowired private PullRequestStateRepository pullRequestStateRepository;

  @Test
  void startsTrackingWhenReadyForReview() {
    PullRequestState state = pullRequestStateService.onReadyForReview(100L, 12L, "alice");

    assertThat(state.getStatus()).isEqualTo(PullRequestStatus.READY);
    assertThat(state.getReadyAt()).isNotNull();
    assertThat(state.getFirstReviewAt()).isNull();
  }

  @Test
  void pausesWhenConvertedToDraft() {
    pullRequestStateService.onReadyForReview(100L, 13L, "alice");

    PullRequestState state = pullRequestStateService.onDraft(100L, 13L);

    assertThat(state.getStatus()).isEqualTo(PullRequestStatus.DRAFT);
  }

  @Test
  void storesFirstReviewOnlyOnce() {
    pullRequestStateService.onReadyForReview(100L, 14L, "alice");

    PullRequestState first = pullRequestStateService.onFirstReview(100L, 14L, "mentor");
    PullRequestState second = pullRequestStateService.onFirstReview(100L, 14L, "mentor-2");

    assertThat(first.getStatus()).isEqualTo(PullRequestStatus.REVIEWED);
    assertThat(first.getFirstReviewAt()).isNotNull();
    assertThat(second.getFirstReviewAt()).isEqualTo(first.getFirstReviewAt());

    PullRequestState persisted =
        pullRequestStateRepository.findByRepositoryIdAndPrNumber(100L, 14L).orElseThrow();
    assertThat(persisted.getStatus()).isEqualTo(PullRequestStatus.REVIEWED);
  }
}
