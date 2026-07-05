alter table slots add column if not exists studio_cancellation_reason text;
alter table chefs add column if not exists avg_rating numeric(3,2);
create index if not exists idx_ratings_chef on ratings(chef_id);
