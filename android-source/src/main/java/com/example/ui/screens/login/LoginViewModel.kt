package com.example.ui.screens.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.model.SupabaseStatus
import com.example.domain.model.UserRole
import com.example.domain.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val selectedPortalDoor: UserRole = UserRole.RESPONSAVEL,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val passwordResetSent: Boolean = false,
    val supabaseStatus: SupabaseStatus? = null
)

class LoginViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        LoginUiState(supabaseStatus = authRepository.getSupabaseStatus())
    )
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onEmailChanged(newEmail: String) {
        _uiState.update { it.copy(email = newEmail, errorMessage = null) }
    }

    fun onPasswordChanged(newPassword: String) {
        _uiState.update { it.copy(password = newPassword, errorMessage = null) }
    }

    fun onPortalDoorSelected(door: UserRole) {
        _uiState.update {
            it.copy(
                selectedPortalDoor = door,
                errorMessage = null
            )
        }
    }

    fun login(
        onSuccess: () -> Unit,
        onBlocked: (String) -> Unit
    ) {
        val state = _uiState.value
        if (state.email.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Por favor, preencha o seu e-mail de acesso.") }
            return
        }
        if (state.password.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Por favor, insira a sua palavra-passe.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = authRepository.signInWithEmail(state.email.trim(), state.password)
            _uiState.update { it.copy(isLoading = false) }

            if (result.isSuccess) {
                val profile = result.getOrNull()
                if (profile == null) {
                    onBlocked("A autenticação foi concluída, mas o seu perfil CIADI+ não foi encontrado. Contacte a administração do CIADI.")
                    return@launch
                }
                if (!profile.ativo) {
                    onBlocked("O seu acesso ao CIADI+ está desativado. Contacte a administração do CIADI.")
                    return@launch
                }
                if (profile.statusAprovacao.equals("pendente", ignoreCase = true)) {
                    onBlocked("O seu pedido de acesso está pendente de aprovação pelo CIADI.")
                    return@launch
                }
                if (profile.statusAprovacao.equals("rejeitado", ignoreCase = true)) {
                    onBlocked("O pedido de acesso não foi aprovado. Contacte a Secretaria do CIADI para obter mais informações.")
                    return@launch
                }
                if (profile.role.code.isBlank()) {
                    onBlocked("O seu perfil ainda não está autorizado a utilizar esta área do CIADI+. Contacte a administração do CIADI.")
                    return@launch
                }
                onSuccess()
            } else {
                val errorMsg = result.exceptionOrNull()?.message ?: "Falha ao autenticar no Supabase."
                if (errorMsg.contains("não foi encontrado") ||
                    errorMsg.contains("desativado") ||
                    errorMsg.contains("não está autorizado") ||
                    errorMsg.contains("pendente de aprovação") ||
                    errorMsg.contains("não foi aprovado")
                ) {
                    onBlocked(errorMsg)
                } else {
                    _uiState.update {
                        it.copy(errorMessage = errorMsg)
                    }
                }
            }
        }
    }

    fun requestPasswordReset() {
        val email = _uiState.value.email
        if (email.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Informe o seu e-mail para recuperar o acesso.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = authRepository.requestPasswordReset(email)
            _uiState.update {
                it.copy(
                    isLoading = false,
                    passwordResetSent = result.isSuccess,
                    errorMessage = if (result.isFailure) result.exceptionOrNull()?.message else null
                )
            }
        }
    }

    fun dismissPasswordResetAlert() {
        _uiState.update { it.copy(passwordResetSent = false) }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
