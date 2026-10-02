package com.example.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Paleta Oficial de Identidade Visual do CIADI
 * Centro Integrado de Apoio e Desenvolvimento Individual
 *
 * Configurada com alto contraste e legibilidade estrita WCAG 2.1 AA.
 */
object CIADIColors {
    // Cores Mestras Oficiais
    val OrangePrimary = Color(0xFFF58200)       // Laranja Oficial CIADI
    val OrangeDark = Color(0xFFD46F00)          // Laranja Escurecido
    val OrangeLight = Color(0xFFFFB85C)         // Laranja Claro
    val Cream = Color(0xFFFFE7BF)               // Creme Acolhedor
    val CreamLight = Color(0xFFFFF7EB)          // Creme Suave
    val Yellow = Color(0xFFFFDF19)              // Amarelo Destaque
    val Brown = Color(0xFF6F351F)               // Castanho Institucional
    val BrownDark = Color(0xFF4A1F10)           // Castanho Profundo
    val BackgroundLight = Color(0xFFFBFDFF)     // Fundo Luminoso (#FBFDFF)
    val SurfaceWhite = Color(0xFFFFFFFF)        // Branco Puro
    val SurfaceCard = Color(0xFFFAFBFC)         // Superfície Leve

    // Tons de Texto e Alto Contraste (Regra de Legibilidade CIADI+)
    val TextPrimary = Color(0xFF111111)         // Texto Principal Preto Nítido (#111111)
    val TextSecondary = Color(0xFF4A4A4A)       // Texto Secundário Nítido
    val TextMuted = Color(0xFF6E6E6E)           // Texto Terciário Legível
    val Outline = Color(0xFFDCD2CB)             // Bordas Visíveis

    // Cores Auxiliares Clínicas
    val SoftTealAuxiliary = Color(0xFF0A5866)   // Verde-petróleo clínico
    val TealPrimary = SoftTealAuxiliary         // Alias Teal Primário
    val SoftTealContainer = Color(0xFFE2F4F7)
    val SuccessGreen = Color(0xFF1B6A38)
    val WarningAmber = Color(0xFFE65100)
    val ErrorRed = Color(0xFFC62828)
    val InfoBlue = Color(0xFF0B63A5)

    // Cores para Perfis
    val RoleFamily = Color(0xFFF58200)          // Laranja Afetivo
    val RoleAt = Color(0xFFE65100)              // Laranja Intenso Campo
    val RoleProfessional = Color(0xFF00796B)    // Verde Clínico
    val RoleAdmin = Color(0xFF6F351F)           // Castanho Institucional
    val RoleGestor = Color(0xFF512DA8)          // Púrpura Gestão
    val RoleOperador = Color(0xFF455A64)        // Ardósia Operacional

    // Modo Escuro
    val BackgroundDark = Color(0xFF161210)
    val SurfaceDark = Color(0xFF231D1A)
    val SurfaceVariantDark = Color(0xFF332A26)
    val TextPrimaryDark = Color(0xFFF7F1EE)
    val TextSecondaryDark = Color(0xFFD4C7C2)
    val OutlineDark = Color(0xFF574A44)
}
