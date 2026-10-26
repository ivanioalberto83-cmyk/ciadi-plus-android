# CIADI+ SOS -> Dashboard (homologação sem custos adicionais)

## Objetivo
- Preservar `main` e os dados existentes.
- Corrigir o retorno da RPC SOS na app: usar o ID real e o protocolo devolvidos pelo Supabase, sem inventar um protocolo local.
- Preparar a subscrição Realtime do Dashboard à tabela existente `public.ciadi_sos_alertas`.
- Manter o envio externo por WhatsApp desativado nesta fase; não chamar automaticamente `ciadi-sos-dispatch` a partir do Android.

## O que esta branch contém
- Correção Android em `SupabaseModulesRepositoryImpl.kt`: valida a sessão, lê o JSON devolvido pela RPC `ciadi_criar_alerta_sos`, confirma `success` e apresenta `alerta_id`, `protocolo` e `mensagem` reais.
- Migração aditiva para incluir `ciadi_sos_alertas` na publicação `supabase_realtime`. O esquema de alertas, níveis e entregas já existe na migração `20261023110000_ciadi_sos_levels_and_delivery.sql`; não criar tabelas paralelas.
- Rascunho de template WhatsApp apenas para eventual ativação futura.

## Custos e segurança
- Não criar novo projeto Supabase nem ramo de base de dados pago.
- Não aplicar a migração à produção nesta fase.
- Não configurar tokens, não enviar mensagens reais e não chamar o dispatcher automaticamente.
- Nunca guardar tokens de serviço no Android ou no repositório.
- A localização só deve ser guardada/consultada de acordo com a autorização e as políticas RLS existentes.

## Sequência de homologação
1. Rever o diff da branch e confirmar que a migração só adiciona a tabela de alertas à publicação Realtime.
2. Compilar e executar testes Android; não considerar o APK atualizado antes de confirmar o artefacto.
3. Preparar no Dashboard a lista de alertas com nível, categoria, estado, hora e localização apenas para perfis autorizados.
4. Testar: sessão válida/expirada, utilizador autorizado/não autorizado, localização recusada, falha de rede, reconexão e duplicação.
5. Aplicar a migração somente depois de backup verificável e aprovação da janela de mudança.

## Estado atual
- A correção do código Android e a migração estão na branch de homologação.
- A migração não foi aplicada à base de dados.
- WhatsApp continua desligado e não foi enviada nenhuma mensagem.
- A ligação visual do Dashboard e o APK de homologação ainda exigem implementação/compilação e validação.
