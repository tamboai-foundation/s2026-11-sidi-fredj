package ai.tambo.foundation.ngoma.ui

import ai.tambo.foundation.ngoma.bridge.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date

private enum class Phase { Splash, Auth, Home }

@Composable
fun NGomaApp(account: AccountBridge, messaging: MessagingBridge, video: VideoBridge) {
    NGomaTheme {
        var phase by remember { mutableStateOf(Phase.Splash) }
        LaunchedEffect(Unit) {
            phase = if (account.restoreSession() != null) Phase.Home else Phase.Auth
        }
        when (phase) {
            Phase.Splash -> SplashScreen()
            Phase.Auth -> AuthScreen(account) { phase = Phase.Home }
            Phase.Home -> NGomaShell(account, messaging, video) { phase = Phase.Auth }
        }
    }
}

@Composable
fun SplashScreen() {
    Box(Modifier.fillMaxSize().background(NColors.Brand), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("NGoma", color = Color.White, fontSize = 44.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(24.dp))
            CircularProgressIndicator(color = Color.White, strokeWidth = 3.dp)
            Spacer(Modifier.height(12.dp))
            Text("Chargement…", color = Color.White.copy(alpha = 0.85f))
        }
    }
}

@Composable
fun AuthScreen(account: AccountBridge, onDone: () -> Unit) {
    val scope = rememberCoroutineScope()
    var register by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var id by remember { mutableStateOf("") }
    var pwd by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize().background(NColors.Bg), contentAlignment = Alignment.Center) {
        Card(Modifier.width(400.dp), shape = RoundedCornerShape(20.dp), elevation = 4.dp) {
            Column(Modifier.padding(32.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("NGoma", color = NColors.Brand, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                Text(if (register) "Créer un compte" else "Connexion", color = NColors.Secondary)
                if (register) {
                    OutlinedTextField(name, { name = it }, label = { Text("Nom") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                }
                OutlinedTextField(id, { id = it }, label = { Text("Téléphone ou e-mail") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(
                    pwd, { pwd = it }, label = { Text("Mot de passe") }, singleLine = true,
                    visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(),
                )
                Button(
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    onClick = {
                        busy = true
                        scope.launch {
                            if (register) account.register(name, id, pwd) else account.login(id, pwd)
                            busy = false
                            onDone()
                        }
                    },
                ) { Text(if (register) "S'inscrire" else "Se connecter") }
                TextButton(onClick = { register = !register }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Text(if (register) "J'ai déjà un compte" else "Créer un compte")
                }
                Text("Mode démo : tout identifiant est accepté.", color = NColors.Grey, fontSize = 12.sp)
            }
        }
    }
}

private enum class Filter(val label: String) { All("Toutes"), Unread("Non lues"), Pinned("Épinglées"), Archived("Archivées") }

@Composable
fun HomeScreen(
    account: AccountBridge, messaging: MessagingBridge, onLogout: () -> Unit,
    openChatWith: String? = null, onChatOpened: () -> Unit = {},
) {
    val scope = rememberCoroutineScope()
    val user by account.user.collectAsState()
    val convs by messaging.conversations.collectAsState()
    var selected by remember { mutableStateOf<Long?>(null) }
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf(Filter.All) }
    var newDialog by remember { mutableStateOf(false) }

    // « Écrire un message » depuis l'écran Personnes : ouvre la conversation de cette personne (la crée si besoin).
    LaunchedEffect(openChatWith) {
        if (openChatWith != null) {
            selected = convs.firstOrNull { it.title == openChatWith }?.id ?: messaging.createConversation(openChatWith)
            onChatOpened()
        }
    }

    val shown = convs
        .filter { c ->
            when (filter) {
                Filter.All -> !c.archived
                Filter.Unread -> !c.archived && c.unread > 0
                Filter.Pinned -> !c.archived && c.pinned
                Filter.Archived -> c.archived
            }
        }
        .filter { it.title.contains(query, ignoreCase = true) }
        .sortedByDescending { it.pinned }

    Row(Modifier.fillMaxSize().background(NColors.Bg)) {
        Column(Modifier.width(360.dp).fillMaxHeight().background(MaterialTheme.colors.surface)) {
            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Avatar(user?.name ?: "?", 36)
                Spacer(Modifier.width(10.dp))
                Text("Messages", fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                TextButton(onClick = { newDialog = true }) { Text("+ Nouveau") }
                TextButton(onClick = { scope.launch { account.logout(); onLogout() } }) { Text("Quitter") }
            }
            OutlinedTextField(
                query, { query = it }, placeholder = { Text("Rechercher") }, singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            )
            Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Filter.values().forEach { f ->
                    val on = f == filter
                    Text(
                        f.label,
                        color = if (on) Color.White else NColors.Secondary,
                        fontSize = 13.sp,
                        modifier = Modifier.clip(RoundedCornerShape(50))
                            .background(if (on) NColors.Brand else NColors.Muted)
                            .clickable { filter = f }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                }
            }
            Divider(color = NColors.Hairline)
            LazyColumn(Modifier.weight(1f)) {
                items(shown, key = { it.id }) { c ->
                    ConversationRow(c, messaging, c.id == selected) {
                        selected = c.id
                        messaging.markRead(c.id)
                    }
                }
            }
        }
        Box(Modifier.weight(1f).fillMaxHeight()) {
            val id = selected
            val conv = convs.firstOrNull { it.id == id }
            if (conv == null) {
                Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("NGoma", color = NColors.Brand, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    Text("Sélectionnez une conversation", color = NColors.Secondary)
                }
            } else {
                ConversationScreen(conv, messaging)
            }
        }
    }

    if (newDialog) {
        var title by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { newDialog = false },
            title = { Text("Nouvelle conversation") },
            text = { OutlinedTextField(title, { title = it }, label = { Text("Nom") }, singleLine = true) },
            confirmButton = {
                TextButton(onClick = {
                    if (title.isNotBlank()) selected = messaging.createConversation(title.trim())
                    newDialog = false
                }) { Text("Créer") }
            },
            dismissButton = { TextButton(onClick = { newDialog = false }) { Text("Annuler") } },
        )
    }
}

@Composable
internal fun Avatar(name: String, size: Int) {
    Box(
        Modifier.size(size.dp).clip(CircleShape).background(NColors.Brand100),
        contentAlignment = Alignment.Center,
    ) {
        Text(name.take(1).uppercase(), color = NColors.Brand700, fontWeight = FontWeight.Bold, fontSize = (size / 2.4).sp)
    }
}

internal fun hour(t: Long): String = SimpleDateFormat("HH:mm").format(Date(t))

@Composable
private fun ConversationRow(c: Conversation, messaging: MessagingBridge, selected: Boolean, onClick: () -> Unit) {
    val last = messaging.lastMessage(c.id)
    Row(
        Modifier.fillMaxWidth()
            .background(if (selected) NColors.Brand50 else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Avatar(c.title, 46)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row {
                Text(
                    (if (c.pinned) "📌 " else "") + c.title,
                    fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (last != null) Text(hour(last.time), color = NColors.Grey, fontSize = 12.sp)
            }
            Text(
                last?.text ?: "Aucun message",
                color = NColors.Secondary, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
        if (c.muted) Text("🔕", modifier = Modifier.padding(start = 6.dp))
        if (c.unread > 0) {
            Spacer(Modifier.width(8.dp))
            Box(Modifier.clip(CircleShape).background(NColors.Brand).padding(horizontal = 7.dp, vertical = 2.dp)) {
                Text("${c.unread}", color = Color.White, fontSize = 12.sp)
            }
        }
    }
    Divider(color = NColors.Hairline)
}

@Composable
fun ConversationScreen(conv: Conversation, messaging: MessagingBridge) {
    val messages by messaging.messages(conv.id).collectAsState()
    var input by remember(conv.id) { mutableStateOf("") }
    var replyTo by remember(conv.id) { mutableStateOf<Message?>(null) }
    var editing by remember(conv.id) { mutableStateOf<Message?>(null) }
    var menuFor by remember(conv.id) { mutableStateOf<Long?>(null) }
    var headerMenu by remember(conv.id) { mutableStateOf(false) }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size, conv.id) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    fun submit() {
        val text = input.trim()
        if (text.isEmpty()) return
        val e = editing
        if (e != null) messaging.edit(e.id, text) else messaging.send(conv.id, text, replyTo?.id)
        input = ""; replyTo = null; editing = null
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().background(MaterialTheme.colors.surface).padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Avatar(conv.title, 40)
            Spacer(Modifier.width(12.dp))
            Text(conv.title, fontWeight = FontWeight.Bold, fontSize = 17.sp, modifier = Modifier.weight(1f))
            Box {
                TextButton(onClick = { headerMenu = true }) { Text("⋮", fontSize = 20.sp) }
                DropdownMenu(headerMenu, { headerMenu = false }) {
                    DropdownMenuItem({ messaging.setPinned(conv.id, !conv.pinned); headerMenu = false }) {
                        Text(if (conv.pinned) "Désépingler" else "Épingler")
                    }
                    DropdownMenuItem({ messaging.setMuted(conv.id, !conv.muted); headerMenu = false }) {
                        Text(if (conv.muted) "Réactiver les notifications" else "Mettre en sourdine")
                    }
                    DropdownMenuItem({ messaging.setArchived(conv.id, !conv.archived); headerMenu = false }) {
                        Text(if (conv.archived) "Désarchiver" else "Archiver")
                    }
                }
            }
        }
        Divider(color = NColors.Hairline)

        LazyColumn(
            Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp),
            state = listState,
            verticalArrangement = Arrangement.spacedBy(6.dp),
            contentPadding = PaddingValues(vertical = 12.dp),
        ) {
            items(messages, key = { it.id }) { m ->
                val mine = m.authorId == 1
                val quoted = m.replyToId?.let { rid -> messages.firstOrNull { it.id == rid } }
                Box(Modifier.fillMaxWidth(), contentAlignment = if (mine) Alignment.CenterEnd else Alignment.CenterStart) {
                    Column(horizontalAlignment = if (mine) Alignment.End else Alignment.Start) {
                        Column(
                            Modifier.widthIn(max = 460.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (mine) NColors.Brand else MaterialTheme.colors.surface)
                                .clickable(enabled = !m.deleted) { menuFor = m.id }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                        ) {
                            val fg = if (mine) Color.White else MaterialTheme.colors.onSurface
                            if (quoted != null) {
                                Text(
                                    quoted.text.ifEmpty { "Message supprimé" },
                                    color = fg.copy(alpha = 0.75f), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(bottom = 4.dp),
                                )
                            }
                            if (m.deleted) {
                                Text("Message supprimé", color = fg.copy(alpha = 0.6f), fontSize = 14.sp)
                            } else {
                                Text(m.text, color = fg)
                            }
                            Text(
                                hour(m.time) + if (m.edited) " · modifié" else "",
                                color = fg.copy(alpha = 0.65f), fontSize = 11.sp,
                                modifier = Modifier.align(Alignment.End),
                            )
                        }
                        if (m.reactions.isNotEmpty()) {
                            Text(
                                m.reactions.entries.joinToString("  ") { "${it.key} ${it.value}" },
                                fontSize = 12.sp,
                                modifier = Modifier.padding(top = 2.dp).clip(RoundedCornerShape(50))
                                    .background(NColors.Muted).padding(horizontal = 8.dp, vertical = 2.dp),
                            )
                        }
                        DropdownMenu(menuFor == m.id, { menuFor = null }) {
                            Row(Modifier.padding(horizontal = 8.dp)) {
                                listOf("👍", "❤️", "😂", "😮").forEach { e ->
                                    TextButton(onClick = { messaging.react(m.id, e); menuFor = null }) { Text(e) }
                                }
                            }
                            DropdownMenuItem({ replyTo = m; editing = null; menuFor = null }) { Text("Répondre") }
                            if (mine) {
                                DropdownMenuItem({ editing = m; replyTo = null; input = m.text; menuFor = null }) { Text("Modifier") }
                                DropdownMenuItem({ messaging.delete(m.id); menuFor = null }) { Text("Supprimer") }
                            }
                        }
                    }
                }
            }
        }

        val banner = editing?.let { "Modification du message" } ?: replyTo?.let { "Réponse à : ${it.text}" }
        if (banner != null) {
            Row(
                Modifier.fillMaxWidth().background(NColors.Brand50).padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(banner, color = NColors.Brand700, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                TextButton(onClick = { editing = null; replyTo = null; input = "" }) { Text("✕") }
            }
        }
        Row(
            Modifier.fillMaxWidth().background(MaterialTheme.colors.surface).padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                input, { input = it }, placeholder = { Text("Message") },
                modifier = Modifier.weight(1f), shape = RoundedCornerShape(24.dp),
            )
            Spacer(Modifier.width(8.dp))
            Button(onClick = ::submit, shape = CircleShape, modifier = Modifier.size(48.dp), contentPadding = PaddingValues(0.dp)) {
                Text(if (editing != null) "✓" else "➤")
            }
        }
    }
}
