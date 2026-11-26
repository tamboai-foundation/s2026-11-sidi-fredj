package ai.tambo.foundation.ngoma.bridge

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class User(val id: Int, val name: String, val bio: String = "", val joinedAt: Long = 0L)

data class Message(
    val id: Long,
    val conversationId: Long,
    val authorId: Int,
    val text: String,
    val time: Long,
    val replyToId: Long? = null,
    val edited: Boolean = false,
    val deleted: Boolean = false,
    val reactions: Map<String, Int> = emptyMap(),
)

data class Conversation(
    val id: Long,
    val title: String,
    val unread: Int = 0,
    val pinned: Boolean = false,
    val muted: Boolean = false,
    val archived: Boolean = false,
)

/** Interface UI <-> TModeler pour le compte. Remplacer le mock par une implémentation TModeler. */
interface AccountBridge {
    val user: StateFlow<User?>
    suspend fun restoreSession(): User?
    suspend fun login(identifier: String, password: String): User
    suspend fun register(name: String, identifier: String, password: String): User
    suspend fun logout()

    // Gestion des utilisateurs (M1 SI, module 1) : voir les users, rechercher, modifier son profil, supprimer son compte.
    val users: StateFlow<List<User>>
    fun updateProfile(name: String, bio: String)
    suspend fun deleteAccount()
}

/** Publication (M1 SI, module 3) : média facultatif (photo ou vidéo) et légende. `mediaPath` vide : légende seule, affichée sur une carte colorée. */
data class Post(
    val id: Long,
    val authorId: Int,
    val caption: String,
    val mediaPath: String,
    val time: Long,
    val edited: Boolean = false,
    /** Identifiants des utilisateurs qui ont aimé la publication. */
    val likes: Set<Int> = emptySet(),
)

data class PostComment(
    val id: Long,
    val postId: Long,
    val authorId: Int,
    val text: String,
    val time: Long,
    /** Réponse à un commentaire : identifiant du commentaire parent (un seul niveau dans l'interface). */
    val parentId: Long? = null,
    val edited: Boolean = false,
    val deleted: Boolean = false,
    val likes: Set<Int> = emptySet(),
)

/** Interface UI <-> TModeler pour les vidéos, likes, commentaires et réponses. Remplacer le mock par une implémentation TModeler. */
interface VideoBridge {
    /** Fil d'actualité : toutes les publications, la plus récente en premier. */
    val posts: StateFlow<List<Post>>
    fun comments(postId: Long): StateFlow<List<PostComment>>
    fun publish(caption: String, mediaPath: String?): Post
    fun editPost(postId: Long, caption: String)
    fun removePost(postId: Long)
    /** Aime ou n'aime plus (bascule) pour l'utilisateur connecté. */
    fun toggleLike(postId: Long)
    fun addComment(postId: Long, text: String, parentId: Long? = null)
    fun editComment(commentId: Long, text: String)
    fun removeComment(commentId: Long)
    fun toggleCommentLike(commentId: Long)
}

/** Interface UI <-> TModeler pour la messagerie. Remplacer le mock par une implémentation TModeler. */
interface MessagingBridge {
    val conversations: StateFlow<List<Conversation>>
    fun messages(conversationId: Long): StateFlow<List<Message>>
    fun lastMessage(conversationId: Long): Message?
    fun send(conversationId: Long, text: String, replyToId: Long? = null)
    fun edit(messageId: Long, text: String)
    fun delete(messageId: Long)
    fun react(messageId: Long, emoji: String)
    fun markRead(conversationId: Long)
    fun setPinned(conversationId: Long, value: Boolean)
    fun setMuted(conversationId: Long, value: Boolean)
    fun setArchived(conversationId: Long, value: Boolean)
    fun createConversation(title: String): Long
}

class AccountBridgeMock : AccountBridge {
    private val state = MutableStateFlow<User?>(null)
    override val user: StateFlow<User?> = state

    private val others = listOf(
        User(2, "Awa Traoré", "Développeuse mobile, Bamako.", System.currentTimeMillis() - 30 * 86_400_000L),
        User(3, "Kofi Mensah", "Design et vidéo, Accra.", System.currentTimeMillis() - 20 * 86_400_000L),
        User(4, "Fatou Diop", "Étudiante en informatique, Dakar.", System.currentTimeMillis() - 12 * 86_400_000L),
        User(5, "Moussa Keita", "Cybersécurité.", System.currentTimeMillis() - 5 * 86_400_000L),
        User(6, "Aïcha Bello", "Passionnée de musique et de code.", System.currentTimeMillis() - 2 * 86_400_000L),
    )
    private val all = MutableStateFlow(others)
    override val users: StateFlow<List<User>> = all

    private fun open(user: User): User {
        state.value = user
        all.value = listOf(user) + others
        return user
    }

    override suspend fun restoreSession(): User? {
        delay(1200)
        return state.value
    }

    override suspend fun login(identifier: String, password: String): User {
        delay(500)
        return open(User(1, identifier.ifBlank { "Moi" }, "", System.currentTimeMillis()))
    }

    override suspend fun register(name: String, identifier: String, password: String): User {
        delay(500)
        return open(User(1, name.ifBlank { "Moi" }, "", System.currentTimeMillis()))
    }

    override suspend fun logout() {
        state.value = null
        all.value = others
    }

    override fun updateProfile(name: String, bio: String) {
        val me = state.value ?: return
        open(me.copy(name = name.trim().ifBlank { me.name }, bio = bio.trim()))
    }

    override suspend fun deleteAccount() = logout()
}

class VideoBridgeMock : VideoBridge {
    private val me = 1
    private var nextId = 1000L
    private val feed = MutableStateFlow<List<Post>>(emptyList())
    override val posts: StateFlow<List<Post>> = feed
    private val notes = MutableStateFlow<List<PostComment>>(emptyList())
    private val perPost = HashMap<Long, MutableStateFlow<List<PostComment>>>()

    init {
        val now = System.currentTimeMillis()
        fun post(author: Int, caption: String, ago: Long, likes: Set<Int>) =
            Post(nextId++, author, caption, "", now - ago, likes = likes)
        feed.value = listOf(
            post(2, "Ma première application de messagerie, étape par étape ✨", 3_600_000, setOf(3, 4, 5)),
            post(3, "Un petit tour dans les coulisses de notre équipe 🎬", 7_200_000, setOf(2)),
            post(4, "Astuce du jour : tester chaque écran avant de le montrer.", 20_000_000, setOf(2, 3, 5, 6)),
            post(5, "Pourquoi on chiffre les messages ? Explication en 30 secondes.", 90_000_000, emptySet()),
            post(6, "Bienvenue sur NGoma 👋", 180_000_000, setOf(2, 3)),
        )
        val first = feed.value.first().id
        val c1 = nextId++
        notes.value = listOf(
            PostComment(c1, first, 3, "Bravo, très clair !", now - 3_000_000),
            PostComment(nextId++, first, 4, "Merci pour le partage 🙏", now - 2_800_000),
            PostComment(nextId++, first, 2, "Merci Kofi !", now - 2_500_000, parentId = c1),
        )
    }

    private fun flowOf(postId: Long) = perPost.getOrPut(postId) { MutableStateFlow(emptyList()) }

    override fun comments(postId: Long): StateFlow<List<PostComment>> = flowOf(postId).also {
        it.value = notes.value.filter { c -> c.postId == postId }.sortedBy { c -> c.time }
    }

    private fun refresh(postId: Long) {
        flowOf(postId).value = notes.value.filter { it.postId == postId }.sortedBy { it.time }
    }

    override fun publish(caption: String, mediaPath: String?): Post {
        val post = Post(nextId++, me, caption.trim(), mediaPath ?: "", System.currentTimeMillis())
        feed.value = listOf(post) + feed.value
        return post
    }

    private fun patch(id: Long, f: (Post) -> Post) {
        feed.value = feed.value.map { if (it.id == id) f(it) else it }
    }

    override fun editPost(postId: Long, caption: String) = patch(postId) { it.copy(caption = caption.trim(), edited = true) }

    override fun removePost(postId: Long) {
        feed.value = feed.value.filter { it.id != postId }
        notes.value = notes.value.filter { it.postId != postId }
    }

    override fun toggleLike(postId: Long) = patch(postId) { it.copy(likes = if (me in it.likes) it.likes - me else it.likes + me) }

    override fun addComment(postId: Long, text: String, parentId: Long?) {
        notes.value = notes.value + PostComment(nextId++, postId, me, text.trim(), System.currentTimeMillis(), parentId)
        refresh(postId)
    }

    private fun patchNote(id: Long, f: (PostComment) -> PostComment) {
        val postId = notes.value.firstOrNull { it.id == id }?.postId ?: return
        notes.value = notes.value.map { if (it.id == id) f(it) else it }
        refresh(postId)
    }

    override fun editComment(commentId: Long, text: String) = patchNote(commentId) { it.copy(text = text.trim(), edited = true) }
    override fun removeComment(commentId: Long) = patchNote(commentId) { it.copy(deleted = true, text = "") }
    override fun toggleCommentLike(commentId: Long) =
        patchNote(commentId) { it.copy(likes = if (me in it.likes) it.likes - me else it.likes + me) }
}

class MessagingBridgeMock : MessagingBridge {
    private val me = 1
    private var nextId = 100L
    private val all = mutableListOf<Message>()
    private val perConversation = HashMap<Long, MutableStateFlow<List<Message>>>()
    private val convs = MutableStateFlow(
        listOf(
            Conversation(1, "Awa Traoré", unread = 2, pinned = true),
            Conversation(2, "Équipe NGoma", unread = 0),
            Conversation(3, "Kofi Mensah", unread = 1),
            Conversation(4, "Fatou Diop", unread = 0, muted = true),
        )
    )
    override val conversations: StateFlow<List<Conversation>> = convs

    init {
        val now = System.currentTimeMillis()
        seed(1, 2, "Salut ! Tu as vu le nouveau projet ?", now - 3_600_000)
        seed(1, 2, "On commence demain matin.", now - 3_500_000)
        seed(2, 3, "Bienvenue dans l'équipe NGoma.", now - 86_400_000)
        seed(2, me, "Merci, content d'être là !", now - 86_000_000)
        seed(3, 4, "Tu peux m'envoyer le rapport ?", now - 7_200_000)
        seed(4, 5, "Bonne journée à tous.", now - 172_800_000)
    }

    private fun seed(conv: Long, author: Int, text: String, time: Long) {
        all += Message(nextId++, conv, author, text, time)
        flow(conv).value = all.filter { it.conversationId == conv }
    }

    private fun flow(id: Long) = perConversation.getOrPut(id) { MutableStateFlow(emptyList()) }

    private fun publish(conv: Long) {
        flow(conv).value = all.filter { it.conversationId == conv }
    }

    override fun messages(conversationId: Long): StateFlow<List<Message>> = flow(conversationId)

    override fun lastMessage(conversationId: Long) =
        all.lastOrNull { it.conversationId == conversationId && !it.deleted }

    override fun send(conversationId: Long, text: String, replyToId: Long?) {
        all += Message(nextId++, conversationId, me, text, System.currentTimeMillis(), replyToId)
        publish(conversationId)
        touch(conversationId)
    }

    private fun touch(conversationId: Long) {
        convs.value = convs.value.toList()
    }

    private fun update(id: Long, f: (Message) -> Message) {
        val i = all.indexOfFirst { it.id == id }
        if (i < 0) return
        all[i] = f(all[i])
        publish(all[i].conversationId)
        touch(all[i].conversationId)
    }

    override fun edit(messageId: Long, text: String) = update(messageId) { it.copy(text = text, edited = true) }
    override fun delete(messageId: Long) = update(messageId) { it.copy(deleted = true, text = "") }
    override fun react(messageId: Long, emoji: String) = update(messageId) {
        it.copy(reactions = it.reactions + (emoji to ((it.reactions[emoji] ?: 0) + 1)))
    }

    private fun updateConv(id: Long, f: (Conversation) -> Conversation) {
        convs.value = convs.value.map { if (it.id == id) f(it) else it }
    }

    override fun markRead(conversationId: Long) = updateConv(conversationId) { it.copy(unread = 0) }
    override fun setPinned(conversationId: Long, value: Boolean) = updateConv(conversationId) { it.copy(pinned = value) }
    override fun setMuted(conversationId: Long, value: Boolean) = updateConv(conversationId) { it.copy(muted = value) }
    override fun setArchived(conversationId: Long, value: Boolean) = updateConv(conversationId) { it.copy(archived = value) }

    override fun createConversation(title: String): Long {
        val id = (convs.value.maxOfOrNull { it.id } ?: 0) + 1
        convs.value = convs.value + Conversation(id, title)
        return id
    }
}
