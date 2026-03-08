create table if not exists active_vehicle (
    user_id bigint primary key references account(id) on delete cascade,
    vehicle_id bigint not null unique references vehicle(id) on delete cascade,
    activated_at timestamptz not null default now()
    );

create index if not exists idx_active_vehicle_vehicle_id on active_vehicle(vehicle_id);