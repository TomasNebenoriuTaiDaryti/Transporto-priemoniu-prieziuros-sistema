create extension if not exists pgcrypto;

create table if not exists account (
    id bigserial primary key,
    email varchar(255) not null unique,
    password_hash varchar(255) not null,
    created_at timestamptz not null default now()
    );

create index if not exists idx_account_email
    on account(email);

create table if not exists app_group (
    id bigserial primary key,
    name varchar(120) not null,
    owner_user_id bigint not null references account(id) on delete cascade,
    created_at timestamptz not null default now()
    );

create table if not exists group_member (
    group_id bigint not null references app_group(id) on delete cascade,
    user_id bigint not null references account(id) on delete cascade,
    role varchar(20) not null default 'MEMBER',
    created_at timestamptz not null default now(),
    primary key (group_id, user_id),
    constraint chk_group_member_role
    check (role in ('OWNER', 'MEMBER'))
    );

create index if not exists idx_group_member_user_id
    on group_member(user_id);


create table if not exists vehicle (
    id bigserial primary key,
    owner_user_id bigint not null references account(id) on delete cascade,
    group_id bigint references app_group(id) on delete cascade,
    vin varchar(17),
    make varchar(80),
    model varchar(120),
    model_year int,
    fuel_type varchar(30),
    transmission varchar(60),
    odometer_km bigint not null default 0,
    created_at timestamptz not null default now(),
    archived_at timestamptz,
    engine_displacement_cc int,
    drive varchar(60),
    body varchar(120),
    doors int,
    seats int,
    co2_g_km numeric(10,2),
    plant_country varchar(80),
    manufacturer varchar(200),
    constraint chk_vin_len
    check (vin is null or length(vin) = 17),
    constraint chk_model_year
    check (model_year is null or model_year between 1950 and 2100),
    constraint chk_vehicle_fuel_type
    check (
        fuel_type is null or fuel_type in ('PETROL', 'DIESEL', 'LPG', 'CNG', 'HYBRID', 'PHEV', 'EV', 'OTHER')
    ),
    constraint chk_engine_displacement
    check (
        engine_displacement_cc is null or
        (engine_displacement_cc > 300 and engine_displacement_cc <= 10000)
    ),
    constraint chk_vehicle_doors
    check (doors is null or doors between 1 and 8),
    constraint chk_vehicle_seats
    check (seats is null or seats between 1 and 20)
    );

create index if not exists idx_vehicle_owner_user_id
    on vehicle(owner_user_id);

create index if not exists idx_vehicle_group_id
    on vehicle(group_id);

create index if not exists idx_vehicle_make_model
    on vehicle(make, model);

create unique index if not exists uq_vehicle_owner_vin
    on vehicle(owner_user_id, vin)
    where group_id is null
    and vin is not null
    and archived_at is null;

create unique index if not exists uq_vehicle_group_vin
    on vehicle(group_id, vin)
    where group_id is not null
    and vin is not null
    and archived_at is null;

create table if not exists active_vehicle (
    user_id bigint primary key references account(id) on delete cascade,
    vehicle_id bigint not null unique references vehicle(id) on delete cascade,
    activated_at timestamptz not null default now()
    );

create index if not exists idx_active_vehicle_vehicle_id
    on active_vehicle(vehicle_id);

create table if not exists document (
    id uuid primary key default gen_random_uuid(),
    vehicle_id bigint not null references vehicle(id) on delete cascade,
    uploaded_by_user_id bigint references account(id) on delete set null,
    type varchar(30) not null,
    title varchar(200) not null,
    description varchar(600),
    issue_date date,
    expires_at date,
    meta jsonb not null default '{}'::jsonb,
    created_at timestamptz not null default now(),
    constraint chk_document_type
    check (type in ('INVOICE', 'INSURANCE', 'INSPECTION', 'FINE', 'OTHER'))
    );

create index if not exists idx_document_vehicle_id
    on document(vehicle_id);

create index if not exists idx_document_vehicle_uploaded_by
    on document(vehicle_id, uploaded_by_user_id);

create index if not exists idx_document_expires_at
    on document(expires_at);

create table if not exists service_log (
    id bigserial primary key,
    vehicle_id bigint not null references vehicle(id) on delete cascade,
    created_by_user_id bigint references account(id) on delete set null,
    kind varchar(30) not null,
    title varchar(160) not null,
    description text,
    performed_at timestamptz not null default now(),
    odometer_km bigint,
    total_cost numeric(12,2) not null default 0,
    currency char(3) not null default 'EUR',
    meta jsonb not null default '{}'::jsonb,
    created_at timestamptz not null default now(),
    constraint chk_service_log_kind
    check (kind in ('OIL', 'TIRES', 'BRAKES', 'SERVICE', 'FUEL', 'OTHER'))
    );

create index if not exists idx_service_log_vehicle_id
    on service_log(vehicle_id);

create index if not exists idx_service_log_vehicle_created_by
    on service_log(vehicle_id, created_by_user_id);

create index if not exists idx_service_log_performed_at
    on service_log(performed_at);

create index if not exists idx_service_log_kind
    on service_log(kind);

create table if not exists reminder (
    id bigserial primary key,
    vehicle_id bigint not null references vehicle(id) on delete cascade,
    title varchar(160) not null,
    message varchar(600),
    due_at timestamptz,
    due_odometer_km bigint,
    notify_before_days int not null default 7,
    notify_before_km bigint not null default 300,
    status varchar(20) not null default 'SCHEDULED',
    created_at timestamptz not null default now(),
    source_type varchar(30) not null,
    source_id varchar(80) not null,
    constraint chk_reminder_status
    check (status in ('SCHEDULED', 'TRIGGERED')),
    constraint chk_reminder_due
    check (due_at is not null or due_odometer_km is not null),
    constraint chk_reminder_source_type
    check (
        source_type in (
        'DOCUMENT',
        'RECORD_OIL_KM',
        'RECORD_OIL_DATE',
        'RECORD_BRAKES_KM',
        'RECORD_TIRES_DATE'
                        )
    ),
    constraint uq_reminder_source
    unique (vehicle_id, source_type, source_id)
    );

create index if not exists idx_reminder_vehicle_id
    on reminder(vehicle_id);

create index if not exists idx_reminder_status
    on reminder(status);

create index if not exists idx_reminder_vehicle_due_at
    on reminder(vehicle_id, due_at);

create index if not exists idx_reminder_vehicle_due_km
    on reminder(vehicle_id, due_odometer_km);