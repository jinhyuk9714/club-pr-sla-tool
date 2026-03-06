alter table dead_letters
  add column if not exists job_type varchar(64);
