alter table pull_request_states
  add column if not exists head_sha varchar(255);
