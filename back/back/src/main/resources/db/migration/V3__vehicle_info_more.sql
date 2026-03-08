alter table vehicle
    add column if not exists engine_displacement_cc int,
    add column if not exists drive varchar(60),
    add column if not exists body varchar(120),
    add column if not exists doors int,
    add column if not exists seats int,
    add column if not exists co2_g_km numeric(10,2),
    add column if not exists plant_country varchar(80),
    add column if not exists manufacturer varchar(200);

create index if not exists idx_vehicle_make_model on vehicle(make, model);