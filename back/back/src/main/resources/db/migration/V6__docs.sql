alter table document
    add column if not exists meta jsonb not null default '{}'::jsonb;

alter table service_log
    add column if not exists kind varchar(30) not null default 'OTHER',
    add column if not exists meta jsonb not null default '{}'::jsonb;

create index if not exists idx_document_vehicle_created_by on document(vehicle_id, uploaded_by_user_id);
create index if not exists idx_service_log_vehicle_created_by on service_log(vehicle_id, created_by_user_id);
create index if not exists idx_service_log_kind on service_log(kind);