# Club PR SLA Tool Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Build an MVP service that tracks GitHub PR review SLA and sends 12h/24h/36h Discord notifications for club/study teams.

**Architecture:** Use a Spring Boot monolith with clear module boundaries: webhook ingestion, PR state store, SLA evaluation engine, notification dispatcher, and dashboard API. Keep delivery idempotent and scheduler-driven (5-minute scans) to reduce system complexity in the first release.

**Tech Stack:** Java 21, Spring Boot 3.x, Gradle Kotlin DSL, PostgreSQL, Redis (optional lock/cache), Flyway, JUnit 5, Testcontainers, MockMvc, Micrometer/Prometheus, Docker Compose.

---

## Rules for Execution

- Follow @test-driven-development for every behavior change.
- Run @verification-before-completion checks before claiming each task done.
- Keep each commit focused to one task.
- YAGNI: do not add AI analysis, GitLab support, or gamification in MVP.

### Task 1: Bootstrap service and health endpoint

**Files:**
- Create: `settings.gradle.kts`
- Create: `build.gradle.kts`
- Create: `src/main/java/com/club/sla/SlaApplication.java`
- Create: `src/main/java/com/club/sla/web/HealthController.java`
- Create: `src/test/java/com/club/sla/web/HealthControllerTest.java`

**Step 1: Write the failing test**

```java
@WebMvcTest(HealthController.class)
class HealthControllerTest {
    @Autowired
    private MockMvc mvc;

    @Test
    void returnsOk() throws Exception {
        mvc.perform(get("/api/health"))
            .andExpect(status().isOk())
            .andExpect(content().string("ok"));
    }
}
```

**Step 2: Run test to verify it fails**

Run: `./gradlew test --tests "com.club.sla.web.HealthControllerTest" -i`  
Expected: FAIL because controller/application files do not exist.

**Step 3: Write minimal implementation**

```java
@RestController
@RequestMapping("/api")
public class HealthController {
    @GetMapping("/health")
    public String health() {
        return "ok";
    }
}
```

**Step 4: Run test to verify it passes**

Run: `./gradlew test --tests "com.club.sla.web.HealthControllerTest"`  
Expected: PASS.

**Step 5: Commit**

```bash
git add settings.gradle.kts build.gradle.kts src/main src/test
git commit -m "feat: bootstrap spring service with health endpoint"
```

### Task 2: Webhook signature verification endpoint

**Files:**
- Create: `src/main/java/com/club/sla/webhook/GithubWebhookController.java`
- Create: `src/main/java/com/club/sla/webhook/GithubSignatureVerifier.java`
- Create: `src/main/java/com/club/sla/webhook/WebhookProperties.java`
- Create: `src/test/java/com/club/sla/webhook/GithubWebhookControllerTest.java`
- Modify: `src/main/resources/application.yml`

**Step 1: Write the failing tests**

```java
@WebMvcTest(GithubWebhookController.class)
class GithubWebhookControllerTest {
    @Autowired MockMvc mvc;
    @MockBean GithubSignatureVerifier verifier;

    @Test
    void rejectsInvalidSignature() throws Exception {
        when(verifier.isValid(anyString(), anyString())).thenReturn(false);
        mvc.perform(post("/api/webhooks/github")
                .header("X-Hub-Signature-256", "sha256=bad")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"action\":\"opened\"}"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void acceptsValidSignature() throws Exception {
        when(verifier.isValid(anyString(), anyString())).thenReturn(true);
        mvc.perform(post("/api/webhooks/github")
                .header("X-Hub-Signature-256", "sha256=ok")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"action\":\"opened\"}"))
            .andExpect(status().isAccepted());
    }
}
```

**Step 2: Run tests to verify they fail**

Run: `./gradlew test --tests "com.club.sla.webhook.GithubWebhookControllerTest"`  
Expected: FAIL due to missing controller and verifier.

**Step 3: Write minimal implementation**

```java
@PostMapping("/api/webhooks/github")
ResponseEntity<Void> receive(
    @RequestHeader("X-Hub-Signature-256") String signature,
    @RequestBody String payload
) {
    return verifier.isValid(payload, signature)
        ? ResponseEntity.accepted().build()
        : ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
}
```

**Step 4: Re-run tests**

Run: `./gradlew test --tests "com.club.sla.webhook.GithubWebhookControllerTest"`  
Expected: PASS.

**Step 5: Commit**

```bash
git add src/main/java/com/club/sla/webhook src/main/resources/application.yml src/test/java/com/club/sla/webhook
git commit -m "feat: add github webhook endpoint with signature verification"
```

### Task 3: Idempotent webhook delivery storage

**Files:**
- Create: `src/main/java/com/club/sla/webhook/DeliveryEvent.java`
- Create: `src/main/java/com/club/sla/webhook/DeliveryEventRepository.java`
- Create: `src/main/java/com/club/sla/webhook/DeliveryIngestionService.java`
- Create: `src/main/resources/db/migration/V1__create_delivery_events.sql`
- Create: `src/test/java/com/club/sla/webhook/DeliveryIngestionServiceTest.java`

**Step 1: Write the failing test**

```java
@DataJpaTest
class DeliveryIngestionServiceTest {
    @Autowired DeliveryEventRepository repo;

    @Test
    void ignoresDuplicateDeliveryId() {
        DeliveryIngestionService service = new DeliveryIngestionService(repo);
        service.record("delivery-1", "pull_request", "{}");
        service.record("delivery-1", "pull_request", "{}");

        assertThat(repo.count()).isEqualTo(1);
    }
}
```

**Step 2: Run test to verify it fails**

Run: `./gradlew test --tests "com.club.sla.webhook.DeliveryIngestionServiceTest"`  
Expected: FAIL because entity/repository/service not implemented.

**Step 3: Write minimal implementation**

```java
@Transactional
public void record(String deliveryId, String eventType, String payload) {
    if (repository.existsByDeliveryId(deliveryId)) return;
    repository.save(new DeliveryEvent(deliveryId, eventType, payload, Instant.now()));
}
```

**Step 4: Re-run test**

Run: `./gradlew test --tests "com.club.sla.webhook.DeliveryIngestionServiceTest"`  
Expected: PASS.

**Step 5: Commit**

```bash
git add src/main/java/com/club/sla/webhook src/main/resources/db/migration/V1__create_delivery_events.sql src/test/java/com/club/sla/webhook
git commit -m "feat: persist webhook deliveries with idempotency"
```

### Task 4: PR state tracking for ready/draft/review events

**Files:**
- Create: `src/main/java/com/club/sla/pr/PullRequestState.java`
- Create: `src/main/java/com/club/sla/pr/PullRequestRepository.java`
- Create: `src/main/java/com/club/sla/pr/PullRequestStateService.java`
- Create: `src/main/resources/db/migration/V2__create_pull_requests.sql`
- Create: `src/test/java/com/club/sla/pr/PullRequestStateServiceTest.java`

**Step 1: Write failing tests**

```java
class PullRequestStateServiceTest {
    @Test
    void startsTrackingWhenReadyForReview() {
        var state = service.onReadyForReview(repoId, 12L, "alice");
        assertThat(state.getReadyAt()).isNotNull();
        assertThat(state.getStatus()).isEqualTo("READY");
    }

    @Test
    void pausesWhenMovedToDraft() {
        service.onReadyForReview(repoId, 12L, "alice");
        var state = service.onDraft(repoId, 12L);
        assertThat(state.getStatus()).isEqualTo("DRAFT");
    }

    @Test
    void stopsWhenFirstReviewArrives() {
        service.onReadyForReview(repoId, 12L, "alice");
        var state = service.onFirstReview(repoId, 12L, "mentor");
        assertThat(state.getFirstReviewAt()).isNotNull();
        assertThat(state.getStatus()).isEqualTo("REVIEWED");
    }
}
```

**Step 2: Run tests to verify fail**

Run: `./gradlew test --tests "com.club.sla.pr.PullRequestStateServiceTest"`  
Expected: FAIL.

**Step 3: Write minimal implementation**

```java
public PullRequestState onFirstReview(Long repoId, Long prNumber, String reviewer) {
    PullRequestState state = load(repoId, prNumber);
    if (state.getFirstReviewAt() == null) {
        state.setFirstReviewAt(Instant.now(clock));
        state.setStatus("REVIEWED");
    }
    return repository.save(state);
}
```

**Step 4: Re-run tests**

Run: `./gradlew test --tests "com.club.sla.pr.PullRequestStateServiceTest"`  
Expected: PASS.

**Step 5: Commit**

```bash
git add src/main/java/com/club/sla/pr src/main/resources/db/migration/V2__create_pull_requests.sql src/test/java/com/club/sla/pr
git commit -m "feat: add pull request state tracking lifecycle"
```

### Task 5: SLA evaluation engine (12h/24h/36h)

**Files:**
- Create: `src/main/java/com/club/sla/sla/SlaPolicy.java`
- Create: `src/main/java/com/club/sla/sla/SlaEvaluation.java`
- Create: `src/main/java/com/club/sla/sla/SlaEngine.java`
- Create: `src/test/java/com/club/sla/sla/SlaEngineTest.java`

**Step 1: Write failing tests**

```java
class SlaEngineTest {
    @Test
    void emitsReminderAt12Hours() {
        var result = engine.evaluate(readyAt.minus(Duration.ofHours(12)), null, false, Set.of());
        assertThat(result.action()).isEqualTo(SlaAction.REMIND_12H);
    }

    @Test
    void emitsEscalationAt24Hours() {
        var result = engine.evaluate(readyAt.minus(Duration.ofHours(24)), null, false, Set.of("REMIND_12H"));
        assertThat(result.action()).isEqualTo(SlaAction.ESCALATE_24H);
    }

    @Test
    void emitsFallbackAt36HoursWhenEnabled() {
        var result = engine.evaluate(readyAt.minus(Duration.ofHours(36)), null, true, Set.of("REMIND_12H","ESCALATE_24H"));
        assertThat(result.action()).isEqualTo(SlaAction.FALLBACK_36H);
    }
}
```

**Step 2: Run tests to verify fail**

Run: `./gradlew test --tests "com.club.sla.sla.SlaEngineTest"`  
Expected: FAIL.

**Step 3: Write minimal implementation**

```java
if (elapsed.compareTo(Duration.ofHours(36)) >= 0 && fallbackEnabled && !sent.contains("FALLBACK_36H")) {
    return SlaEvaluation.of(SlaAction.FALLBACK_36H);
}
if (elapsed.compareTo(Duration.ofHours(24)) >= 0 && !sent.contains("ESCALATE_24H")) {
    return SlaEvaluation.of(SlaAction.ESCALATE_24H);
}
if (elapsed.compareTo(Duration.ofHours(12)) >= 0 && !sent.contains("REMIND_12H")) {
    return SlaEvaluation.of(SlaAction.REMIND_12H);
}
return SlaEvaluation.none();
```

**Step 4: Re-run tests**

Run: `./gradlew test --tests "com.club.sla.sla.SlaEngineTest"`  
Expected: PASS.

**Step 5: Commit**

```bash
git add src/main/java/com/club/sla/sla src/test/java/com/club/sla/sla
git commit -m "feat: implement SLA evaluation rules for 12h/24h/36h"
```

### Task 6: Notification dispatch and dedup logging

**Files:**
- Create: `src/main/java/com/club/sla/notify/NotificationPort.java`
- Create: `src/main/java/com/club/sla/notify/SlaNotificationService.java`
- Create: `src/main/java/com/club/sla/sla/SlaEventLog.java`
- Create: `src/main/java/com/club/sla/sla/SlaEventLogRepository.java`
- Create: `src/main/resources/db/migration/V3__create_sla_event_logs.sql`
- Create: `src/test/java/com/club/sla/notify/SlaNotificationServiceTest.java`

**Step 1: Write failing tests**

```java
class SlaNotificationServiceTest {
    @Test
    void sendsReminderOnlyOncePerPrStage() {
        service.dispatch(pr, SlaAction.REMIND_12H);
        service.dispatch(pr, SlaAction.REMIND_12H);

        verify(port, times(1)).send(any());
    }
}
```

**Step 2: Run tests to verify fail**

Run: `./gradlew test --tests "com.club.sla.notify.SlaNotificationServiceTest"`  
Expected: FAIL.

**Step 3: Write minimal implementation**

```java
if (logRepository.existsByRepoIdAndPrNumberAndStage(pr.repoId(), pr.number(), action.name())) {
    return;
}
port.send(NotificationMessage.from(pr, action));
logRepository.save(SlaEventLog.of(pr.repoId(), pr.number(), action.name(), Instant.now(clock)));
```

**Step 4: Re-run tests**

Run: `./gradlew test --tests "com.club.sla.notify.SlaNotificationServiceTest"`  
Expected: PASS.

**Step 5: Commit**

```bash
git add src/main/java/com/club/sla/notify src/main/java/com/club/sla/sla src/main/resources/db/migration/V3__create_sla_event_logs.sql src/test/java/com/club/sla/notify
git commit -m "feat: add notification dispatch with stage dedup logs"
```

### Task 7: Discord adapter and retry policy

**Files:**
- Create: `src/main/java/com/club/sla/notify/discord/DiscordWebhookClient.java`
- Create: `src/main/java/com/club/sla/notify/discord/DiscordNotificationAdapter.java`
- Create: `src/main/java/com/club/sla/notify/DeadLetterEvent.java`
- Create: `src/main/java/com/club/sla/notify/DeadLetterRepository.java`
- Create: `src/main/resources/db/migration/V4__create_dead_letters.sql`
- Create: `src/test/java/com/club/sla/notify/discord/DiscordNotificationAdapterTest.java`

**Step 1: Write failing tests**

```java
class DiscordNotificationAdapterTest {
    @Test
    void storesDeadLetterAfterThreeFailures() {
        when(client.post(any())).thenThrow(new RuntimeException("boom"));
        adapter.send(message);
        verify(deadLetterRepository).save(any());
    }
}
```

**Step 2: Run tests to verify fail**

Run: `./gradlew test --tests "com.club.sla.notify.discord.DiscordNotificationAdapterTest"`  
Expected: FAIL.

**Step 3: Write minimal implementation**

```java
for (int i = 0; i < 3; i++) {
    try {
        client.post(payload);
        return;
    } catch (Exception ignored) {
    }
}
deadLetterRepository.save(DeadLetterEvent.of("DISCORD_SEND_FAILED", payload));
```

**Step 4: Re-run tests**

Run: `./gradlew test --tests "com.club.sla.notify.discord.DiscordNotificationAdapterTest"`  
Expected: PASS.

**Step 5: Commit**

```bash
git add src/main/java/com/club/sla/notify src/main/resources/db/migration/V4__create_dead_letters.sql src/test/java/com/club/sla/notify/discord
git commit -m "feat: add discord adapter with retry and dead letter fallback"
```

### Task 8: SLA scanner scheduler with lock

**Files:**
- Create: `src/main/java/com/club/sla/sla/SlaScannerJob.java`
- Create: `src/main/java/com/club/sla/sla/SchedulerLockService.java`
- Create: `src/test/java/com/club/sla/sla/SlaScannerJobTest.java`
- Modify: `src/main/resources/application.yml`

**Step 1: Write failing tests**

```java
class SlaScannerJobTest {
    @Test
    void skipsWhenLockNotAcquired() {
        when(lockService.tryLock("sla-scan")).thenReturn(false);
        job.scan();
        verifyNoInteractions(notificationService);
    }

    @Test
    void scansWhenLockAcquired() {
        when(lockService.tryLock("sla-scan")).thenReturn(true);
        job.scan();
        verify(prRepository).findTrackable();
    }
}
```

**Step 2: Run tests to verify fail**

Run: `./gradlew test --tests "com.club.sla.sla.SlaScannerJobTest"`  
Expected: FAIL.

**Step 3: Write minimal implementation**

```java
@Scheduled(fixedDelayString = "${sla.scan-interval-ms:300000}")
public void scan() {
    if (!lockService.tryLock("sla-scan")) return;
    try {
        // load trackable PRs, evaluate SLA, dispatch notifications
    } finally {
        lockService.unlock("sla-scan");
    }
}
```

**Step 4: Re-run tests**

Run: `./gradlew test --tests "com.club.sla.sla.SlaScannerJobTest"`  
Expected: PASS.

**Step 5: Commit**

```bash
git add src/main/java/com/club/sla/sla src/test/java/com/club/sla/sla src/main/resources/application.yml
git commit -m "feat: schedule SLA scanner with lock protection"
```

### Task 9: Dashboard API for admin visibility

**Files:**
- Create: `src/main/java/com/club/sla/dashboard/DashboardController.java`
- Create: `src/main/java/com/club/sla/dashboard/DashboardQueryService.java`
- Create: `src/main/java/com/club/sla/dashboard/DashboardDto.java`
- Create: `src/test/java/com/club/sla/dashboard/DashboardControllerTest.java`

**Step 1: Write failing test**

```java
@WebMvcTest(DashboardController.class)
class DashboardControllerTest {
    @Autowired MockMvc mvc;
    @MockBean DashboardQueryService queryService;

    @Test
    void returnsSlaSummary() throws Exception {
        when(queryService.fetch(anyLong())).thenReturn(new DashboardDto(4, 2, 1));
        mvc.perform(get("/api/repositories/1/dashboard"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.onTrack").value(4))
            .andExpect(jsonPath("$.atRisk").value(2))
            .andExpect(jsonPath("$.breached").value(1));
    }
}
```

**Step 2: Run test to verify fail**

Run: `./gradlew test --tests "com.club.sla.dashboard.DashboardControllerTest"`  
Expected: FAIL.

**Step 3: Write minimal implementation**

```java
@GetMapping("/api/repositories/{repositoryId}/dashboard")
public DashboardDto dashboard(@PathVariable Long repositoryId) {
    return queryService.fetch(repositoryId);
}
```

**Step 4: Re-run test**

Run: `./gradlew test --tests "com.club.sla.dashboard.DashboardControllerTest"`  
Expected: PASS.

**Step 5: Commit**

```bash
git add src/main/java/com/club/sla/dashboard src/test/java/com/club/sla/dashboard
git commit -m "feat: add admin dashboard summary API"
```

### Task 10: Observability and end-to-end verification

**Files:**
- Create: `src/main/java/com/club/sla/metrics/SlaMetrics.java`
- Create: `src/test/java/com/club/sla/e2e/SlaFlowE2ETest.java`
- Create: `docker-compose.yml`
- Create: `README.md`

**Step 1: Write failing E2E test**

```java
@SpringBootTest
class SlaFlowE2ETest {
    @Test
    void fullFlowReadyReminderEscalationReviewStop() {
        // seed PR ready_at, advance clock to 12h then 24h, assert notifications
        // submit first review, assert scanner emits no further actions
    }
}
```

**Step 2: Run test to verify fail**

Run: `./gradlew test --tests "com.club.sla.e2e.SlaFlowE2ETest"`  
Expected: FAIL until flow wiring and clock control are complete.

**Step 3: Implement minimal wiring**

```java
Counter.builder("sla_notifications_total")
    .tag("stage", action.name())
    .register(meterRegistry)
    .increment();
```

**Step 4: Run full verification suite**

Run: `./gradlew test`  
Expected: PASS for unit/integration/E2E tests.

**Step 5: Commit**

```bash
git add src/main/java/com/club/sla/metrics src/test/java/com/club/sla/e2e docker-compose.yml README.md
git commit -m "chore: add observability and end-to-end verification flow"
```

## Final Verification Checklist

Run in order:

1. `./gradlew clean test`
Expected: all tests pass.
2. `./gradlew bootRun` then `curl localhost:8080/api/health`
Expected: `ok`.
3. Send sample webhook payload with valid signature to `/api/webhooks/github`
Expected: HTTP 202.
4. Validate metrics endpoint `/actuator/prometheus`
Expected: includes `sla_notifications_total`.

## Deliverables

1. Running Spring Boot service with webhook + SLA engine + dashboard API
2. Database migrations (`V1`..`V4`) and local compose stack
3. Test suite covering SLA policy and PR lifecycle
4. README with setup, local run, and verification commands
