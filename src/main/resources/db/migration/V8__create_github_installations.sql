create table if not exists github_installations (
  installation_id bigint primary key,
  account_id bigint not null,
  account_login varchar(255) not null,
  account_type varchar(32) not null,
  status varchar(32) not null,
  installed_at timestamptz not null,
  updated_at timestamptz not null
);

create table if not exists github_installation_repository_entries (
  id bigserial primary key,
  installation_id bigint not null references github_installations (installation_id),
  repository_id bigint not null,
  repository_name varchar(255) not null,
  repository_full_name varchar(255) not null,
  active boolean not null default true,
  updated_at timestamptz not null
);

create unique index if not exists uk_github_installation_repository_entries_installation_repository
  on github_installation_repository_entries (installation_id, repository_id);

create unique index if not exists uk_github_installation_repository_entries_repository
  on github_installation_repository_entries (repository_id);

create table if not exists installation_settings (
  installation_id bigint primary key references github_installations (installation_id),
  encrypted_discord_webhook text null,
  reminder_hours integer not null,
  escalation_hours integer not null,
  fallback_enabled boolean not null default false,
  configured boolean not null default false,
  updated_at timestamptz not null
);
