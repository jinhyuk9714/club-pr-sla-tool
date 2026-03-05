create table if not exists pull_request_states (
  id bigserial primary key,
  repository_id bigint not null,
  pr_number bigint not null,
  author_login varchar(255),
  first_reviewer_login varchar(255),
  status varchar(32) not null,
  ready_at timestamptz,
  first_review_at timestamptz
);

create unique index if not exists uk_pull_request_states_repository_pr
  on pull_request_states (repository_id, pr_number);
