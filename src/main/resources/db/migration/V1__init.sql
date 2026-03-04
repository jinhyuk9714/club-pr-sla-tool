create table if not exists app_bootstrap_marker (
  id bigint primary key,
  created_at timestamptz not null default now()
);
