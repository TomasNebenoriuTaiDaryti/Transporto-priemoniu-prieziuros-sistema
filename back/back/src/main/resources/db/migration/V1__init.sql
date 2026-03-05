create table if not exists account (
    id bigserial primary key,
    email varchar(255) not null unique,
    password_hash varchar(255) not null,
    created_at timestamptz not null default now()
    );
create index if not exists idx_account_email on account(email);
