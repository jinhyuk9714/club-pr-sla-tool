create table if not exists delivery_events (
  id bigserial primary key,
  delivery_id varchar(255) not null,
  event_type varchar(100) not null,
  payload text not null,
  received_at timestamptz not null
);

create unique index if not exists uk_delivery_events_delivery_id
  on delivery_events (delivery_id);
