# CIADI+ SOS -> Dashboard -> WhatsApp (homologação)

## Objetivo
- Preservar `main` e os dados existentes.
- Transmitir alertas SOS do CIADI+ para `public.ciadi_sos_alertas`, visíveis no Dashboard por Supabase Realtime.
- Enviar aviso ao WhatsApp oficial CIADI configurado como `949090999`, usando a WhatsApp Business Cloud API oficial.
- Não enviar nomes de crianças, conteúdo clínico ou coordenadas exatas no WhatsApp. A localização autorizada permanece consultável no Dashboard por utilizadores autorizados.

## O que esta branch contém
- Migração aditiva para categoria/nível, registo idempotente de tentativas WhatsApp e publicação Realtime da tabela SOS.
- Edge Function `ciadi-sos-whatsapp`, que exige JWT válido, valida o perfil e a propriedade/autorização do alerta, envia um template aprovado e regista o ID de mensagem/estado.
- Esta função ainda precisa ser chamada pelo fluxo Android após a criação do alerta. A aplicação Android está empacotada em `ciadi+ (1).zip`; não alterar o ZIP nem declarar APK atualizado até o código fonte ser extraído, alterado e compilado com testes bloqueantes.

## Segredos necessários (Supabase Edge Function secrets)
- `CIADI_SOS_WHATSAPP_TO=244949090999` (destinatário normalizado em formato internacional)
- `WHATSAPP_PHONE_NUMBER_ID` (ID do número remetente no Meta, não o número telefónico)
- `WHATSAPP_ACCESS_TOKEN` (token de sistema; nunca guardar no Android nem no GitHub)
- `WHATSAPP_GRAPH_API_VERSION` (versão Graph API aprovada/ativa)
- `WHATSAPP_SOS_TEMPLATE_NAME` (nome de template aprovado no WhatsApp Manager)
- `WHATSAPP_SOS_TEMPLATE_LANGUAGE=pt_PT` (ajustar ao idioma aprovado)
- Supabase standard secrets: `SUPABASE_URL`, `SUPABASE_ANON_KEY`, `SUPABASE_SERVICE_ROLE_KEY`.

O template deve ter exatamente cinco variáveis de corpo nesta ordem: ID do alerta, nível, categoria, data/hora e indicação de localização. Confirmar o nome, idioma e estrutura do template antes de ativar.

## Sequência de homologação
1. Rever/aplicar a migração apenas em ambiente de homologação com backup verificável.
2. Configurar os secrets do WhatsApp Business Cloud API; não colar tokens no chat nem no repositório.
3. Deploy da Edge Function com JWT obrigatório.
4. Ligar Android à RPC existente `ciadi_criar_alerta_sos`; após sucesso, chamar `ciadi-sos-whatsapp` com `alerta_id`.
5. Atualizar o Dashboard para subscrever `ciadi_sos_alertas` via Realtime e exibir categoria, nível, estado, hora e localização apenas quando autorizada.
6. Testar: utilizador autorizado, utilizador não autorizado, sessão expirada, localização recusada, WhatsApp indisponível, resposta 4xx/5xx, duplicação, repetição após falha e reconexão do Dashboard.
7. APK debug só pode ser considerado atualizado após testes passarem e assinatura/verificação do artefacto.

## Limitação conhecida
A criação do alerta atual grava notificação interna no Supabase, mas não confirma entrega WhatsApp. Esta branch prepara o serviço de envio; o fluxo Android e o Dashboard ainda precisam ser ligados e validados antes de uso real.
