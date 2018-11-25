package me.iberger.jmusicbot

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.squareup.moshi.Moshi
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import me.iberger.jmusicbot.data.*
import me.iberger.jmusicbot.exceptions.AuthException
import me.iberger.jmusicbot.exceptions.InvalidParametersException
import me.iberger.jmusicbot.exceptions.NotFoundException
import me.iberger.jmusicbot.exceptions.UsernameTakenException
import me.iberger.jmusicbot.listener.PlayerUpdateListener
import me.iberger.jmusicbot.listener.QueueUpdateListener
import me.iberger.jmusicbot.network.MusicBotAPI
import me.iberger.jmusicbot.network.TokenAuthenticator
import me.iberger.jmusicbot.network.process
import me.iberger.jmusicbot.network.verifyHostAddress
import okhttp3.OkHttpClient
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import timber.log.Timber
import java.util.*
import kotlin.concurrent.fixedRateTimer

class MusicBot(
    private val mPreferences: SharedPreferences,
    baseUrl: String,
    user: User,
    initToken: String
) {
    var user: User = user
        set(newUser) {
            newUser.save(mPreferences)
            field = newUser
        }

    var authToken: String = initToken
        set(newToken) {
            mPreferences.edit { putString(KEY_AUTHORIZATION, newToken) }
            field = newToken
        }

    init {
        this.user = user
        authToken = initToken
    }

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder().apply {
        addInterceptor { chain ->
            chain.proceed(chain.request().newBuilder().addHeader(KEY_AUTHORIZATION, authToken).build())
        }
    }.authenticator(TokenAuthenticator()).cache(null).build()

    private val apiClient: MusicBotAPI = Retrofit.Builder()
        .addConverterFactory(MoshiConverterFactory.create(mMoshi).asLenient())
        .baseUrl(baseUrl)
        .client(okHttpClient)
        .build()
        .create(MusicBotAPI::class.java)

    val provider: List<MusicBotPlugin>
        get() = apiClient.getProvider().process()!!

    val suggesters: List<MusicBotPlugin>
        get() = apiClient.getSuggesters().process()!!

    private var mQueueUpdateTimer: Timer? = null
    private var mPlayerUpdateTimer: Timer? = null

    private val mQueueUpdateListeners: MutableList<QueueUpdateListener> = mutableListOf()
    private val mPlayerUpdateListeners: MutableList<PlayerUpdateListener> = mutableListOf()

    fun deleteUser(): Deferred<Unit?> =
        GlobalScope.async { apiClient.deleteUser().process() }

    @Throws(InvalidParametersException::class, AuthException::class)
    fun changePassword(newPassword: String) = GlobalScope.async {
        authToken = apiClient.changePassword(
            Credentials.PasswordChange((newPassword))
        ).process()!!
        user.password = newPassword
        user.save(mPreferences)
    }

    fun refreshToken(): Response<String> = apiClient.login(Credentials.Login(user)).execute()

    fun search(providerId: String, query: String): Deferred<List<Song>> =
        GlobalScope.async { apiClient.searchForSong(providerId, query).process()!! }

    @Throws(InvalidParametersException::class, AuthException::class, NotFoundException::class)
    fun enqueue(song: Song): Deferred<Unit> =
        GlobalScope.async { updateQueue(apiClient.enqueue(song.id, song.provider.id).process()) }

    @Throws(InvalidParametersException::class, AuthException::class, NotFoundException::class)
    fun dequeue(song: Song): Deferred<Unit> =
        GlobalScope.async { updateQueue(apiClient.dequeue(song.id, song.provider.id).process()) }

    fun moveSong(song: Song, newPosition: Int): Deferred<Unit> =
        GlobalScope.async { updateQueue(apiClient.moveSong(song, newPosition).process()) }

    fun lookupSong(providerId: String, songId: String) =
        GlobalScope.async { apiClient.lookupSong(providerId, songId) }

    val history: List<QueueEntry>
        get() = apiClient.getHistory().process()!!

    fun getSuggestions(suggesterId: String): Deferred<List<Song>> = GlobalScope.async {
        apiClient.getSuggestions(suggesterId).process()!!
    }

    fun deleteSuggestion(suggesterId: String, song: Song): Deferred<Unit?> =
        GlobalScope.async { apiClient.deleteSuggestion(suggesterId, song.id, song.provider.id).process() }

    private fun changePlayerState(action: PlayerAction): Deferred<Unit> =
        GlobalScope.async { updatePlayer(apiClient.setPlayerState(PlayerStateChange(action)).process()) }

    fun pause(): Deferred<Unit> = GlobalScope.async { updatePlayer(apiClient.pause().process()!!) }
    fun play(): Deferred<Unit> = GlobalScope.async { updatePlayer(apiClient.play().process()!!) }
    fun skip(): Deferred<Unit> = GlobalScope.async { updatePlayer(apiClient.skip().process()!!) }

    fun startQueueUpdates(listener: QueueUpdateListener, period: Long = 500) {
        mQueueUpdateListeners.add(listener)
        mQueueUpdateTimer = fixedRateTimer(period = period) { updateQueue() }
    }

    fun stopQueueUpdates(listener: QueueUpdateListener) {
        mQueueUpdateListeners.remove(listener)
        if (mQueueUpdateListeners.isEmpty()) {
            mQueueUpdateTimer?.cancel()
            mQueueUpdateTimer = null
        }
    }

    fun startPlayerUpdates(listener: PlayerUpdateListener, period: Long = 500) {
        mPlayerUpdateListeners.add(listener)
        mPlayerUpdateTimer = fixedRateTimer(period = period) { updatePlayer() }
    }

    fun stopPlayerUpdates(listener: PlayerUpdateListener) {
        mPlayerUpdateListeners.remove(listener)
        if (mPlayerUpdateListeners.isEmpty()) {
            mPlayerUpdateTimer?.cancel()
            mPlayerUpdateTimer = null
        }
    }

    private fun updateQueue(queue: List<QueueEntry>? = null) {
        try {
            val newQueue = queue ?: apiClient.getQueue().process()!!
            mQueueUpdateListeners.forEach { it.onQueueChanged(newQueue) }
        } catch (e: Exception) {
            mQueueUpdateListeners.forEach { it.onUpdateError(e) }
        }
    }

    private fun updatePlayer(playerState: PlayerState? = null) {
        try {
            val newState = playerState ?: apiClient.getPlayerState().process()!!
            mPlayerUpdateListeners.forEach { it.onPlayerStateChanged(newState) }
        } catch (e: Exception) {
            mPlayerUpdateListeners.forEach { it.onUpdateError(e) }
        }
    }


    // ########## Companion object with init functions ########## //

    companion object {

        lateinit var instance: MusicBot
        internal var baseUrl: String? = null

        private val mMoshi = Moshi.Builder().build()
        private lateinit var apiClient: MusicBotAPI

        @Throws(IllegalArgumentException::class, UsernameTakenException::class)
        fun init(
            context: Context, userName: String? = null, password: String? = null, hostAddress: String? = null
        ): Deferred<MusicBot> = GlobalScope.async {
            Timber.d("Initiating MusicBot")
            val preferences = context.getSharedPreferences(KEY_PREFERENCES, Context.MODE_PRIVATE)
            verifyHostAddress(context, hostAddress)
            Timber.d("User setup")
            if (!hasUser(context)) {
                User(
                    userName ?: throw IllegalArgumentException("No user saved and no username given"),
                    password = password
                ).save(preferences)
            }
            authorize(context).let {
                instance = MusicBot(preferences, baseUrl!!, it.first, it.second)
                return@async instance
            }
        }

        fun hasAuthorization(context: Context) = try {
            authorize(context)
            true
        } catch (e: Exception) {
            Timber.w(e)
            false
        }

        private fun authorize(context: Context): Pair<User, String> {
            val preferences = context.getSharedPreferences(KEY_PREFERENCES, Context.MODE_PRIVATE)
            if (!hasUser(context)) throw NotFoundException(NotFoundException.Type.USER, "No user saved")
            preferences.getString(KEY_AUTHORIZATION, null)?.also {
                try {
                    apiClient.testToken(it)
                    return User.load(preferences)!! to it
                } catch (e: Exception) {
                    Timber.w(e)
                    return@also
                }
            }
            User.load(preferences, mMoshi)!!.also { user ->
                return if (!user.password.isNullOrBlank()) user to loginUser(user)
                else user to registerUser(user.name)
            }
        }

        private fun hasUser(context: Context) =
            context.getSharedPreferences(KEY_PREFERENCES, Context.MODE_PRIVATE).contains(KEY_USER)

        fun hasServer(context: Context): Boolean {
            verifyHostAddress(context)
            baseUrl?.also {
                apiClient = Retrofit.Builder()
                    .addConverterFactory(MoshiConverterFactory.create(mMoshi).asLenient())
                    .baseUrl(it)
                    .build()
                    .create(MusicBotAPI::class.java)
                return true
            }
            return false
        }

        private fun loginUser(user: User): String {
            Timber.d("Logging in user ${user.name}")
            Timber.d(mMoshi.adapter<Credentials.Login>(Credentials.Login::class.java).toJson(Credentials.Login(user)))
            return apiClient.login(Credentials.Login(user)).process()!!
        }

        private fun registerUser(name: String): String {
            Timber.d("Registering user $name")
            return apiClient.registerUser(Credentials.Register(name)).process(errorCodes = mapOf(409 to UsernameTakenException()))!!
        }
    }
}