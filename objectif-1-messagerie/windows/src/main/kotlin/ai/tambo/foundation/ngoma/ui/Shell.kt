package ai.tambo.foundation.ngoma.ui

import ai.tambo.foundation.ngoma.bridge.AccountBridge
import ai.tambo.foundation.ngoma.bridge.MessagingBridge
import ai.tambo.foundation.ngoma.bridge.VideoBridge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Identifiant de l'utilisateur connecté dans les mocks (à remplacer par l'utilisateur de la session TModeler). */
internal const val ME = 1

private enum class Tab(val label: String, val icon: String) {
    Messages("Messages", "💬"),
    Videos("Vidéos", "▶️"),
    People("Personnes", "👥"),
    Profile("Profil", "🙂"),
}

/** Cadre de l'application : une barre d'onglets à gauche (Messages, Vidéos, Personnes, Profil) et l'écran actif. */
@Composable
fun NGomaShell(account: AccountBridge, messaging: MessagingBridge, video: VideoBridge, onLogout: () -> Unit) {
    var tab by remember { mutableStateOf(Tab.Messages) }
    // Demande d'ouverture d'une conversation avec une personne (bouton « Écrire un message » de l'écran Personnes)
    var openChatWith by remember { mutableStateOf<String?>(null) }

    Row(Modifier.fillMaxSize().background(NColors.Bg)) {
        Column(
            Modifier.width(84.dp).fillMaxHeight().background(MaterialTheme.colors.surface).padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Tab.values().forEach { t ->
                val on = t == tab
                Column(
                    Modifier.width(72.dp).clip(RoundedCornerShape(12.dp))
                        .background(if (on) NColors.Brand50 else androidx.compose.ui.graphics.Color.Transparent)
                        .clickable { tab = t }
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(t.icon, fontSize = 22.sp)
                    Text(t.label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = if (on) NColors.Brand else NColors.Secondary)
                }
            }
        }
        Box(Modifier.weight(1f).fillMaxHeight()) {
            when (tab) {
                Tab.Messages -> HomeScreen(account, messaging, onLogout, openChatWith) { openChatWith = null }
                Tab.Videos -> VideosScreen(account, video)
                Tab.People -> PeopleScreen(account, video) { name -> openChatWith = name; tab = Tab.Messages }
                Tab.Profile -> ProfileScreen(account, video, onLogout)
            }
        }
    }
}
