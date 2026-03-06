create table if not exists admin_audit_logs (
  id bigserial primary key,
  operation varchar(64) not null,
  request_path varchar(255) not null,
  http_method varchar(8) not null,
  http_status integer not null,
  outcome varchar(32) not null,
  repository_id bigint null,
  pr_number bigint null,
  dead_letter_id bigint null,
  error_message text null,
  created_at timestamptz not null default now()
);

create index if not exists idx_admin_audit_logs_created_at
  on admin_audit_logs (created_at desc);

create index if not exists idx_admin_audit_logs_operation_created_at
  on admin_audit_logs (operation, created_at desc);
