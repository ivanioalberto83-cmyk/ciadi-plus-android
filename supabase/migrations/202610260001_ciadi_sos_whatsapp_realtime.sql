-- CIADI+ SOS Realtime (homologação)
-- O esquema SOS e a tabela ciadi_sos_entregas já existem na migração
-- 20261023110000_ciadi_sos_levels_and_delivery.sql.
-- Esta migração é intencionalmente aditiva e não altera dados nem permissões.
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
