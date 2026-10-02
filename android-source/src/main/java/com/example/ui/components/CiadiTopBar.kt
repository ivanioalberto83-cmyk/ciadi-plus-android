package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.model.UserProfile
import com.example.ui.theme.CIADIColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CiadiTopBar(
    title: String,
    currentUser: UserProfile? = null,
    onNavigateBack: (() -> Unit)? = null,
    onProfileClick: (() -> Unit)? = null,
    onJanethClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (onNavigateBack == null) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_ciadi_logo_1790332105105),
                        contentDescription = "Logo CIADI",
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(6.dp))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = CIADIColors.Brown
                    )
                )
            }
        },
        navigationIcon = {
            if (onNavigateBack != null) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.testTag("topbar_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Voltar",
                        tint = CIADIColors.Brown
                    )
                }
            }
        },
        actions = {
            // Acesso Rápido à Janeth (Assistente Oficial CIADI)
            if (onJanethClick != null) {
                IconButton(
                    onClick = onJanethClick,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("topbar_janeth_button")
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.janeth_avatar_1790333481531),
                        contentDescription = "Falar com Janeth",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
            }

            if (currentUser != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clickable(enabled = onProfileClick != null) { onProfileClick?.invoke() }
                        .padding(horizontal = 4.dp, vertical = 4.dp)
                ) {
                    RoleBadge(role = currentUser.role)
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = { onProfileClick?.invoke() },
                        modifier = Modifier.size(36.dp).testTag("topbar_profile_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = "Perfil do usuário",
                            tint = CIADIColors.Brown
                        )
                    }
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = CIADIColors.BackgroundLight,
            titleContentColor = CIADIColors.Brown,
            navigationIconContentColor = CIADIColors.Brown,
            actionIconContentColor = CIADIColors.Brown
        ),
        modifier = modifier
    )
}
