# CIADI — Modelo de mensagem SOS para aprovação WhatsApp

**Estado:** rascunho para submissão; ainda não aprovado pela Meta.
**Nome sugerido:** `ciadi_alerta_sos`
**Categoria sugerida:** Utility (a classificação final é da Meta).
**Idioma:** Português (Portugal), escolher o código de idioma que estiver disponível no WhatsApp Manager.
**Destinatário de teste/produção:** confirmar o número CIADI `949090999` em formato internacional `244949090999` e confirmar que esse número aceita mensagens. O número de destino não é o mesmo que o Phone Number ID remetente.

## Corpo sugerido

`Alerta SOS CIADI: referência {{1}}. Nível {{2}}, categoria {{3}}, registado às {{4}}. {{5}} Consulte o Dashboard CIADI para os detalhes autorizados.`

## Variáveis de exemplo para pré-visualização

1. Referência: SOS-EXEMPLO-001 (apenas demonstração, sem dados reais)
2. Nível: ALTO
3. Categoria: segurança
4. Data/hora: 26/10/2026 10:30
5. Localização: Localização autorizada disponível no Dashboard / Localização não partilhada

## Privacidade e segurança

- Não incluir nome da criança, diagnóstico, texto livre do alerta, número de processo ou coordenadas exatas na mensagem.
- Consultar localização e detalhes apenas no Dashboard, após autenticação e autorização.
- Utilizar opt-in/documentação de base legal e avaliar necessidade de comunicar este canal aos responsáveis e profissionais.
- Nunca guardar tokens da Meta no Android, no repositório Git ou em mensagens.
- Confirmar que o modelo está aprovado e que o número de destino está autorizado a receber mensagens antes de ativar envios reais.

## Configuração da API necessária

- Meta Business Portfolio e WhatsApp Business Account.
- Número remetente registado na Cloud API e respetivo Phone Number ID.
- Token de acesso de sistema guardado apenas como segredo da Edge Function.
- Template aprovado no WhatsApp Manager.
- Webhook de status de mensagens para diferenciar enviado, entregue, lido e falhado. A resposta HTTP de envio, por si só, não prova entrega final.
- Secrets no Supabase: `CIADI_SOS_WHATSAPP_TO`, `WHATSAPP_PHONE_NUMBER_ID`, `WHATSAPP_ACCESS_TOKEN`, `WHATSAPP_GRAPH_API_VERSION`, `WHATSAPP_SOS_TEMPLATE_NAME`, `WHATSAPP_SOS_TEMPLATE_LANGUAGE`.

## Aprovação e testes

Submeter o modelo no WhatsApp Manager e aguardar estado Approved. Testar primeiro com número autorizado de homologação. Confirmar que os 5 parâmetros no código correspondem exatamente às 5 variáveis do template. Não enviar mensagens a destinatários reais antes de aprovação e teste de consentimento.
