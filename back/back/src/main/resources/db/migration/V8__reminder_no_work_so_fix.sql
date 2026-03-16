alter table reminder
    add column if not exists source_type varchar(30),
    add column if not exists source_id varchar(80);

update reminder
set source_type = coalesce(source_type, 'LEGACY'),
    source_id   = coalesce(source_id, id::text)
where source_type is null or source_id is null;

alter table reminder
    alter column source_type set not null,
alter column source_id set not null;

do $$
declare
idx_is_partial boolean;
begin
  if exists (
    select 1
    from pg_constraint
    where conname = 'uq_reminder_source'
  ) then
    return;
end if;
  if to_regclass('public.uq_reminder_source') is not null then
select (i.indpred is not null)
into idx_is_partial
from pg_index i
         join pg_class c on c.oid = i.indexrelid
where c.relname = 'uq_reminder_source';

if idx_is_partial then
execute 'drop index if exists uq_reminder_source';
execute 'alter table reminder add constraint uq_reminder_source unique (vehicle_id, source_type, source_id)';
return;
else
      execute 'alter table reminder add constraint uq_reminder_source unique using index uq_reminder_source';
      return;
end if;
end if;
execute 'alter table reminder add constraint uq_reminder_source unique (vehicle_id, source_type, source_id)';
end $$;

create index if not exists idx_reminder_vehicle_due_at on reminder(vehicle_id, due_at);
create index if not exists idx_reminder_vehicle_due_km on reminder(vehicle_id, due_odometer_km);