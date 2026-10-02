package com.example.data.remote.realtime

import android.util.Log
import com.example.core.config.SupabaseConfig
import com.example.data.remote.client.SupabaseClientFactory
import com.example.data.remote.dto.ChatMensagemDto
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * Gerenciador oficial de conexões Realtime do Supabase no CIADI+.
 *
 * Escuta eventos da tabela 'ciadi_chat_mensagens' via WebSocket do Supabase Realtime (Phoenix Channels)
 * com fallback contínuo via Polling seguro com RLS.
 *
 * Estados de subscrição:
 * - SUBSCRIBED
 * - CHANNEL_ERROR
 * - TIMED_OUT
 * - CLOSED
 */
class SupabaseRealtimeManager(
    private val clientFactory: SupabaseClientFactory
) {
    companion object {
        private const val TAG = "SupabaseRealtime"
        const val STATUS_SUBSCRIBED = "SUBSCRIBED"
        const val STATUS_CHANNEL_ERROR = "CHANNEL_ERROR"
        const val STATUS_TIMED_OUT = "TIMED_OUT"
        const val STATUS_CLOSED = "CLOSED"
    }

    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val mensagemAdapter = moshi.adapter(ChatMensagemDto::class.java)

    private val statusMap = ConcurrentHashMap<String, MutableStateFlow<String>>()
    private val activeWebSockets = ConcurrentHashMap<String, WebSocket>()
    private val pollingJobs = ConcurrentHashMap<String, Job>()
    private var heartbeatJob: Job? = null

    private val okHttpClient = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()

    fun getStatusFlow(grupoId: String): StateFlow<String> {
        return statusMap.getOrPut(grupoId) { MutableStateFlow(STATUS_CLOSED) }.asStateFlow()
    }

    fun startListening(
        grupoId: String,
        onNewMessage: (ChatMensagemDto) -> Unit
    ) {
        val flow = statusMap.getOrPut(grupoId) { MutableStateFlow(STATUS_CLOSED) }
        flow.value = "CONNECTING..."

        // 1. Inicia conexão WebSocket Phoenix do Supabase Realtime
        connectWebSocket(grupoId, onNewMessage)

        // 2. Inicia polling de sincronização rápida (fallback/garantia RLS a cada 3s)
        startPolling(grupoId, onNewMessage)
    }

    fun stopListening(grupoId: String) {
        activeWebSockets.remove(grupoId)?.close(1000, "Leaving channel")
        pollingJobs.remove(grupoId)?.cancel()
        statusMap[grupoId]?.value = STATUS_CLOSED
    }

    private fun connectWebSocket(
        grupoId: String,
        onNewMessage: (ChatMensagemDto) -> Unit
    ) {
        val flow = statusMap.getOrPut(grupoId) { MutableStateFlow(STATUS_CLOSED) }
        val baseWsUrl = SupabaseConfig.supabaseUrl
            .replace("https://", "wss://")
            .replace("http://", "ws://")
        val wsUrl = "$baseWsUrl/realtime/v1/websocket?apikey=${SupabaseConfig.supabasePublishableKey}&vsn=1.0.0"

        val request = Request.Builder()
            .url(wsUrl)
            .build()

        val listener = object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "WebSocket conectado para grupo $grupoId")
                activeWebSockets[grupoId] = webSocket

                val token = (clientFactory.sessionManager?.sessionFlow?.value as? com.example.domain.model.UserSession.Authenticated)?.accessToken
                    ?: SupabaseConfig.supabasePublishableKey

                // Envia join do Phoenix Channel para a tabela de mensagens com token de autorização
                val joinMsg = JSONObject().apply {
                    put("topic", "realtime:public:ciadi_chat_mensagens")
                    put("event", "phx_join")
                    put("payload", JSONObject().apply {
                        put("access_token", token)
                        put("config", JSONObject().apply {
                            put("broadcast", JSONObject().apply { put("self", false) })
                            put("presence", JSONObject().apply { put("key", "") })
                            put("postgres_changes", org.json.JSONArray().apply {
                                put(JSONObject().apply {
                                    put("event", "INSERT")
                                    put("schema", "public")
                                    put("table", "ciadi_chat_mensagens")
                                    put("filter", "grupo_id=eq.$grupoId")
                                })
                            })
                        })
                    })
                    put("ref", "join_${System.currentTimeMillis()}")
                }
                webSocket.send(joinMsg.toString())
                flow.value = STATUS_SUBSCRIBED
                Log.d("CIADI_CHAT", "CHAT_REALTIME_STATUS: SUBSCRIBED ($grupoId)")

                // Mantém batimentos cardíacos (Heartbeat)
                startHeartbeat(webSocket)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val json = JSONObject(text)
                    val event = json.optString("event")
                    val payload = json.optJSONObject("payload")

                    if (event == "phx_reply") {
                        val responseObj = payload?.optJSONObject("response")
                        val status = payload?.optString("status")
                        if (status == "ok") {
                            flow.value = STATUS_SUBSCRIBED
                        } else if (status == "error") {
                            flow.value = STATUS_CHANNEL_ERROR
                        }
                    } else if (event == "INSERT" || event == "postgres_changes") {
                        val record = payload?.optJSONObject("data")?.optJSONObject("record")
                            ?: payload?.optJSONObject("record")
                        if (record != null) {
                            val msgGrupoId = record.optString("grupo_id")
                            if (msgGrupoId.isBlank() || msgGrupoId == grupoId) {
                                val parsed = mensagemAdapter.fromJson(record.toString())
                                if (parsed != null) {
                                    onNewMessage(parsed)
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Erro ao processar mensagem Realtime: ${e.message}")
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.w(TAG, "Falha no WebSocket Realtime ($grupoId): ${t.message}")
                flow.value = STATUS_CHANNEL_ERROR
                Log.d("CIADI_CHAT", "CHAT_REALTIME_STATUS: CHANNEL_ERROR ($grupoId)")
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                flow.value = STATUS_CLOSED
                Log.d("CIADI_CHAT", "CHAT_REALTIME_STATUS: CLOSED ($grupoId)")
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                flow.value = STATUS_CLOSED
                Log.d("CIADI_CHAT", "CHAT_REALTIME_STATUS: CLOSED ($grupoId)")
            }
        }

        try {
            val ws = okHttpClient.newWebSocket(request, listener)
            activeWebSockets[grupoId] = ws
        } catch (e: Exception) {
            Log.e(TAG, "Não foi possível conectar WebSocket: ${e.message}")
            flow.value = STATUS_CHANNEL_ERROR
            Log.d("CIADI_CHAT", "CHAT_REALTIME_STATUS: CHANNEL_ERROR ($grupoId)")
        }
    }

    private fun startPolling(
        grupoId: String,
        onNewMessage: (ChatMensagemDto) -> Unit
    ) {
        pollingJobs[grupoId]?.cancel()
        pollingJobs[grupoId] = scope.launch {
            var knownIds = mutableSetOf<String>()
            while (isActive) {
                try {
                    delay(3000)
                    if (!clientFactory.isReadyForConnection()) continue

                    val resp = clientFactory.restApi.getChatMensagens(
                        grupoIdFilter = "eq.$grupoId",
                        order = "created_at.asc"
                    )
                    if (resp.isSuccessful) {
                        val items = resp.body().orEmpty()
                        for (item in items) {
                            if (item.id.isNotBlank() && knownIds.add(item.id)) {
                                onNewMessage(item)
                            }
                        }
                        // Se websocket falhar, o polling mantém estado ativo
                        val currentStatus = statusMap[grupoId]?.value
                        if (currentStatus == STATUS_CLOSED || currentStatus == STATUS_CHANNEL_ERROR) {
                            statusMap[grupoId]?.value = STATUS_SUBSCRIBED
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Polling falhou temporariamente: ${e.message}")
                }
            }
        }
    }

    private fun startHeartbeat(webSocket: WebSocket) {
        heartbeatJob?.cancel()
        heartbeatJob = scope.launch {
            while (isActive) {
                delay(25000)
                try {
                    val hb = JSONObject().apply {
                        put("topic", "phoenix")
                        put("event", "heartbeat")
                        put("payload", JSONObject())
                        put("ref", "hb_${System.currentTimeMillis()}")
                    }
                    webSocket.send(hb.toString())
                } catch (e: Exception) {
                    break
                }
            }
        }
    }
}
