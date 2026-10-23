-- CIADI+ SOS v2: severidade, categorias, outbox e validação de autorização.
-- Esta migration é para revisão/aplicação controlada; não altera produção até ser aplicada.
begin;

alter table public.ciadi_sos_alertas
  add column if not exists categoria text not null default 'OUTRO',
  add column if not exists nivel_emergencia text not null default 'URGENTE',
  add column if not exists protocolo text,
  add column if not exists canal_estado jsonb not null default '{}'::jsonb;

alter table public.ciadi_sos_alertas
  drop constraint if exists ciadi_sos_alertas_categoria_check;
alter table public.ciadi_sos_alertas
  add constraint ciadi_sos_alertas_categoria_check
  check (categoria in ('RISCO_IMEDIATO','DESAPARECIMENTO','ACIDENTE','CRISE_COMPORTAMENTAL','FUGA','BULLYING','PROTECAO','OUTRO'));

alter table public.ciadi_sos_alertas
  drop constraint if exists ciadi_sos_alertas_nivel_check;
alter table public.ciadi_sos_alertas
  add constraint ciadi_sos_alertas_nivel_check
  check (nivel_emergencia in ('CRITICO','URGENTE','ATENCAO','INFORMATIVO'));

alter table public.ciadi_sos_configuracao
  add column if not exists email_emergencia text,
  add column if not exists whatsapp_emergencia text;

create table if not exists public.ciadi_sos_entregas (
  id uuid primary key default gen_random_uuid(),
  alerta_id uuid not null references public.ciadi_sos_alertas(id) on delete cascade,
  canal text not null check (canal in ('whatsapp','email')),
  destinatario text not null,
  estado text not null default 'pendente'
    check (estado in ('pendente','a_enviar','aceite_provedor','entregue','lido','falhou','nao_configurado')),
  tentativas integer not null default 0,
  id_provedor text,
  erro text,
  criado_em timestamptz not null default now(),
  atualizado_em timestamptz not null default now(),
  enviado_em timestamptz,
  unique(alerta_id, canal, destinatario)
);

alter table public.ciadi_sos_entregas enable row level security;
drop policy if exists ciadi_sos_entregas_gestao_read on public.ciadi_sos_entregas;
create policy ciadi_sos_entregas_gestao_read
  on public.ciadi_sos_entregas for select to authenticated
  using (public.ciadi_is_management());

revoke all on public.ciadi_sos_entregas from anon, authenticated;
grant select on public.ciadi_sos_entregas to authenticated;
grant all on public.ciadi_sos_entregas to service_role;

create index if not exists idx_ciadi_sos_entregas_alerta_estado
  on public.ciadi_sos_entregas(alerta_id, estado);

-- Destinos institucionais fornecidos pela direção.
do $$
begin
  if exists (select 1 from public.ciadi_sos_configuracao) then
    update public.ciadi_sos_configuracao
       set telefone_emergencia = '+244949090999',
           whatsapp_emergencia = '+244949090999',
           email_emergencia = 'geral@ciadi.ao',
           nome_emergencia = coalesce(nullif(nome_emergencia,''), 'CIADI — Central de Alertas'),
           updated_at = now()
     where id = (select id from public.ciadi_sos_configuracao order by updated_at desc limit 1);
  else
    insert into public.ciadi_sos_configuracao
      (ativo, telefone_emergencia, whatsapp_emergencia, email_emergencia, nome_emergencia,
       mensagem_padrao, notificar_gestao, notificar_profissionais)
    values
      (true, '+244949090999', '+244949090999', 'geral@ciadi.ao',
       'CIADI — Central de Alertas', 'Alerta SOS CIADI. Necessária atenção imediata.', true, true);
  end if;
end $$;

create or replace function public.ciadi_criar_alerta_sos_core(
  p_paciente_id uuid,
  p_mensagem text,
  p_latitude numeric,
  p_longitude numeric,
  p_localizacao_autorizada boolean,
  p_tipo text,
  p_categoria text,
  p_nivel_emergencia text
) returns jsonb
language plpgsql
security definer
set search_path = public, pg_temp
as $function$
declare
  v_perfil uuid := auth.uid();
  v_id uuid;
  v_protocolo text;
  v_mensagem text;
  v_cfg public.ciadi_sos_configuracao%rowtype;
  v_recipient uuid;
  v_location_ok boolean;
begin
  if v_perfil is null then
    raise exception 'Sessão não autenticada';
  end if;

  if not exists (select 1 from public.perfis where id = v_perfil and ativo = true) then
    raise exception 'Perfil inativo ou inexistente';
  end if;

  if p_paciente_id is null then
    raise exception 'É obrigatório selecionar um assistido autorizado';
  end if;

  if upper(coalesce(p_tipo,'')) not in ('SOS','BULLYING') then
    raise exception 'Tipo de alerta inválido';
  end if;
  if upper(coalesce(p_categoria,'')) not in
    ('RISCO_IMEDIATO','DESAPARECIMENTO','ACIDENTE','CRISE_COMPORTAMENTAL','FUGA','BULLYING','PROTECAO','OUTRO') then
    raise exception 'Categoria de alerta inválida';
  end if;
  if upper(coalesce(p_nivel_emergencia,'')) not in ('CRITICO','URGENTE','ATENCAO','INFORMATIVO') then
    raise exception 'Nível de emergência inválido';
  end if;
  if p_latitude is not null and (p_latitude < -90 or p_latitude > 90) then
    raise exception 'Latitude inválida';
  end if;
  if p_longitude is not null and (p_longitude < -180 or p_longitude > 180) then
    raise exception 'Longitude inválida';
  end if;

  -- A autorização é verificada no servidor; não se confia apenas no seletor do Android.
  if not exists (
    select 1 from public.pacientes p
    where p.id = p_paciente_id and coalesce(p.ativo,true) = true
      and (
        p.perfil_id = v_perfil
        or exists (
          select 1 from public.ciadi_vinculos_familiares vf
          where vf.perfil_id = v_perfil and vf.paciente_id = p.id and vf.ativo = true
            and (vf.validade_ate is null or vf.validade_ate >= current_date)
        )
        or exists (
          select 1 from public.perfis pf
          where pf.id = v_perfil and pf.ativo = true and pf.tipo in ('admin','gestor','operador')
        )
        or exists (
          select 1
          from public.profissionais pr
          join public.perfis pf on pf.id = pr.perfil_id
          join public.agendamentos ag on ag.profissional_id = pr.id
          where pf.id = v_perfil and pf.ativo = true and pr.ativo = true
            and ag.paciente_id = p.id and ag.data_hora >= now() - interval '30 days'
        )
        or exists (
          select 1
          from public.ciadi_atribuicoes_at atx
          join public.profissionais pr on pr.id = atx.profissional_id
          join public.perfis pf on pf.id = pr.perfil_id
          where pf.id = v_perfil and pf.ativo = true and pr.ativo = true
            and atx.paciente_id = p.id and atx.ativo = true
        )
      )
  ) then
    raise exception 'O utilizador não tem autorização para este assistido';
  end if;

  select * into v_cfg
    from public.ciadi_sos_configuracao
   where ativo = true
   order by updated_at desc
   limit 1;

  v_mensagem := coalesce(nullif(trim(p_mensagem),''), coalesce(v_cfg.mensagem_padrao,'Alerta SOS CIADI.'));
  v_location_ok := coalesce(p_localizacao_autorizada,false)
                   and p_latitude is not null and p_longitude is not null;
  v_protocolo := 'SOS-' || upper(substr(replace(gen_random_uuid()::text,'-',''),1,10));

  insert into public.ciadi_sos_alertas
    (perfil_id,paciente_id,tipo,mensagem,latitude,longitude,localizacao_autorizada,
     estado,categoria,nivel_emergencia,protocolo,canal_estado)
  values
    (v_perfil,p_paciente_id,case when upper(p_tipo)='SOS' then 'sos' else 'emergencia' end,v_mensagem,
     case when v_location_ok then p_latitude else null end,
     case when v_location_ok then p_longitude else null end,
     v_location_ok,'aberto',upper(p_categoria),upper(p_nivel_emergencia),v_protocolo,
     jsonb_build_object('whatsapp','pendente','email','pendente'))
  returning id into v_id;

  if coalesce(v_cfg.notificar_gestao,true) then
    for v_recipient in
      select id from public.perfis where ativo=true and tipo in ('admin','gestor')
    loop
      insert into public.notificacoes(perfil_id,titulo,mensagem,tipo,lida,link)
      values(v_recipient,
        case when upper(p_nivel_emergencia)='CRITICO' then '🚨 SOS CRÍTICO CIADI' else '🚨 Alerta CIADI' end,
        v_protocolo || ' · ' || upper(p_categoria) || ' · ' || v_mensagem,
        'sos',false,'ciadi://sos/'||v_id::text);
    end loop;
  end if;

  if coalesce(v_cfg.notificar_profissionais,true) then
    for v_recipient in
      select distinct pp.id
      from (
        select a.profissional_id from public.agendamentos a
         where a.paciente_id=p_paciente_id and a.profissional_id is not null
           and a.data_hora >= now() - interval '30 days'
        union
        select atx.profissional_id from public.ciadi_atribuicoes_at atx
         where atx.paciente_id=p_paciente_id and atx.ativo=true
      ) x
      join public.profissionais pr on pr.id=x.profissional_id and pr.ativo=true
      join public.perfis pp on pp.id=pr.perfil_id and pp.ativo=true
    loop
      insert into public.notificacoes(perfil_id,titulo,mensagem,tipo,lida,link)
      values(v_recipient,'🚨 Alerta CIADI',
        v_protocolo || ' · ' || upper(p_categoria) || ' · ' || v_mensagem,
        'sos',false,'ciadi://sos/'||v_id::text);
    end loop;
  end if;

  if nullif(coalesce(v_cfg.whatsapp_emergencia,v_cfg.telefone_emergencia),'') is not null then
    insert into public.ciadi_sos_entregas(alerta_id,canal,destinatario)
    values(v_id,'whatsapp',coalesce(v_cfg.whatsapp_emergencia,v_cfg.telefone_emergencia))
    on conflict (alerta_id,canal,destinatario) do nothing;
  end if;
  if nullif(v_cfg.email_emergencia,'') is not null then
    insert into public.ciadi_sos_entregas(alerta_id,canal,destinatario)
    values(v_id,'email',v_cfg.email_emergencia)
    on conflict (alerta_id,canal,destinatario) do nothing;
  end if;

  return jsonb_build_object(
    'success',true,'alerta_id',v_id,'protocolo',v_protocolo,
    'mensagem','Alerta registado. Estado dos canais externos será confirmado separadamente.',
    'localizacao_enviada',v_location_ok,
    'whatsapp_status',case when nullif(coalesce(v_cfg.whatsapp_emergencia,v_cfg.telefone_emergencia),'') is null then 'nao_configurado' else 'pendente' end,
    'email_status',case when nullif(v_cfg.email_emergencia,'') is null then 'nao_configurado' else 'pendente' end
  );
end;
$function$;

revoke all on function public.ciadi_criar_alerta_sos_core(uuid,text,numeric,numeric,boolean,text,text,text) from public, anon, authenticated;

-- Mantém compatibilidade com versões antigas e acrescenta a nova assinatura para a app atual.
create or replace function public.ciadi_criar_alerta_sos(
  p_paciente_id uuid default null,
  p_mensagem text default null,
  p_latitude numeric default null,
  p_longitude numeric default null,
  p_localizacao_autorizada boolean default false
) returns jsonb
language sql
security definer
set search_path = public, pg_temp
as $function$
  select public.ciadi_criar_alerta_sos_core(
    p_paciente_id,p_mensagem,p_latitude,p_longitude,p_localizacao_autorizada,
    'SOS','OUTRO','URGENTE'
  );
$function$;

create or replace function public.ciadi_criar_alerta_sos(
  p_paciente_id uuid,
  p_mensagem text,
  p_latitude numeric,
  p_longitude numeric,
  p_localizacao_autorizada boolean,
  p_tipo text,
  p_categoria text,
  p_nivel_emergencia text
) returns jsonb
language sql
security definer
set search_path = public, pg_temp
as $function$
  select public.ciadi_criar_alerta_sos_core(
    p_paciente_id,p_mensagem,p_latitude,p_longitude,p_localizacao_autorizada,
    p_tipo,p_categoria,p_nivel_emergencia
  );
$function$;

revoke all on function public.ciadi_criar_alerta_sos_core(uuid,text,numeric,numeric,boolean,text,text,text) from public, anon, authenticated;
grant execute on function public.ciadi_criar_alerta_sos(uuid,text,numeric,numeric,boolean) to authenticated;
grant execute on function public.ciadi_criar_alerta_sos(uuid,text,numeric,numeric,boolean,text,text,text) to authenticated;

commit;
