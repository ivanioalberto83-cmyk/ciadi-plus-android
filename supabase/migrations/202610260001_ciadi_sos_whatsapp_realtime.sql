-- CIADI+ SOS WhatsApp + Dashboard realtime (homologacao only)
-- Additive migration: preserves existing alert rows and table structure.
alter table public.ciadi_sos_alertas
  add column if not exists categoria text not null default 'geral',
  add column if not exists nivel text not null default 'alto';

alter table public.ciadi_sos_alertas
  drop constraint if exists ciadi_sos_alertas_categoria_check;
alter table public.ciadi_sos_alertas
  add constraint ciadi_sos_alertas_categoria_check
  check (categoria in ('seguranca','saude','comportamento','bullying','desaparecimento','geral'));

alter table public.ciadi_sos_alertas
  drop constraint if exists ciadi_sos_alertas_nivel_check;
alter table public.ciadi_sos_alertas
  add constraint ciadi_sos_alertas_nivel_check
  check (nivel in ('critico','alto','moderado','baixo'));

create table if not exists public.ciadi_sos_whatsapp_entregas (
  id uuid primary key default gen_random_uuid(),
  alerta_id uuid not null unique references public.ciadi_sos_alertas(id) on delete cascade,
  destinatario text not null,
  estado text not null default 'pending' check (estado in ('pending','sent','failed')),
  provider_message_id text,
  erro_codigo text,
  tentativas integer not null default 0 check (tentativas >= 0),
  criado_em timestamptz not null default now(),
  atualizado_em timestamptz not null default now(),
  enviado_em timestamptz
);

alter table public.ciadi_sos_whatsapp_entregas enable row level security;
revoke all on public.ciadi_sos_whatsapp_entregas from anon, authenticated;
grant all on public.ciadi_sos_whatsapp_entregas to service_role;

-- Dashboard clients using Supabase Realtime can receive alert changes.
do $$
begin
  if not exists (
    select 1
    from pg_publication_tables
    where pubname = 'supabase_realtime'
      and schemaname = 'public'
      and tablename = 'ciadi_sos_alertas'
  ) then
    alter publication supabase_realtime add table public.ciadi_sos_alertas;
  end if;
end $$;
