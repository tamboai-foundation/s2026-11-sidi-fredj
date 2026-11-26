package ai.tambo.foundation.ngoma.ui

import ai.tambo.foundation.ngoma.bridge.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

/** Mon profil (M1 SI, module 1) : modifier mon nom et ma description, voir mes vidéos, me déconnecter, supprimer mon compte. */
@Composable
fun ProfileScreen(account: AccountBridge, video: VideoBridge, onLogout: () -> Unit) {
    val scope = rememberCoroutineScope()
    val me by account.user.collectAsState()
    val posts by video.posts.collectAsState()
    val mine = posts.filter { it.authorId == ME }
    var name by remember(me?.name) { mutableStateOf(me?.name ?: "") }
    var bio by remember(me?.bio) { mutableStateOf(me?.bio ?: "") }
    var saved by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize().background(NColors.Bg), contentAlignment = Alignment.TopCenter) {
        Column(Modifier.widthIn(max = 560.dp).fillMaxHeight().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Card(shape = RoundedCornerShape(20.dp), elevation = 2.dp) {
                Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Avatar(me?.name ?: "?", 72)
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Text(me?.name ?: "", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                            Text("${mine.size} vidéo${if (mine.size > 1) "s" else ""} publiée${if (mine.size > 1) "s" else ""}", color = NColors.Secondary, fontSize = 13.sp)
                        }
                    }
                    OutlinedTextField(name, { name = it }, label = { Text("Nom") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(bio, { bio = it }, label = { Text("Description") }, minLines = 3, modifier = Modifier.fillMaxWidth())
                    Button(
                        onClick = { account.updateProfile(name, bio); saved = true },
                        modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(12.dp),
                    ) { Text(if (saved) "Enregistré ✓" else "Enregistrer") }
                }
            }
            Card(shape = RoundedCornerShape(20.dp), elevation = 2.dp) {
                Column(Modifier.padding(24.dp).fillMaxWidth()) {
                    Text("Mes vidéos", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(10.dp))
                    if (mine.isEmpty()) {
                        Text("Vous n'avez rien publié pour l'instant.", color = NColors.Secondary)
                    } else {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            mine.take(3).forEach { PostMiniature(it, Modifier.weight(1f)) }
                            repeat(3 - mine.take(3).size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
            }
            Card(shape = RoundedCornerShape(20.dp), elevation = 2.dp) {
                Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = { scope.launch { account.logout(); onLogout() } }, modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(12.dp)) {
                        Text("Se déconnecter")
                    }
                    OutlinedButton(onClick = { confirmDelete = true }, modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(12.dp)) {
                        Text("Supprimer mon compte", color = NColors.Negative)
                    }
                }
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Supprimer le compte ?") },
            text = { Text("Cette action est définitive.") },
            confirmButton = { TextButton(onClick = { confirmDelete = false; scope.launch { account.deleteAccount(); onLogout() } }) { Text("Supprimer", color = NColors.Negative) } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Annuler") } },
        )
    }
}
