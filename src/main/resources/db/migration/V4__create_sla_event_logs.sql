create table if not exists sla_event_logs (
  id bigserial primary key,
  repo_id bigint not null,
  pr_number bigint not null,
  stage varchar(64) not null,
  created_at timestamptz not null
);

create unique index if not exists uk_sla_event_logs_repo_pr_stage
  on sla_event_logs (repo_id, pr_number, stage);
