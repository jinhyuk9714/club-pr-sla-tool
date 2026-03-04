# Club PR SLA Tool Design

**Date:** 2026-03-05  
**Status:** Approved

## 1) Problem and Target

Build a lightweight review-operations layer for clubs/study groups using GitHub + Discord.

- Target users: club admins, mentors, mentees
- Core pain: pull requests stay unreviewed for too long
- Primary outcome: improve first-review response within a 24-hour SLA

## 2) Product Scope (MVP)

### Included

1. Track PR lifecycle from `Ready for review`
2. 12-hour reminder to assigned reviewer
3. 24-hour escalation to mentor/admin
4. Optional 36-hour fallback reviewer assignment
5. Simple dashboard showing `on-track`, `at-risk`, `breached`

### Excluded (MVP out-of-scope)

1. AI code quality scoring
2. Cross-platform integrations (GitLab/Jira/etc.)
3. Complex ranking/gamification

## 3) Approaches Considered

### A. GitHub App + periodic SLA scanner (recommended)

- Webhooks ingest PR/review state
- Scheduler scans every 5 minutes for SLA milestones
- Fast to build, low operational complexity

### B. Queue-driven delayed jobs

- Per-event delayed jobs at 12h/24h/36h
- More real-time precision, higher complexity

### C. GitHub Actions-centric approach

- Low server complexity, constrained flexibility

Chosen approach: **A** for faster validation in a club environment.

## 4) Architecture

1. **GitHub App**
- Captures PR, review, and state-change events

2. **Ingestion API (Spring Boot)**
- Verifies webhook signature
- Stores events idempotently using delivery ID
- Updates PR state snapshot

3. **SLA Engine**
- 5-minute scheduled scan
- Detects 12h/24h/36h thresholds
- Executes reminder/escalation/fallback actions

4. **Notification Service**
- Sends Discord webhook messages with template variants

5. **Dashboard**
- Shows team SLA status and blocked PRs

6. **Storage**
- PostgreSQL for workspaces/repos/PR/SLA logs
- Redis optional for scheduler lock/cache

7. **Observability**
- Prometheus metrics + Grafana dashboard

## 5) SLA Policy (Approved)

1. Start timer at `ready_for_review`
2. Pause when PR returns to `draft`
3. Stop when first meaningful review is submitted
4. Notification stages:
- 12h: reviewer reminder
- 24h: mentor/admin escalation (SLA breach)
- 36h: fallback reviewer assignment (optional toggle)

## 6) Data Model

### workspace
- Team-level configuration: SLA hours, channels, mentions

### repository_installation
- GitHub installation metadata per repository

### pull_request
- Repository, PR number, author, status, `ready_at`, `first_review_at`

### sla_state
- Current status and stage timestamps

### sla_event_log
- Immutable log of automated decisions and notifications

## 7) Error Handling and Reliability

1. Invalid webhook signature -> `401` and secure log
2. Duplicate delivery IDs -> ignore safely and return success
3. External API failures -> exponential backoff (max 3 retries)
4. Scheduler concurrency -> distributed lock
5. Failed notifications -> dead-letter record for operator retry

## 8) Validation Strategy

### Unit tests
- SLA timing calculations
- Draft/ready transitions
- Stage deduplication logic

### Integration tests
- Webhook ingest -> DB state update -> scanner -> outbound notification payload

### End-to-end scenario
- PR ready -> 12h reminder -> 24h escalation -> first review -> stop timer

### Basic load check
- ~1,000 open PR records scan performance and query cost

## 9) Rollout Plan (8 Weeks)

1. Weeks 1-2: GitHub App + webhook ingestion + PR state persistence
2. Weeks 3-4: SLA engine + 12h/24h/36h automation
3. Weeks 5-6: dashboard + admin settings
4. Week 7: observability + retry/dead-letter handling
5. Week 8: pilot with 1-2 club teams and KPI review

## 10) Go / No-Go Criteria

### Go
- First-review-within-24h rate improves by >= 20 percentage points in pilot

### No-Go / Pivot
- Alert fatigue remains high
- Admins report low perceived value and poor continued usage intent
