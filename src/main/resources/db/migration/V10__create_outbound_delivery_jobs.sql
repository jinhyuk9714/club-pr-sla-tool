create table if not exists outbound_delivery_jobs (
  id bigserial primary key,
  job_type varchar(64) not null,
  status varchar(32) not null,
  unique_key varchar(255) not null,
  installation_id bigint,
  repository_id bigint,
  pr_number bigint,
  payload_json text not null,
  attempt_count integer not null default 0,
  next_attempt_at timestamptz not null,
  last_error text,
  created_at timestamptz not null,
  updated_at timestamptz not null
);

create index if not exists idx_outbound_delivery_jobs_status_next_attempt_at
  on outbound_delivery_jobs (status, next_attempt_at, created_at);

create unique index if not exists uk_outbound_delivery_jobs_active_unique_key
  on outbound_delivery_jobs (unique_key)
  where status in ('PENDING', 'PROCESSING');
