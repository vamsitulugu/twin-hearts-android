-- Run once in the Supabase SQL editor. (README says the old server already has an fcm_tokens table:
-- if so, just make sure it has these columns.)
create table if not exists fcm_tokens (
  token      text primary key,
  user_id    text not null,
  couple_id  text not null,
  role       text not null,            -- 'user1' | 'user2'
  platform   text not null default 'android',
  updated_at timestamptz not null default now()
);
create index if not exists fcm_tokens_couple_role on fcm_tokens (couple_id, role);
