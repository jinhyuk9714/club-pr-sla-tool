create table if not exists dead_letters (
  id bigserial primary key,
  reason varchar(100) not null,
  payload text not null,
  created_at timestamptz not null
);
