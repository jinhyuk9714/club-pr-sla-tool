package com.club.sla.pr;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PullRequestStateRepository extends JpaRepository<PullRequestState, Long> {

  Optional<PullRequestState> findByRepositoryIdAndPrNumber(Long repositoryId, Long prNumber);

  List<PullRequestState> findByStatusAndReadyAtIsNotNullAndFirstReviewAtIsNull(
      PullRequestStatus status);
}
