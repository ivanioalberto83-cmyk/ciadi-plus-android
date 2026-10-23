// Supabase Edge Function: ciadi-sos-dispatch
// Sends queued CIADI SOS deliveries and records provider-level status.
// Required secrets: SUPABASE_URL, SUPABASE_ANON_KEY, SUPABASE_SERVICE_ROLE_KEY,
// WHATSAPP_ACCESS_TOKEN, WHATSAPP_PHONE_NUMBER_ID, RESEND_API_KEY, SOS_FROM_EMAIL.
import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
};

Deno.serve(async (req) => {
  if (req.method === "OPTIONS") return new Response("ok", { headers: corsHeaders });
  if (req.method !== "POST") return json({ error: "Método não permitido" }, 405);

  const authHeader = req.headers.get("Authorization") ?? "";
  const token = authHeader.replace(/^Bearer\s+/i, "");
  if (!token) return json({ error: "Sessão obrigatória" }, 401);

  const url = Deno.env.get("SUPABASE_URL");
  const anonKey = Deno.env.get("SUPABASE_ANON_KEY");
  const serviceKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY");
  if (!url || !anonKey || !serviceKey) return json({ error: "Backend não configurado" }, 500);

  const authClient = createClient(url, anonKey, { global: { headers: { Authorization: `Bearer ${token}` } } });
  const { data: authData, error: authError } = await authClient.auth.getUser(token);
  if (authError || !authData.user) return json({ error: "Sessão inválida" }, 401);

  let payload: { alerta_id?: string };
  try { payload = await req.json(); } catch { return json({ error: "JSON inválido" }, 400); }
  const alertaId = payload.alerta_id;
  if (!alertaId || !/^[0-9a-f-]{36}$/i.test(alertaId)) return json({ error: "ID do alerta inválido" }, 400);

  const admin = createClient(url, serviceKey, { auth: { persistSession: false, autoRefreshToken: false } });
  const { data: alert, error: alertError } = await admin
    .from("ciadi_sos_alertas")
    .select("id,perfil_id,paciente_id,tipo,categoria,nivel_emergencia,mensagem,latitude,longitude,localizacao_autorizada,protocolo,criado_em")
    .eq("id", alertaId).maybeSingle();
  if (alertError || !alert) return json({ error: "Alerta não encontrado" }, 404);

  // Only the person who created the alert or authorized CIADI management may dispatch it.
  const { data: actor } = await admin.from("perfis").select("nome_completo,nome_exibicao,tipo,ativo").eq("id", authData.user.id).maybeSingle();
  if (alert.perfil_id !== authData.user.id) {
    if (!actor?.ativo || !["admin", "gestor", "operador"].includes(actor.tipo)) {
      return json({ error: "Sem autorização para encaminhar este alerta" }, 403);
    }
  }
  const { data: patient } = await admin.from("pacientes").select("nome_completo").eq("id", alert.paciente_id).maybeSingle();
  const patientName = patient?.nome_completo ?? "Assistido não identificado";
  const actorName = actor?.nome_exibicao ?? actor?.nome_completo ?? "Utilizador CIADI+";

  const { data: deliveries, error: deliveriesError } = await admin
    .from("ciadi_sos_entregas")
    .select("*")
    .eq("alerta_id", alertaId)
    .in("estado", ["pendente", "falhou"])
    .lt("tentativas", 3);
  if (deliveriesError) return json({ error: "Não foi possível consultar os canais de envio" }, 500);

  const results: Array<{ canal: string; estado: string; detalhe?: string }> = [];
  for (const delivery of deliveries ?? []) {
    await admin.from("ciadi_sos_entregas").update({
      estado: "a_enviar", tentativas: delivery.tentativas + 1, erro: null, atualizado_em: new Date().toISOString()
    }).eq("id", delivery.id);

    let outcome: { ok: boolean; providerId?: string; error?: string } = { ok: false, error: "Canal não configurado" };
    try {
      const locationUrl = alert.localizacao_autorizada && alert.latitude != null && alert.longitude != null
        ? `https://maps.google.com/?q=${alert.latitude},${alert.longitude}`
        : "Localização não disponível/partilhada";
      const text = [
        `🚨 ALERTA CIADI — ${alert.nivel_emergencia}`,
        `Protocolo: ${alert.protocolo ?? alert.id}`,
        `Assistido: ${patientName}`,
        `Acionado por: ${actorName}`,
        `Tipo: ${alert.tipo} | Categoria: ${alert.categoria}`,
        `Ocorrido em: ${alert.criado_em}`,
        `Descrição: ${String(alert.mensagem ?? "").slice(0, 800)}`,
        `Localização: ${locationUrl}`,
      ].join("\n");

      if (delivery.canal === "whatsapp") {
        const accessToken = Deno.env.get("WHATSAPP_ACCESS_TOKEN");
        const phoneNumberId = Deno.env.get("WHATSAPP_PHONE_NUMBER_ID");
        if (!accessToken || !phoneNumberId) {
          outcome = { ok: false, error: "Faltam WHATSAPP_ACCESS_TOKEN/WHATSAPP_PHONE_NUMBER_ID" };
        } else {
          const templateName = Deno.env.get("WHATSAPP_SOS_TEMPLATE_NAME");
          const templateLanguage = Deno.env.get("WHATSAPP_SOS_TEMPLATE_LANGUAGE");
          // If configured, use the institution's approved template for business-initiated messages.
          // Template body variables must be ordered: level, protocol, patient, category, description, location.
          const whatsappBody = templateName ? {
            messaging_product: "whatsapp",
            recipient_type: "individual",
            to: delivery.destinatario.replace(/\D/g, ""),
            type: "template",
            template: {
              name: templateName,
              language: { code: templateLanguage || "pt_PT" },
              components: [{
                type: "body",
                parameters: [
                  { type: "text", text: String(alert.nivel_emergencia) },
                  { type: "text", text: String(alert.protocolo ?? alert.id) },
                  { type: "text", text: patientName },
                  { type: "text", text: String(alert.categoria) },
                  { type: "text", text: String(alert.mensagem ?? "").slice(0, 800) },
                  { type: "text", text: locationUrl }
                ]
              }]
            }
          } : {
            messaging_product: "whatsapp",
            recipient_type: "individual",
            to: delivery.destinatario.replace(/\D/g, ""),
            type: "text",
            text: { preview_url: false, body: text }
          };
          const resp = await fetch(`https://graph.facebook.com/v21.0/${phoneNumberId}/messages`, {
            method: "POST",
            headers: { Authorization: `Bearer ${accessToken}`, "Content-Type": "application/json" },
            body: JSON.stringify(whatsappBody)
          });
          const data = await resp.json().catch(() => ({}));
          outcome = resp.ok
            ? { ok: true, providerId: data?.messages?.[0]?.id }
            : { ok: false, error: JSON.stringify(data).slice(0, 1000) };
        }
      } else if (delivery.canal === "email") {
        const apiKey = Deno.env.get("RESEND_API_KEY");
        const from = Deno.env.get("SOS_FROM_EMAIL");
        if (!apiKey || !from) {
          outcome = { ok: false, error: "Faltam RESEND_API_KEY/SOS_FROM_EMAIL" };
        } else {
          const resp = await fetch("https://api.resend.com/emails", {
            method: "POST",
            headers: { Authorization: `Bearer ${apiKey}`, "Content-Type": "application/json" },
            body: JSON.stringify({
              from,
              to: [delivery.destinatario],
              subject: `[CIADI SOS ${alert.nivel_emergencia}] ${alert.protocolo ?? alert.id}`,
              text
            })
          });
          const data = await resp.json().catch(() => ({}));
          outcome = resp.ok
            ? { ok: true, providerId: data?.id }
            : { ok: false, error: JSON.stringify(data).slice(0, 1000) };
        }
      }
    } catch (error) {
      outcome = { ok: false, error: String(error).slice(0, 1000) };
    }

    const newState = outcome.ok ? "aceite_provedor" : "falhou";
    await admin.from("ciadi_sos_entregas").update({
      estado: newState,
      id_provedor: outcome.providerId ?? null,
      erro: outcome.error ?? null,
      enviado_em: outcome.ok ? new Date().toISOString() : null,
      atualizado_em: new Date().toISOString()
    }).eq("id", delivery.id);
    results.push({ canal: delivery.canal, estado: newState, detalhe: outcome.error });
  }

  const statusByChannel: Record<string, string> = {};
  for (const channel of ["whatsapp", "email"]) {
    const { data: rows } = await admin.from("ciadi_sos_entregas").select("estado").eq("alerta_id", alertaId).eq("canal", channel);
    statusByChannel[channel] = rows?.some((r) => r.estado === "lido") ? "lido"
      : rows?.some((r) => r.estado === "entregue") ? "entregue"
      : rows?.some((r) => r.estado === "aceite_provedor") ? "aceite_provedor"
      : rows?.some((r) => r.estado === "falhou") ? "falhou"
      : rows?.length ? "pendente" : "nao_configurado";
  }
  await admin.from("ciadi_sos_alertas").update({ canal_estado: statusByChannel }).eq("id", alertaId);
  return json({ success: true, alerta_id: alertaId, protocolo: alert.protocolo, canais: statusByChannel, resultados: results });
});

function json(body: unknown, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { ...corsHeaders, "Content-Type": "application/json" },
  });
}
