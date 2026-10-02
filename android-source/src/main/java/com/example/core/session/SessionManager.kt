package com.example.core.session

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.domain.model.Permission
import com.example.domain.model.UserProfile
import com.example.domain.model.UserRole
import com.example.domain.model.UserSession
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private val Context.dataStore by preferencesDataStore(name = "ciadi_session")

class SessionManager(private val context: Context) {

    private val KEY_ACCESS_TOKEN = stringPreferencesKey("access_token")
    private val KEY_REFRESH_TOKEN = stringPreferencesKey("refresh_token")
    private val KEY_EXPIRES_AT = stringPreferencesKey("expires_at")
    private val KEY_USER_ID = stringPreferencesKey("user_id")
    private val KEY_USER_EMAIL = stringPreferencesKey("user_email")
    private val KEY_USER_ROLE = stringPreferencesKey("user_role")
    private val KEY_USER_NAME = stringPreferencesKey("user_name")
    private val KEY_ACTIVE_PATIENT_ID = stringPreferencesKey("active_patient_id")
    private val KEY_ACTIVE_PATIENT_NAME = stringPreferencesKey("active_patient_name")
    private val KEY_SPECIALTY = stringPreferencesKey("specialty")

    private val _sessionFlow = MutableStateFlow<UserSession>(UserSession.Unauthenticated)
    val sessionFlow: StateFlow<UserSession> = _sessionFlow.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.IO)
    private var restoreJob: Job? = null

    init {
        restoreSession()
    }

    private fun restoreSession() {
        restoreJob = scope.launch {
            try {
                if (_sessionFlow.value is UserSession.Authenticated) {
                    return@launch
                }
                val prefs = context.dataStore.data.first()
                if (_sessionFlow.value is UserSession.Authenticated) {
                    return@launch
                }
                val token = prefs[KEY_ACCESS_TOKEN]
                val refreshToken = prefs[KEY_REFRESH_TOKEN]
                val userId = prefs[KEY_USER_ID]
                val email = prefs[KEY_USER_EMAIL]
                val roleCode = prefs[KEY_USER_ROLE]
                val name = prefs[KEY_USER_NAME]
                val activePatientId = prefs[KEY_ACTIVE_PATIENT_ID]
                val activePatientName = prefs[KEY_ACTIVE_PATIENT_NAME]
                val specialty = prefs[KEY_SPECIALTY]

                if (!token.isNullOrBlank() && !email.isNullOrBlank()) {
                    val role = UserRole.fromCode(roleCode)
                    if (role != null) {
                        val profile = UserProfile(
                            id = userId ?: "usr_restored",
                            email = email,
                            fullName = name ?: "Usuário CIADI+",
                            role = role,
                            activePatientId = activePatientId,
                            activePatientName = activePatientName,
                            specialty = specialty,
                            permissions = Permission.defaultPermissionsFor(role)
                        )
                        _sessionFlow.value = UserSession.Authenticated(
                            user = profile,
                            accessToken = token,
                            refreshToken = refreshToken ?: ""
                        )
                    } else if (_sessionFlow.value !is UserSession.Authenticated) {
                        _sessionFlow.value = UserSession.Unauthenticated
                    }
                } else if (_sessionFlow.value !is UserSession.Authenticated) {
                    _sessionFlow.value = UserSession.Unauthenticated
                }
            } catch (c: CancellationException) {
                // Não alterar estado se a corrotina foi cancelada
                throw c
            } catch (_: Exception) {
                if (_sessionFlow.value !is UserSession.Authenticated) {
                    _sessionFlow.value = UserSession.Unauthenticated
                }
            }
        }
    }

    fun getCurrentAccessToken(): String? {
        return when (val session = _sessionFlow.value) {
            is UserSession.Authenticated -> session.accessToken
            else -> null
        }
    }

    fun getCurrentRefreshToken(): String? {
        return when (val session = _sessionFlow.value) {
            is UserSession.Authenticated -> session.refreshToken
            else -> null
        }
    }

    fun getCurrentUserId(): String? {
        return when (val session = _sessionFlow.value) {
            is UserSession.Authenticated -> session.user.id
            else -> null
        }
    }

    fun isSessionValid(): Boolean {
        return when (val session = _sessionFlow.value) {
            is UserSession.Authenticated -> session.accessToken.isNotBlank() && session.user.id.isNotBlank()
            else -> false
        }
    }

    fun setAuthenticated(
        user: UserProfile,
        accessToken: String,
        refreshToken: String,
        expiresAt: Long? = null
    ) {
        restoreJob?.cancel()
        _sessionFlow.value = UserSession.Authenticated(
            user = user,
            accessToken = accessToken,
            refreshToken = refreshToken
        )

        scope.launch {
            context.dataStore.edit { prefs ->
                prefs[KEY_ACCESS_TOKEN] = accessToken
                prefs[KEY_REFRESH_TOKEN] = refreshToken
                if (expiresAt != null) {
                    prefs[KEY_EXPIRES_AT] = expiresAt.toString()
                }
                prefs[KEY_USER_ID] = user.id
                prefs[KEY_USER_EMAIL] = user.email
                prefs[KEY_USER_ROLE] = user.role.code
                prefs[KEY_USER_NAME] = user.fullName
                if (!user.activePatientId.isNullOrBlank()) {
                    prefs[KEY_ACTIVE_PATIENT_ID] = user.activePatientId
                }
                if (!user.activePatientName.isNullOrBlank()) {
                    prefs[KEY_ACTIVE_PATIENT_NAME] = user.activePatientName
                }
                if (!user.specialty.isNullOrBlank()) {
                    prefs[KEY_SPECIALTY] = user.specialty
                }
            }
        }
    }

    fun updateTokens(accessToken: String, refreshToken: String, expiresAt: Long? = null) {
        val current = _sessionFlow.value
        if (current is UserSession.Authenticated) {
            _sessionFlow.value = current.copy(
                accessToken = accessToken,
                refreshToken = refreshToken
            )
            scope.launch {
                context.dataStore.edit { prefs ->
                    prefs[KEY_ACCESS_TOKEN] = accessToken
                    prefs[KEY_REFRESH_TOKEN] = refreshToken
                    if (expiresAt != null) {
                        prefs[KEY_EXPIRES_AT] = expiresAt.toString()
                    }
                }
            }
        }
    }

    fun updateProfile(profile: UserProfile) {
        val current = _sessionFlow.value
        if (current is UserSession.Authenticated) {
            _sessionFlow.value = current.copy(user = profile)
            scope.launch {
                context.dataStore.edit { prefs ->
                    prefs[KEY_USER_ID] = profile.id
                    prefs[KEY_USER_EMAIL] = profile.email
                    prefs[KEY_USER_ROLE] = profile.role.code
                    prefs[KEY_USER_NAME] = profile.fullName
                    if (!profile.activePatientId.isNullOrBlank()) {
                        prefs[KEY_ACTIVE_PATIENT_ID] = profile.activePatientId
                    }
                    if (!profile.activePatientName.isNullOrBlank()) {
                        prefs[KEY_ACTIVE_PATIENT_NAME] = profile.activePatientName
                    }
                    if (!profile.specialty.isNullOrBlank()) {
                        prefs[KEY_SPECIALTY] = profile.specialty
                    }
                }
            }
        }
    }

    fun clearSession() {
        restoreJob?.cancel()
        _sessionFlow.value = UserSession.Unauthenticated
        scope.launch {
            context.dataStore.edit { prefs ->
                prefs.clear()
            }
        }
    }

    fun switchRoleForPreview(role: UserRole) {
        val current = _sessionFlow.value
        if (current is UserSession.Authenticated) {
            val updatedProfile = current.user.copy(
                role = role,
                permissions = Permission.defaultPermissionsFor(role)
            )
            updateProfile(updatedProfile)
        }
    }
}
