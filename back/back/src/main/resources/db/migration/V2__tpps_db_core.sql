create extension if not exists pgcrypto;

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

    constraint chk_group_member_role check (role in ('OWNER','ADMIN','MEMBER'))
    );

create table if not exists vehicle (
    id bigserial primary key,
    group_id bigint not null references app_group(id) on delete cascade,
    nickname varchar(80),
    vin varchar(17),
    make varchar(80),
    model varchar(120),
    model_year int,
    trim varchar(120),
    fuel_type varchar(30),
    engine varchar(120),
    transmission varchar(60),
    plate_number varchar(20),
    odometer_km bigint not null default 0,
    odometer_updated_at timestamptz,
    created_at timestamptz not null default now(),
    archived_at timestamptz,

    constraint chk_vin_len check (vin is null or length(vin) = 17),
    constraint chk_model_year check (model_year is null or (model_year between 1950 and 2100)),
    constraint chk_vehicle_fuel_type check (fuel_type is null or fuel_type in ('PETROL','DIESEL','LPG','CNG','HYBRID','PHEV','EV','OTHER'))
    );

create index if not exists idx_group_member_user_id on group_member(user_id);

create table if not exists user_settings (
    user_id bigint primary key references account(id) on delete cascade,
    language_code varchar(10) not null default 'lt',
    timezone varchar(64) not null default 'Europe/Vilnius',
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
    );

create table if not exists user_device (
    id bigserial primary key,
    user_id bigint not null references account(id) on delete cascade,
    platform varchar(20) not null default 'ANDROID',
    device_name varchar(120),
    push_token varchar(512),
    created_at timestamptz not null default now(),
    last_seen_at timestamptz,

    constraint chk_device_platform check (platform in ('ANDROID','IOS','WEB'))
    );

create index if not exists idx_vehicle_group_id on vehicle(group_id);
create index if not exists idx_user_device_user_id on user_device(user_id);

create unique index if not exists uq_vehicle_group_plate
    on vehicle(group_id, plate_number)
    where plate_number is not null and archived_at is null;

create index if not exists idx_vehicle_group_id on vehicle(group_id);
create unique index if not exists uq_vehicle_group_vin
    on vehicle(group_id, vin)
    where vin is not null and archived_at is null;

create table if not exists vin_profile (
    id bigserial primary key,
    vin varchar(17) not null unique,
    decoded_json jsonb not null,
    source varchar(40) not null default 'UNKNOWN',
    fetched_at timestamptz not null default now(),
    constraint chk_vin_profile_len check (length(vin)=17)
    );

create table if not exists odometer_record (
    id bigserial primary key,
    vehicle_id bigint not null references vehicle(id) on delete cascade,
    recorded_by_user_id bigint references account(id) on delete set null,
    odometer_km bigint not null,
    recorded_at timestamptz not null default now(),
    source varchar(20) not null default 'MANUAL',
    note varchar(400),

    constraint chk_odometer_source check (source in ('MANUAL','GPS','IMPORT'))
    );

create index if not exists idx_odometer_vehicle_id on odometer_record(vehicle_id);
create index if not exists idx_odometer_recorded_at on odometer_record(recorded_at);

create table if not exists gps_trip (
    id bigserial primary key,
    vehicle_id bigint not null references vehicle(id) on delete cascade,
    started_at timestamptz not null,
    ended_at timestamptz,
    distance_km numeric(10,2) not null default 0,
    start_lat numeric(9,6),
    start_lng numeric(9,6),
    end_lat numeric(9,6),
    end_lng numeric(9,6),
    created_by_user_id bigint references account(id) on delete set null,
    status varchar(20) not null default 'IN_PROGRESS',

    constraint chk_trip_status check (status in ('IN_PROGRESS','COMPLETED','CANCELLED'))
    );

create index if not exists idx_gps_trip_vehicle_id on gps_trip(vehicle_id);
create index if not exists idx_gps_trip_started_at on gps_trip(started_at);

create table if not exists expense_category (
    id bigserial primary key,
    code varchar(40) not null unique,
    name_lt varchar(120) not null,
    name_en varchar(120),
    created_at timestamptz not null default now()
    );

insert into expense_category(code, name_lt, name_en)
values
    ('FUEL','Kuras','Fuel'),
    ('SERVICE','Aptarnavimas','Service'),
    ('REPAIR','Remontas','Repair'),
    ('TIRES','Padangos','Tires'),
    ('INSURANCE','Draudimas','Insurance'),
    ('INSPECTION','Techninė apžiūra','Inspection'),
    ('TAX','Mokesčiai','Tax'),
    ('OTHER','Kita','Other')
    on conflict (code) do nothing;

create table if not exists fuel_entry (
    id bigserial primary key,
    vehicle_id bigint not null references vehicle(id) on delete cascade,
    created_by_user_id bigint references account(id) on delete set null,
    filled_at timestamptz not null default now(),
    odometer_km bigint,
    liters numeric(10,3) not null,
    price_per_liter numeric(10,4),
    total_cost numeric(12,2),
    currency char(3) not null default 'EUR',
    is_full_tank boolean not null default true,
    station varchar(120),
    note varchar(400)
    );

create index if not exists idx_fuel_vehicle_id on fuel_entry(vehicle_id);
create index if not exists idx_fuel_filled_at on fuel_entry(filled_at);

create table if not exists service_log (
    id bigserial primary key,
    vehicle_id bigint not null references vehicle(id) on delete cascade,
    created_by_user_id bigint references account(id) on delete set null,
    type varchar(20) not null,
    title varchar(160) not null,
    description text,
    performed_at timestamptz not null default now(),
    odometer_km bigint,
    labor_cost numeric(12,2) not null default 0,
    parts_cost numeric(12,2) not null default 0,
    total_cost numeric(12,2) not null default 0,
    currency char(3) not null default 'EUR',
    workshop varchar(160),
    invoice_no varchar(80),
    created_at timestamptz not null default now(),

    constraint chk_service_log_type check (type in ('MAINTENANCE','REPAIR'))
    );

create index if not exists idx_service_log_vehicle_id on service_log(vehicle_id);
create index if not exists idx_service_log_performed_at on service_log(performed_at);

create table if not exists maintenance_plan (
    id bigserial primary key,
    vehicle_id bigint not null references vehicle(id) on delete cascade,
    name varchar(160) not null,
    category_code varchar(40) references expense_category(code) on delete set null,
    interval_km bigint,
    interval_days int,
    last_done_at timestamptz,
    last_done_odometer_km bigint,
    next_due_at timestamptz,
    next_due_odometer_km bigint,
    is_active boolean not null default true,
    created_at timestamptz not null default now(),

    constraint chk_plan_interval check (interval_km is not null or interval_days is not null)
    );

create index if not exists idx_maint_plan_vehicle_id on maintenance_plan(vehicle_id);
create index if not exists idx_maint_plan_next_due_at on maintenance_plan(next_due_at);
create index if not exists idx_maint_plan_next_due_km on maintenance_plan(next_due_odometer_km);

create table if not exists reminder (
    id bigserial primary key,
    vehicle_id bigint not null references vehicle(id) on delete cascade,
    plan_id bigint references maintenance_plan(id) on delete set null,
    title varchar(160) not null,
    message varchar(600),
    due_at timestamptz,
    due_odometer_km bigint,
    notify_before_days int not null default 7,
    notify_before_km bigint not null default 300,
    status varchar(20) not null default 'SCHEDULED',
    created_at timestamptz not null default now(),
    triggered_at timestamptz,
    completed_at timestamptz,

    constraint chk_reminder_status check (status in ('SCHEDULED','TRIGGERED','COMPLETED','CANCELLED')),
    constraint chk_reminder_due check (due_at is not null or due_odometer_km is not null)
    );

create index if not exists idx_reminder_vehicle_id on reminder(vehicle_id);
create index if not exists idx_reminder_status on reminder(status);

create table if not exists document (
    id uuid primary key default gen_random_uuid(),
    vehicle_id bigint not null references vehicle(id) on delete cascade,
    uploaded_by_user_id bigint references account(id) on delete set null,
    type varchar(30) not null,
    title varchar(200) not null,
    description varchar(600),
    file_name varchar(255) not null,
    mime_type varchar(120) not null,
    file_size_bytes bigint not null,
    storage_provider varchar(30) not null default 'LOCAL',
    storage_path varchar(600) not null,
    issue_date date,
    expires_at date,
    created_at timestamptz not null default now(),

    constraint chk_document_type check (type in ('INSURANCE','INSPECTION','INVOICE','FINE','PHOTO','OTHER')),
    constraint chk_storage_provider check (storage_provider in ('LOCAL','S3','GCS'))
    );

create index if not exists idx_document_vehicle_id on document(vehicle_id);
create index if not exists idx_document_expires_at on document(expires_at);

create table if not exists service_log_document (
    service_log_id bigint not null references service_log(id) on delete cascade,
    document_id uuid not null references document(id) on delete cascade,
    primary key (service_log_id, document_id)
    );

create table if not exists vehicle_stats_daily (
    vehicle_id bigint not null references vehicle(id) on delete cascade,
    day date not null,
    distance_km numeric(10,2) not null default 0,
    fuel_cost numeric(12,2) not null default 0,
    service_cost numeric(12,2) not null default 0,
    repair_cost numeric(12,2) not null default 0,
    created_at timestamptz not null default now(),
    primary key(vehicle_id, day)
    );

create table if not exists ai_conversation (
    id bigserial primary key,
    group_id bigint not null references app_group(id) on delete cascade,
    vehicle_id bigint references vehicle(id) on delete set null,
    created_by_user_id bigint references account(id) on delete set null,
    title varchar(160),
    created_at timestamptz not null default now()
    );

create index if not exists idx_ai_conv_group_id on ai_conversation(group_id);
create index if not exists idx_ai_conv_vehicle_id on ai_conversation(vehicle_id);

create table if not exists ai_message (
    id bigserial primary key,
    conversation_id bigint not null references ai_conversation(id) on delete cascade,
    role varchar(10) not null,
    content text not null,
    model varchar(80),
    created_at timestamptz not null default now(),
    constraint chk_ai_role check (role in ('USER','ASSISTANT','SYSTEM'))
    );

create index if not exists idx_ai_message_conv_id on ai_message(conversation_id);
create index if not exists idx_ai_message_created_at on ai_message(created_at);