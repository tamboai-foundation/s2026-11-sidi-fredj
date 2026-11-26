package ai.tambo.foundation.ngoma.ui

import ai.tambo.foundation.ngoma.bridge.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date

/** Gestion des utilisateurs (M1 SI, module 1) : voir les utilisateurs, rechercher, ouvrir un profil, écrire à quelqu'un. */
@Composable
fun PeopleScreen(account: AccountBridge, video: VideoBridge, onWrite: (String) -> Unit) {
    val users by account.users.collectAsState()
    var query by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf<Int?>(null) }
    val shown = users.filter { query.isBlank() || it.name.contains(query, ignoreCase = true) || it.bio.contains(query, ignoreCase = true) }
    val current = users.firstOrNull { it.id == selected }

    Row(Modifier.fillMaxSize().background(NColors.Bg)) {
        Column(Modifier.width(360.dp).fillMaxHeight().background(MaterialTheme.colors.surface)) {
            Column(Modifier.padding(16.dp)) {
                Text("Personnes", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(query, { query = it }, placeholder = { Text("Rechercher un utilisateur") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Text("${shown.size} utilisateur${if (shown.size > 1) "s" else ""}", color = NColors.Secondary, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
            }
            Divider(color = NColors.Hairline)
            LazyColumn(Modifier.weight(1f)) {
                if (shown.isEmpty()) item { Text("Aucun utilisateur ne correspond.", color = NColors.Secondary, modifier = Modifier.padding(16.dp)) }
                items(shown, key = { it.id }) { u ->
                    Row(
                        Modifier.fillMaxWidth().background(if (u.id == selected) NColors.Brand50 else Color.Transparent)
                            .clickable { selected = u.id }.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Avatar(u.name, 44)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(u.name + if (u.id == ME) " (vous)" else "", fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(u.bio.ifBlank { "Pas de description" }, color = NColors.Secondary, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    Divider(color = NColors.Hairline)
                }
            }
        }
        Box(Modifier.weight(1f).fillMaxHeight()) {
            if (current == null) {
                Text("Sélectionnez une personne", color = NColors.Secondary, modifier = Modifier.align(Alignment.Center))
            } else {
                UserDetail(current, video, onWrite)
            }
        }
    }
}

@Composable
private fun UserDetail(user: User, video: VideoBridge, onWrite: (String) -> Unit) {
    val posts by video.posts.collectAsState()
    val mine = posts.filter { it.authorId == user.id }
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxWidth().background(MaterialTheme.colors.surface).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Avatar(user.name, 84)
            Text(user.name, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
            Text(user.bio.ifBlank { "Pas de description" }, color = NColors.Secondary, textAlign = TextAlign.Center)
            if (user.joinedAt > 0) Text("Membre depuis le " + SimpleDateFormat("dd/MM/yyyy").format(Date(user.joinedAt)), color = NColors.Grey, fontSize = 12.sp)
            if (user.id != ME) Button(onClick = { onWrite(user.name) }, shape = RoundedCornerShape(12.dp)) { Text("Écrire un message") }
        }
        Divider(color = NColors.Hairline)
        Column(Modifier.padding(16.dp)) {
            Text("Vidéos (${mine.size})", fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            if (mine.isEmpty()) {
                Text("Aucune vidéo publiée.", color = NColors.Secondary)
            } else {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    mine.take(4).forEach { PostMiniature(it, Modifier.weight(1f)) }
                    repeat(4 - mine.take(4).size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}
