alter table vehicle
drop constraint if exists fk_vehicle_group;

alter table vehicle
    alter column group_id drop not null;

alter table vehicle
    add constraint fk_vehicle_group
        foreign key (group_id) references app_group(id) on delete cascade;

alter table vehicle
    add column if not exists owner_user_id bigint;

update vehicle v
set owner_user_id = g.owner_user_id
    from app_group g
where v.group_id = g.id
  and v.owner_user_id is null;

update vehicle
set owner_user_id = (select id from account order by id asc limit 1)
where owner_user_id is null;

alter table vehicle
    alter column owner_user_id set not null;

alter table vehicle
    add constraint fk_vehicle_owner
        foreign key (owner_user_id) references account(id) on delete cascade;

create index if not exists idx_vehicle_owner_user_id on vehicle(owner_user_id);

drop index if exists uq_vehicle_group_vin;
drop index if exists uq_vehicle_group_plate;

create unique index if not exists uq_vehicle_group_vin
    on vehicle(group_id, vin)
    where group_id is not null and vin is not null and archived_at is null;

create unique index if not exists uq_vehicle_owner_vin
    on vehicle(owner_user_id, vin)
    where group_id is null and vin is not null and archived_at is null;

create unique index if not exists uq_vehicle_group_plate
    on vehicle(group_id, plate_number)
    where group_id is not null and plate_number is not null and archived_at is null;

create unique index if not exists uq_vehicle_owner_plate
    on vehicle(owner_user_id, plate_number)
    where group_id is null and plate_number is not null and archived_at is null;