// Supabase Edge Function: ciadi-sos-provider-webhook
// Receives delivery/read/failure receipts from WhatsApp Cloud API and Resend.
// Required secrets: SUPABASE_URL, SUPABASE_SERVICE_ROLE_KEY,
// WHATSAPP_WEBHOOK_VERIFY_TOKEN, WHATSAPP_APP_SECRET, RESEND_WEBHOOK_SECRET.
import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

const corsHeaders = { "Content-Type": "application/json" };

Deno.serve(async (req) => {
  const url = Deno.env.get("SUPABASE_URL");
  const serviceKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY");
  if (!url || !serviceKey) return json({ error: "Backend não configurado" }, 500);

  if (req.method === "GET") {
    const verifyToken = Deno.env.get("WHATSAPP_WEBHOOK_VERIFY_TOKEN");
    const mode = new URL(req.url).searchParams.get("hub.mode");
    const token = new URL(req.url).searchParams.get("hub.verify_token");
    const challenge = new URL(req.url).searchParams.get("hub.challenge");
    if (mode === "subscribe" && verifyToken && token === verifyToken && challenge) {
      return new Response(challenge, { status: 200, headers: { "Content-Type": "text/plain" } });
    }
    return json({ error: "Verificação de webhook recusada" }, 403);
  }

  if (req.method !== "POST") return json({ error: "Método não permitido" }, 405);
  const rawBody = await req.text();
  const admin = createClient(url, serviceKey, { auth: { persistSession: false, autoRefreshToken: false } });

  const resendId = req.headers.get("svix-id");
  const resendSignature = req.headers.get("svix-signature");
  const resendTimestamp = req.headers.get("svix-timestamp");
  if (resendId && resendSignature && resendTimestamp) {
    const secret = Deno.env.get("RESEND_WEBHOOK_SECRET");
    if (!secret || !await verifyResendSignature(secret, resendId, resendTimestamp, resendSignature, rawBody)) {
      return json({ error: "Assinatura Resend inválida" }, 401);
    }
    const event = safeJson(rawBody);
    const providerId = event?.data?.email_id ?? event?.data?.id;
    if (!providerId) return json({ received: true, updated: 0 });
    const type = String(event?.type ?? "");
    const state = type === "email.delivered" ? "entregue"
      : ["email.bounced", "email.failed", "email.complained"].includes(type) ? "falhou"
      : type === "email.delivery_delayed" ? "aceite_provedor" : null;
    if (!state) return json({ received: true, ignored: true });
    const { data, error } = await admin.from("ciadi_sos_entregas")
      .update({ estado: state, atualizado_em: new Date().toISOString(), enviado_em: state === "entregue" ? new Date().toISOString() : undefined })
      .eq("canal", "email").eq("id_provedor", providerId).select("id,alerta_id");
    if (error) return json({ error: "Não foi possível atualizar o estado do e-mail" }, 500);
    await refreshAlertStatuses(admin, data ?? []);
    return json({ received: true, updated: data?.length ?? 0 });
  }

  const whatsappSignature = req.headers.get("x-hub-signature-256");
  const appSecret = Deno.env.get("WHATSAPP_APP_SECRET");
  if (!whatsappSignature || !appSecret || !await verifyWhatsAppSignature(appSecret, whatsappSignature, rawBody)) {
    return json({ error: "Assinatura WhatsApp inválida" }, 401);
  }
  const event = safeJson(rawBody);
  const statuses = (event?.entry ?? []).flatMap((entry: any) =>
    (entry.changes ?? []).flatMap((change: any) => change.value?.statuses ?? [])
  );
  let updated = 0;
  const affected: Array<{ id: string; alerta_id: string }> = [];
  for (const statusItem of statuses) {
    const providerId = statusItem?.id;
    if (!providerId) continue;
    const providerStatus = String(statusItem?.status ?? "");
    const state = providerStatus === "read" ? "lido"
      : providerStatus === "delivered" ? "entregue"
      : providerStatus === "sent" ? "aceite_provedor"
      : providerStatus === "failed" ? "falhou" : null;
    if (!state) continue;
    const { data, error } = await admin.from("ciadi_sos_entregas")
      .update({ estado: state, atualizado_em: new Date().toISOString(), enviado_em: ["entregue", "lido"].includes(state) ? new Date().toISOString() : undefined })
      .eq("canal", "whatsapp").eq("id_provedor", providerId).select("id,alerta_id");
    if (error) return json({ error: "Não foi possível atualizar o estado do WhatsApp" }, 500);
    updated += data?.length ?? 0;
    affected.push(...(data ?? []));
  }
  await refreshAlertStatuses(admin, affected);
  return json({ received: true, updated });
});

async function verifyWhatsAppSignature(secret: string, header: string, body: string): Promise<boolean> {
  const expectedPrefix = "sha256=";
  if (!header.startsWith(expectedPrefix)) return false;
  const key = await crypto.subtle.importKey("raw", new TextEncoder().encode(secret), { name: "HMAC", hash: "SHA-256" }, false, ["sign"]);
  const signature = new Uint8Array(await crypto.subtle.sign("HMAC", key, new TextEncoder().encode(body)));
  return constantTimeEqual(toHex(signature), header.slice(expectedPrefix.length).toLowerCase());
}

async function verifyResendSignature(secret: string, id: string, timestamp: string, signatureHeader: string, body: string): Promise<boolean> {
  const rawSecret = secret.startsWith("whsec_") ? secret.slice(6) : secret;
  let keyBytes: Uint8Array;
  try { keyBytes = Uint8Array.from(atob(rawSecret), (c) => c.charCodeAt(0)); }
  catch { return false; }
  const key = await crypto.subtle.importKey("raw", keyBytes, { name: "HMAC", hash: "SHA-256" }, false, ["sign"]);
  const signedContent = new TextEncoder().encode(`${id}.${timestamp}.${body}`);
  const signature = btoa(String.fromCharCode(...new Uint8Array(await crypto.subtle.sign("HMAC", key, signedContent))));
  const candidates = signatureHeader.split(" ").map((part) => part.split(",")).flat().filter((part) => part.startsWith("v1,")).map((part) => part.slice(3));
  return candidates.some((candidate) => constantTimeEqual(candidate, signature));
}

async function refreshAlertStatuses(admin: ReturnType<typeof createClient>, deliveries: Array<{ alerta_id: string }>) {
  for (const alertId of [...new Set(deliveries.map((d) => d.alerta_id))]) {
    const channelStatus: Record<string, string> = {};
    for (const channel of ["whatsapp", "email"]) {
      const { data } = await admin.from("ciadi_sos_entregas").select("estado").eq("alerta_id", alertId).eq("canal", channel);
      channelStatus[channel] = data?.some((r: any) => r.estado === "lido") ? "lido"
        : data?.some((r: any) => r.estado === "entregue") ? "entregue"
        : data?.some((r: any) => r.estado === "aceite_provedor") ? "aceite_provedor"
        : data?.some((r: any) => r.estado === "falhou") ? "falhou"
        : data?.length ? "pendente" : "nao_configurado";
    }
    await admin.from("ciadi_sos_alertas").update({ canal_estado: channelStatus }).eq("id", alertId);
  }
}

function safeJson(value: string): any {
  try { return JSON.parse(value); } catch { return null; }
}
function toHex(bytes: Uint8Array): string {
  return Array.from(bytes, (b) => b.toString(16).padStart(2, "0")).join("");
}
function constantTimeEqual(a: string, b: string): boolean {
  if (a.length !== b.length) return false;
  let result = 0;
  for (let i = 0; i < a.length; i++) result |= a.charCodeAt(i) ^ b.charCodeAt(i);
  return result === 0;
}
function json(body: unknown, status = 200) {
  return new Response(JSON.stringify(body), { status, headers: corsHeaders });
}
