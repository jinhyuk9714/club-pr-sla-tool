alter table dead_letters
  add column if not exists repo_id bigint;

alter table dead_letters
  add column if not exists pr_number bigint;

alter table dead_letters
  add column if not exists stage varchar(64);

alter table dead_letters
  add column if not exists replay_status varchar(16);

update dead_letters
set replay_status = 'PENDING'
where replay_status is null;

alter table dead_letters
  alter column replay_status set not null;

alter table dead_letters
  alter column replay_status set default 'PENDING';

alter table dead_letters
  add column if not exists replay_attempts integer not null default 0;

alter table dead_letters
  add column if not exists last_error text;

alter table dead_letters
  add column if not exists replayed_at timestamptz;

alter table dead_letters
  add column if not exists updated_at timestamptz;

update dead_letters
set updated_at = created_at
where updated_at is null;

alter table dead_letters
  alter column updated_at set not null;

alter table dead_letters
  alter column updated_at set default now();

create index if not exists idx_dead_letters_status_created_at
  on dead_letters (replay_status, created_at desc);
