alter table reminder
    add column if not exists source_type varchar(30),
    add column if not exists source_id varchar(80);

create unique index if not exists uq_reminder_source
    on reminder(vehicle_id, source_type, source_id)
    where source_type is not null and source_id is not null;

create index if not exists idx_reminder_vehicle_due_at on reminder(vehicle_id, due_at);
create index if not exists idx_reminder_vehicle_due_km on reminder(vehicle_id, due_odometer_km);