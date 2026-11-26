package ai.tambo.foundation.ngoma.ui

import ai.tambo.foundation.ngoma.bridge.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.loadImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.awt.FileDialog
import java.awt.Frame
import java.io.File

private val gradients = listOf(
    Color(0xFFF43F5E) to Color(0xFFFB923C),
    Color(0xFF4F46E5) to Color(0xFF38BDF8),
    Color(0xFF059669) to Color(0xFFA3E635),
    Color(0xFFC026D3) to Color(0xFFF472B6),
    Color(0xFF1E293B) to Color(0xFF64748B),
)

internal fun timeAgo(t: Long): String {
    val s = maxOf(1L, (System.currentTimeMillis() - t) / 1000)
    return when {
        s < 60 -> "à l'instant"
        s < 3600 -> "il y a ${s / 60} min"
        s < 86400 -> "il y a ${s / 3600} h"
        else -> "il y a ${s / 86400} j"
    }
}

private fun gradientOf(post: Post) = gradients[(post.id % gradients.size).toInt()]

/** Vignette d'une publication (profils, liste de mes vidéos). */
@Composable
internal fun PostMiniature(post: Post, modifier: Modifier = Modifier) {
    val (a, b) = gradientOf(post)
    Box(modifier.height(190.dp).clip(RoundedCornerShape(16.dp)).background(Brush.linearGradient(listOf(a, b)))) {
        Column(Modifier.align(Alignment.BottomStart).padding(10.dp)) {
            Text(post.caption.ifBlank { "Vidéo" }, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text("❤️ ${post.likes.size}", color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp)
        }
    }
}

/**
 * Fil de vidéos façon TikTok (M1 SI, module 3) : défilement vertical page par page, publier, aimer, commenter,
 * modifier et supprimer ses vidéos. La lecture vidéo n'est pas incluse (aucun lecteur dans Compose Desktop) :
 * à ajouter avec la bibliothèque de votre choix (vlcj, JavaFX Media…) sur `Post.mediaPath` (les photos sont affichées).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VideosScreen(account: AccountBridge, video: VideoBridge) {
    val posts by video.posts.collectAsState()
    val users by account.users.collectAsState()
    var mineOnly by remember { mutableStateOf(false) }
    var commentsFor by remember { mutableStateOf<Long?>(null) }
    var publishing by remember { mutableStateOf(false) }
    val shown = if (mineOnly) posts.filter { it.authorId == ME } else posts
    val listState = rememberLazyListState()

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        if (shown.isEmpty()) {
            Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(if (mineOnly) "Vous n'avez rien publié pour l'instant." else "Aucune publication pour le moment.", color = Color.White.copy(alpha = 0.8f))
                Button(onClick = { publishing = true }, shape = RoundedCornerShape(50)) { Text("Publier") }
            }
        } else {
            LazyColumn(Modifier.fillMaxSize(), state = listState, flingBehavior = rememberSnapFlingBehavior(listState)) {
                items(shown, key = { it.id }) { post ->
                    PostPage(
                        post, users.firstOrNull { it.id == post.authorId }?.name ?: "Utilisateur", video,
                        onComments = { commentsFor = post.id },
                        modifier = Modifier.fillParentMaxHeight().fillMaxWidth(),
                    )
                }
            }
        }

        Row(
            Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.5f), Color.Transparent))).padding(12.dp),
            horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Pour vous", color = Color.White.copy(alpha = if (mineOnly) 0.6f else 1f), fontWeight = FontWeight.Bold, modifier = Modifier.clickable { mineOnly = false })
            Spacer(Modifier.width(20.dp))
            Text("Mes vidéos", color = Color.White.copy(alpha = if (mineOnly) 1f else 0.6f), fontWeight = FontWeight.Bold, modifier = Modifier.clickable { mineOnly = true })
            Spacer(Modifier.weight(1f))
            Button(onClick = { publishing = true }, shape = RoundedCornerShape(50), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)) { Text("+ Publier") }
        }

        val open = commentsFor
        if (open != null) {
            Row(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)).clickable { commentsFor = null }) {
                Spacer(Modifier.weight(1f))
                Box(Modifier.width(420.dp).fillMaxHeight().background(MaterialTheme.colors.surface).clickable(enabled = false) {}) {
                    CommentsPanel(open, account, video) { commentsFor = null }
                }
            }
        }
    }

    if (publishing) PublishDialog(onDismiss = { publishing = false }) { caption, path ->
        video.publish(caption, path)
        publishing = false
        mineOnly = false
    }
}

@Composable
private fun PostPage(post: Post, author: String, video: VideoBridge, onComments: () -> Unit, modifier: Modifier) {
    val comments by video.comments(post.id).collectAsState()
    var menu by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf(false) }
    val (a, b) = gradientOf(post)
    val liked = ME in post.likes

    Box(modifier.background(Brush.linearGradient(listOf(a, b)))) {
        Column(Modifier.align(Alignment.Center).padding(horizontal = 80.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            if (post.mediaPath.isNotBlank()) {
                val image = remember(post.mediaPath) { loadPhoto(post.mediaPath) }
                if (image != null) {
                    Image(image, null, Modifier.heightIn(max = 480.dp), contentScale = ContentScale.Fit)
                } else {
                    Text("🎬 ${File(post.mediaPath).name}", color = Color.White, fontSize = 16.sp)
                }
                Spacer(Modifier.height(8.dp))
            }
            Text(post.caption, color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
        }
        Column(
            Modifier.align(Alignment.BottomStart).fillMaxWidth().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f)))).padding(start = 20.dp, end = 100.dp, top = 24.dp, bottom = 24.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Avatar(author, 32)
                Spacer(Modifier.width(8.dp))
                Text(author, color = Color.White, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(8.dp))
                Text(timeAgo(post.time) + if (post.edited) " · modifié" else "", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
            }
        }
        Column(Modifier.align(Alignment.CenterEnd).padding(end = 16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Column(Modifier.clickable { video.toggleLike(post.id) }, horizontalAlignment = Alignment.CenterHorizontally) {
                Text(if (liked) "❤️" else "🤍", fontSize = 30.sp)
                Text("${post.likes.size}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
            Column(Modifier.clickable(onClick = onComments), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("💬", fontSize = 30.sp)
                Text("${comments.count { !it.deleted }}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
            if (post.authorId == ME) {
                Box {
                    Text("⋮", color = Color.White, fontSize = 30.sp, modifier = Modifier.clickable { menu = true })
                    DropdownMenu(menu, { menu = false }) {
                        DropdownMenuItem({ menu = false; editing = true }) { Text("Modifier la légende") }
                        DropdownMenuItem({ menu = false; video.removePost(post.id) }) { Text("Supprimer la vidéo", color = NColors.Negative) }
                    }
                }
            }
        }
    }

    if (editing) {
        var caption by remember { mutableStateOf(post.caption) }
        AlertDialog(
            onDismissRequest = { editing = false },
            title = { Text("Modifier la légende") },
            text = { OutlinedTextField(caption, { caption = it }, modifier = Modifier.fillMaxWidth()) },
            confirmButton = { TextButton(onClick = { video.editPost(post.id, caption); editing = false }) { Text("Enregistrer") } },
            dismissButton = { TextButton(onClick = { editing = false }) { Text("Annuler") } },
        )
    }
}

/** Commentaires et réponses d'une publication : ajouter, répondre, aimer, modifier et supprimer les siens. */
@Composable
private fun CommentsPanel(postId: Long, account: AccountBridge, video: VideoBridge, onClose: () -> Unit) {
    val all by video.comments(postId).collectAsState()
    val users by account.users.collectAsState()
    var text by remember { mutableStateOf("") }
    var replyTo by remember { mutableStateOf<PostComment?>(null) }
    var editing by remember { mutableStateOf<PostComment?>(null) }
    fun name(id: Int) = users.firstOrNull { it.id == id }?.name ?: "Utilisateur"
    val visible = all.filter { !it.deleted || all.any { r -> r.parentId == it.id && !r.deleted } }
    val roots = visible.filter { it.parentId == null }

    fun submit() {
        val value = text.trim()
        if (value.isEmpty()) return
        val e = editing
        if (e != null) video.editComment(e.id, value) else video.addComment(postId, value, replyTo?.let { it.parentId ?: it.id })
        text = ""; replyTo = null; editing = null
    }

    @Composable
    fun CommentLine(c: PostComment) {
        Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
            Avatar(name(c.authorId), 32)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Row {
                    Text(name(c.authorId), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(Modifier.width(6.dp))
                    Text(timeAgo(c.time) + if (c.edited) " · modifié" else "", color = NColors.Secondary, fontSize = 12.sp)
                }
                if (c.deleted) {
                    Text("Commentaire supprimé", color = NColors.Secondary, fontSize = 14.sp)
                } else {
                    Text(c.text, fontSize = 14.sp)
                    Row(Modifier.padding(top = 2.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text((if (ME in c.likes) "❤️" else "🤍") + " ${c.likes.size}", fontSize = 12.sp, color = NColors.Secondary, modifier = Modifier.clickable { video.toggleCommentLike(c.id) })
                        Text("Répondre", fontSize = 12.sp, color = NColors.Secondary, modifier = Modifier.clickable { replyTo = c; editing = null })
                        if (c.authorId == ME) {
                            Text("Modifier", fontSize = 12.sp, color = NColors.Secondary, modifier = Modifier.clickable { editing = c; replyTo = null; text = c.text })
                            Text("Supprimer", fontSize = 12.sp, color = NColors.Negative, modifier = Modifier.clickable { video.removeComment(c.id) })
                        }
                    }
                }
            }
        }
    }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Commentaires (${all.count { !it.deleted }})", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            TextButton(onClick = onClose) { Text("✕") }
        }
        Divider(color = NColors.Hairline)
        LazyColumn(Modifier.weight(1f).padding(horizontal = 16.dp)) {
            if (roots.isEmpty()) item { Text("Aucun commentaire. Soyez le premier !", color = NColors.Secondary, modifier = Modifier.padding(vertical = 12.dp)) }
            items(roots, key = { it.id }) { c ->
                Column {
                    CommentLine(c)
                    visible.filter { it.parentId == c.id }.forEach { r -> Box(Modifier.padding(start = 42.dp)) { CommentLine(r) } }
                }
            }
        }
        Divider(color = NColors.Hairline)
        val banner = editing?.let { "Modification du commentaire" } ?: replyTo?.let { "Réponse à ${name(it.authorId)}" }
        if (banner != null) {
            Row(Modifier.fillMaxWidth().background(NColors.Brand50).padding(horizontal = 16.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(banner, color = NColors.Brand700, fontSize = 12.sp, modifier = Modifier.weight(1f))
                TextButton(onClick = { replyTo = null; editing = null; text = "" }) { Text("Annuler") }
            }
        }
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(text, { text = it }, placeholder = { Text("Ajouter un commentaire…") }, singleLine = true, shape = RoundedCornerShape(24.dp), modifier = Modifier.weight(1f))
            Spacer(Modifier.width(8.dp))
            Button(onClick = ::submit, enabled = text.isNotBlank(), shape = CircleShape, modifier = Modifier.size(48.dp), contentPadding = PaddingValues(0.dp)) {
                Text(if (editing != null) "✓" else "➤")
            }
        }
    }
}

/** Photo décodée si le fichier est une image (null pour une vidéo ou un fichier illisible). */
private fun loadPhoto(path: String): ImageBitmap? {
    if (!Regex("(?i).*\\.(jpe?g|png|gif|webp|bmp)$").matches(path)) return null
    return runCatching { File(path).inputStream().use { loadImageBitmap(it) } }.getOrNull()
}

/** Publier : photo ou vidéo (facultatif) et/ou légende ; une légende seule est acceptée. */
@Composable
private fun PublishDialog(onDismiss: () -> Unit, onPublish: (caption: String, path: String?) -> Unit) {
    var caption by remember { mutableStateOf("") }
    var path by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Publier") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = {
                    val dialog = FileDialog(null as Frame?, "Choisir une photo ou une vidéo", FileDialog.LOAD)
                    dialog.isVisible = true
                    if (dialog.file != null) path = File(dialog.directory, dialog.file).absolutePath
                }) { Text(path?.let { "🎬 " + File(it).name } ?: "Ajouter une photo ou une vidéo (facultatif)") }
                OutlinedTextField(caption, { caption = it }, label = { Text("Légende") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = { TextButton(enabled = path != null || caption.isNotBlank(), onClick = { onPublish(caption, path) }) { Text("Publier") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } },
    )
}
