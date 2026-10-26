import { createClient } from "npm:@supabase/supabase-js@2";

const json = (body: unknown, status = 200) =>
  new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json", "Cache-Control": "no-store" },
  });

Deno.serve(async (req: Request) => {
  if (req.method !== "POST") return json({ error: "Método não permitido" }, 405);

  const authorization = req.headers.get("Authorization");
  if (!authorization?.startsWith("Bearer ")) return json({ error: "Autenticação necessária" }, 401);

  const url = Deno.env.get("SUPABASE_URL");
  const anonKey = Deno.env.get("SUPABASE_ANON_KEY");
  const serviceRoleKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY");
  if (!url || !anonKey || !serviceRoleKey) return json({ error: "Configuração Supabase incompleta" }, 500);

  const userClient = createClient(url, anonKey, {
    global: { headers: { Authorization: authorization } },
    auth: { persistSession: false, autoRefreshToken: false },
  });
  const { data: authData, error: authError } = await userClient.auth.getUser();
  if (authError || !authData.user) return json({ error: "Sessão inválida ou expirada" }, 401);

  const body = await req.json().catch(() => null);
  const alertId = typeof body?.alerta_id === "string" ? body.alerta_id : "";
  if (!/^[0-9a-f]{8}-[0-9a-f]{4}-[1-8][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i.test(alertId)) {
    return json({ error: "alerta_id inválido" }, 400);
  }

  const recipient = (Deno.env.get("CIADI_SOS_WHATSAPP_TO") ?? "").replace(/\D/g, "");
  const phoneNumberId = Deno.env.get("WHATSAPP_PHONE_NUMBER_ID");
  const accessToken = Deno.env.get("WHATSAPP_ACCESS_TOKEN");
  const apiVersion = Deno.env.get("WHATSAPP_GRAPH_API_VERSION") ?? "v23.0";
  const templateName = Deno.env.get("WHATSAPP_SOS_TEMPLATE_NAME");
  const templateLanguage = Deno.env.get("WHATSAPP_SOS_TEMPLATE_LANGUAGE") ?? "pt_PT";
  if (!recipient || !phoneNumberId || !accessToken || !templateName) {
    return json({ error: "WhatsApp Cloud API não configurada; nenhum alerta foi enviado." }, 503);
  }

  const admin = createClient(url, serviceRoleKey, {
    auth: { persistSession: false, autoRefreshToken: false },
  });
  const { data: profile, error: profileError } = await admin
    .from("perfis").select("tipo,ativo").eq("id", authData.user.id).maybeSingle();
  if (profileError) return json({ error: "Não foi possível validar o perfil" }, 500);
  if (!profile || profile.ativo === false) return json({ error: "Perfil inativo ou inexistente" }, 403);

  const { data: alert, error: alertError } = await admin
    .from("ciadi_sos_alertas")
    .select("id,perfil_id,paciente_id,categoria,nivel,latitude,longitude,localizacao_autorizada,criado_em")
    .eq("id", alertId).maybeSingle();
  if (alertError) return json({ error: "Não foi possível consultar o alerta" }, 500);
  if (!alert) return json({ error: "Alerta não encontrado" }, 404);
  const isOwner = alert.perfil_id === authData.user.id;
  const isStaff = ["admin", "gestor"].includes(String(profile.tipo ?? "").toLowerCase());
  if (!isOwner && !isStaff) return json({ error: "Sem autorização para este alerta" }, 403);

  // Do not include patient names, clinical message text, or exact coordinates in WhatsApp.
  const { data: existing, error: existingError } = await admin
    .from("ciadi_sos_whatsapp_entregas")
    .select("id,estado,provider_message_id")
    .eq("alerta_id", alert.id).maybeSingle();
  if (existingError) return json({ error: "Falha ao verificar envio anterior" }, 500);
  if (existing?.estado === "sent") {
    return json({ success: true, duplicate: true, provider_message_id: existing.provider_message_id });
  }

  let deliveryId: string | null = null;
  if (!existing) {
    const { data: created, error: createError } = await admin
      .from("ciadi_sos_whatsapp_entregas")
      .insert({ alerta_id: alert.id, destinatario: recipient, estado: "pending", tentativas: 1 })
      .select("id").single();
    if (createError) {
      if (createError.code === "23505") return json({ success: true, duplicate: true, status: "pending" });
      return json({ error: "Não foi possível registar a tentativa de envio" }, 500);
    }
    deliveryId = created.id;
  } else if (existing.estado === "failed") {
    const { data: retry, error: retryError } = await admin
      .from("ciadi_sos_whatsapp_entregas")
      .update({ estado: "pending", erro_codigo: null, destinatario: recipient, tentativas: 1 })
      .eq("id", existing.id).eq("estado", "failed").select("id").maybeSingle();
    if (retryError) return json({ error: "Não foi possível preparar nova tentativa" }, 500);
    if (!retry) return json({ success: true, duplicate: true, status: "pending" });
    deliveryId = retry.id;
  } else {
    return json({ success: true, duplicate: true, status: "pending" });
  }

  const occurredAt = new Date(alert.criado_em).toLocaleString("pt-PT", { timeZone: "Africa/Luanda" });
  const locationStatus = alert.localizacao_autorizada && alert.latitude != null && alert.longitude != null
    ? "Localização autorizada disponível no Dashboard"
    : "Localização não partilhada";
  const templatePayload = {
    messaging_product: "whatsapp",
    to: recipient,
    type: "template",
    template: {
      name: templateName,
      language: { code: templateLanguage },
      components: [{
        type: "body",
        parameters: [
          { type: "text", text: alert.id },
          { type: "text", text: String(alert.nivel ?? "alto").toUpperCase() },
          { type: "text", text: String(alert.categoria ?? "geral") },
          { type: "text", text: occurredAt },
          { type: "text", text: locationStatus },
        ],
      }],
    },
  };

  let response: Response;
  try {
    response = await fetch(`https://graph.facebook.com/${apiVersion}/${phoneNumberId}/messages`, {
      method: "POST",
      headers: { Authorization: `Bearer ${accessToken}`, "Content-Type": "application/json" },
      body: JSON.stringify(templatePayload),
    });
  } catch {
    await admin.from("ciadi_sos_whatsapp_entregas")
      .update({ estado: "failed", erro_codigo: "network_error", atualizado_em: new Date().toISOString() })
      .eq("id", deliveryId);
    return json({ error: "Falha de rede ao enviar alerta WhatsApp" }, 502);
  }

  const result = await response.json().catch(() => ({}));
  if (!response.ok) {
    const code = String(result?.error?.code ?? response.status).slice(0, 80);
    await admin.from("ciadi_sos_whatsapp_entregas")
      .update({ estado: "failed", erro_codigo: code, atualizado_em: new Date().toISOString() })
      .eq("id", deliveryId);
    return json({ error: "O fornecedor WhatsApp recusou o envio", provider_code: code }, 502);
  }

  const providerMessageId = typeof result?.messages?.[0]?.id === "string" ? result.messages[0].id : null;
  await admin.from("ciadi_sos_whatsapp_entregas")
    .update({
      estado: "sent",
      provider_message_id: providerMessageId,
      erro_codigo: null,
      enviado_em: new Date().toISOString(),
      atualizado_em: new Date().toISOString(),
    })
    .eq("id", deliveryId);

  return json({ success: true, alerta_id: alert.id, provider_message_id: providerMessageId });
});
