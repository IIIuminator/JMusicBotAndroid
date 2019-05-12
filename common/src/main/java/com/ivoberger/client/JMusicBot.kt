/*
* Copyright 2019 Ivo Berger
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package com.ivoberger.client


import com.ivoberger.client.api.MusicBotService
import com.ivoberger.client.api.PORT
import com.ivoberger.client.api.listenForServerMulticast
import com.ivoberger.client.api.process
import com.ivoberger.client.di.BaseComponent
import com.ivoberger.client.di.ServerSession
import com.ivoberger.client.di.UserSession
import com.ivoberger.client.exceptions.*
import com.ivoberger.client.listener.ConnectionChangeListener
import com.ivoberger.client.model.*
import com.tinder.StateMachine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import timber.log.Timber
import timber.log.debug
import timber.log.warn
import java.util.*
import kotlin.concurrent.timer

object JMusicBot {

    internal val stateMachine = StateMachine.create<State, Event, SideEffect> {
        initialState(State.Disconnected)
        state<State.Disconnected> { on<Event.StartDiscovery> { transitionTo(State.Discovering) } }
        state<State.Discovering> {
            on<Event.ServerFound> { transitionTo(State.AuthRequired, SideEffect.StartServerSession) }
            on<Event.Disconnect> { transitionTo(State.Disconnected, SideEffect.EndServerSession) }
        }
        state<State.AuthRequired> {
            on<Event.Authorize> { transitionTo(State.Connected, SideEffect.StartUserSession) }
            on<Event.Disconnect> { transitionTo(State.Disconnected, SideEffect.EndServerSession) }
        }
        state<State.Connected> {
            on<Event.AuthExpired> { transitionTo(State.AuthRequired, SideEffect.EndUserSession) }
            on<Event.Disconnect> { transitionTo(State.Disconnected, SideEffect.EndServerSession) }
        }
        onTransition { transition ->
            val validTransition = transition as? StateMachine.Transition.Valid
            validTransition?.let { trans ->
                Timber.debug { "State transition from ${trans.fromState} to ${trans.toState} by ${trans.event}" }
                when (trans.sideEffect) {
                    is SideEffect.StartServerSession -> {
                        val event = trans.event as Event.ServerFound
                        mServerSession = mBaseComponent.serverSession(event.serverModule)
                        mServiceClient = mServerSession!!.musicBotService()
                    }
                    is SideEffect.StartUserSession -> {
                        val event = trans.event as Event.Authorize
                        mUserSession = mServerSession!!.userSession(event.userModule)
                        mServiceClient = mUserSession!!.musicBotService()
                        connectionListeners.forEach { it.onConnectionRecovered() }
                    }
                    SideEffect.EndUserSession -> {
                        user = null
                        mUserSession = null
                        mServiceClient = mServerSession!!.musicBotService()
                    }
                    SideEffect.EndServerSession -> {
                        user = null
                        mUserSession = null
                        mServerSession = null
                        if (trans.fromState is State.Discovering) return@onTransition
                        val event = trans.event as Event.Disconnect
                        connectionListeners.forEach { it.onConnectionLost(event.reason) }
                    }
                }
                return@onTransition
            }
            val invalidTransition = transition as? StateMachine.Transition.Invalid
            invalidTransition?.let { Timber.debug { "Attempted state transition from ${it.fromState} by ${it.event}" } }
        }
    }

    val state: State
        get() = stateMachine.state
    val isConnected: Boolean
        get() = state.isConnected

    internal lateinit var mBaseComponent: BaseComponent
//        DaggerBaseComponent.builder().baseModule(BaseModule(HttpLoggingInterceptor.Level.BASIC)).build()

    private var mServerSession: ServerSession? = null
    private var mUserSession: UserSession? = null

    var baseUrl: String? = null
        get() = mServerSession?.baseUrl() ?: field
        private set

    private var mServiceClient: MusicBotService? = null

    val connectionListeners: MutableList<ConnectionChangeListener> = mutableListOf()

    private val mQueue: Channel<List<QueueEntry>> = Channel(Channel.CONFLATED)
    private val mPlayerState: Channel<PlayerState> = Channel(Channel.CONFLATED)

    private var mQueueUpdateTimer: Timer? = null
    private var mPlayerUpdateTimer: Timer? = null

    var user: User? = null
        get() = mUserSession?.user ?: field
        internal set(value) {
            Timber.debug { "Setting user to ${value?.name}" }
            field = value
            if (value == null) authToken = null
        }

    internal var authToken: Auth.Token? = null
        get() = mUserSession?.authToken ?: field
        set(value) {
            Timber.debug { "Setting token to $value" }
            field = value
            field?.let {
                user?.permissions = it.permissions
            }
        }

    @Throws(
        InvalidParametersException::class, NotFoundException::class,
        ServerErrorException::class, IllegalStateException::class
    )
    suspend fun connect(authUser: User, host: String) = withContext(Dispatchers.IO) {
        Timber.debug { "Quick connect" }
        if (!state.hasServer) discoverHost(host)
        if (!state.isConnected) authorize(authUser)
    }

    suspend fun discoverHost(knownHost: String? = null) = withContext(Dispatchers.IO) {
        if (state.isDiscovering || state.running != null) return@withContext
        Timber.debug { "Discovering host" }
        if (!state.isDisconnected) stateMachine.transition(Event.Disconnect())
        stateMachine.transition(Event.StartDiscovery)
        val hostAddress = knownHost ?: listenForServerMulticast()
        hostAddress?.let {
            baseUrl = knownHost ?: "http://$it:$PORT/"
            Timber.debug { "Found host: $baseUrl" }
            stateMachine.transition(Event.ServerFound(baseUrl!!))
            return@withContext
        }
        Timber.debug { "No host found" }
        stateMachine.transition(Event.Disconnect())
    }

    suspend fun recoverConnection() = withContext(Dispatchers.IO) {
        Timber.debug { "Reconnecting" }
        while (!state.hasServer) {
            discoverHost()
        }
        authorize()
    }

    @Throws(
        InvalidParametersException::class, NotFoundException::class,
        ServerErrorException::class, IllegalStateException::class
    )
    suspend fun authorize(authUser: User?) = withContext(Dispatchers.IO) {
        user = authUser
        authorize()
    }

    @Throws(
        InvalidParametersException::class, NotFoundException::class,
        ServerErrorException::class, IllegalStateException::class
    )
    suspend fun authorize(userName: String? = null, password: String? = null) = withContext(Dispatchers.IO) {
        state.serverCheck()
        Timber.debug { "Starting authorization" }
        if (tokenValid()) return@withContext
        if (userName.isNullOrBlank() && user == null) throw IllegalStateException("No username stored or supplied")
        try {
            register(userName)
            if (!password.isNullOrBlank()) changePassword(password)
            return@withContext
        } catch (e: UsernameTakenException) {
            Timber.warn(e) { e.message ?: "" }
            if (password.isNullOrBlank() && user?.password.isNullOrBlank()) {
                Timber.debug { "No passwords found, throwing exception, $password, ${user?.name}" }
                throw e
            }
        }
        try {
            login(userName, password)
            if (tokenValid()) return@withContext
        } catch (e: Exception) {
            Timber.warn(e) { e.message ?: "" }
            Timber.debug { "Authorization failed" }
            throw e
        }
    }

    private suspend fun tokenValid(): Boolean {
        if (authToken == null) {
            Timber.debug { "Invalid Token: No token stored" }
            return false
        }
        try {
            if (authToken!!.isExpired()) {
                Timber.debug { "Invalid Token: Token expired" }
                authToken = null
                return false
            }
            val tmpUser = mServiceClient!!.testToken(authToken!!.toAuthHeader()).process()
            if (tmpUser?.name == user?.name) {
                Timber.debug { "Valid Token: ${user?.name}" }
                stateMachine.transition(Event.Authorize(user!!, authToken!!))
                return true
            }
            Timber.debug { "Invalid Token: User changed" }
        } catch (e: Exception) {
            Timber.debug { "Invalid Token: Test failed (${e.localizedMessage}" }
        }
        authToken = null
        return false
    }

    @Throws(
        InvalidParametersException::class, AuthException::class,
        NotFoundException::class, ServerErrorException::class, IllegalStateException::class
    )
    private suspend fun register(userName: String? = null) = withContext(Dispatchers.IO) {
        Timber.debug { "Registering ${userName?.let { User(it) } ?: user}" }
        state.serverCheck()
        val credentials = when {
            (!userName.isNullOrBlank()) -> {
                user = User(userName)
                Auth.Register(userName)
            }
            user != null -> Auth.Register(user!!)
            else -> throw IllegalStateException("No username stored or supplied")
        }
        val token = mServiceClient!!.registerUser(credentials).process()!!
        Timber.debug { "Registered $user" }
        authToken = Auth.Token(token)
        stateMachine.transition(Event.Authorize(user!!, authToken!!))
    }

    @Throws(
        InvalidParametersException::class, AuthException::class,
        NotFoundException::class, ServerErrorException::class, IllegalStateException::class
    )
    private suspend fun login(userName: String? = null, password: String? = null) = withContext(Dispatchers.IO) {
        Timber.debug { "Logging in ${userName ?: user?.name}" }
        state.serverCheck()
        val credentials = when {
            (!(userName.isNullOrBlank() || password.isNullOrBlank())) -> {
                user = User(userName, password)
                Auth.Basic(userName, password).toAuthHeader()
            }
            user != null -> Auth.Basic(user!!).toAuthHeader()
            else -> throw IllegalStateException("No user stored or supplied")
        }
        Timber.debug { "Auth: $credentials" }
        val token = mServiceClient!!.loginUser(credentials).process()!!
        Timber.debug { "Logged in ${user?.name}" }
        authToken = Auth.Token(token)
        stateMachine.transition(Event.Authorize(user!!, authToken!!))
    }

    @Throws(InvalidParametersException::class, AuthException::class)
    suspend fun changePassword(newPassword: String) = withContext(Dispatchers.IO) {
        state.connectionCheck()
        authToken = Auth.Token(mServiceClient!!.changePassword(Auth.PasswordChange((newPassword))).process()!!)
        authToken?.also { user?.password = newPassword }
    }

    suspend fun logout() = withContext(Dispatchers.IO) {
        stopQueueUpdates()
        stopPlayerUpdates()
        stateMachine.transition(Event.AuthExpired)
    }

    suspend fun reloadPermissions() = withContext(Dispatchers.IO) {
        stateMachine.transition(Event.AuthExpired)
        recoverConnection()
    }

    @Throws(
        InvalidParametersException::class, AuthException::class,
        NotFoundException::class, ServerErrorException::class, IllegalStateException::class
    )
    suspend fun deleteUser() = withContext(Dispatchers.IO) {
        state.connectionCheck()
        Timber.debug { "Deleting user ${user?.name}" }
        authToken ?: throw IllegalStateException("Auth token is null")
        mServiceClient!!.deleteUser().process()
        user = null
        stateMachine.transition(Event.AuthExpired)
    }

    @Throws(InvalidParametersException::class, AuthException::class, NotFoundException::class)
    suspend fun enqueue(song: Song) = withContext(Dispatchers.IO) {
        state.connectionCheck()
        updateQueue(mServiceClient!!.enqueue(song.id, song.provider.id).process())
    }

    @Throws(InvalidParametersException::class, AuthException::class, NotFoundException::class)
    suspend fun dequeue(song: Song) = withContext(Dispatchers.IO) {
        state.connectionCheck()
        updateQueue(mServiceClient!!.dequeue(song.id, song.provider.id).process())
    }

    suspend fun moveEntry(entry: QueueEntry, providerId: String, songId: String, newPosition: Int) =
        withContext(Dispatchers.IO) {
            state.connectionCheck()
            updateQueue(mServiceClient!!.moveEntry(entry, providerId, songId, newPosition).process())
        }

    suspend fun search(providerId: String, query: String): List<Song> = withContext(Dispatchers.IO) {
        state.connectionCheck()
        return@withContext mServiceClient!!.searchForSong(providerId, query).process() ?: listOf()
    }

    suspend fun suggestions(suggesterId: String): List<Song> = withContext(Dispatchers.IO) {
        state.connectionCheck()
        return@withContext mServiceClient!!.getSuggestions(suggesterId).process() ?: listOf()
    }

    suspend fun deleteSuggestion(suggesterId: String, song: Song) = withContext(Dispatchers.IO) {
        state.connectionCheck()
        mServiceClient!!.deleteSuggestion(suggesterId, song.id, song.provider.id).process()
    }

    suspend fun pause() = withContext(Dispatchers.IO) {
        state.connectionCheck()
        updatePlayer(mServiceClient!!.pause().process())
    }

    suspend fun play() = withContext(Dispatchers.IO) {
        state.connectionCheck()
        updatePlayer(mServiceClient!!.play().process())
    }

    suspend fun skip() = withContext(Dispatchers.IO) {
        state.connectionCheck()
        updatePlayer(mServiceClient!!.skip().process())
    }

    suspend fun getProvider(): List<MusicBotPlugin> = withContext(Dispatchers.IO) {
        state.connectionCheck()
        return@withContext mServiceClient!!.getProvider().process() ?: listOf()
    }

    suspend fun getSuggesters(): List<MusicBotPlugin> = withContext(Dispatchers.IO) {
        state.connectionCheck()
        return@withContext mServiceClient!!.getSuggesters().process() ?: listOf()
    }

    suspend fun getVersionInfo(): VersionInfo = withContext(Dispatchers.IO) {
        state.serverCheck()
        return@withContext mServiceClient!!.getVersionInfo().process()!!
    }

    fun getQueue(period: Long = 500): Channel<List<QueueEntry>> {
        startQueueUpdates(period)
        return mQueue
    }

    fun startQueueUpdates(period: Long = 500) {
        if (mQueueUpdateTimer == null) mQueueUpdateTimer = timer(period = period) { updateQueue() }
    }

    fun stopQueueUpdates() {
        mQueueUpdateTimer?.cancel()
        mQueueUpdateTimer = null
    }

    fun getPlayerState(period: Long = 500): Channel<PlayerState> {
        startPlayerUpdates(period)
        return mPlayerState
    }

    fun startPlayerUpdates(period: Long = 500) {
        if (mPlayerUpdateTimer == null) mPlayerUpdateTimer = timer(period = period) { updatePlayer() }
    }

    fun stopPlayerUpdates() {
        mPlayerUpdateTimer?.cancel()
        mPlayerUpdateTimer = null
    }

    private fun updateQueue(newQueue: List<QueueEntry>? = null) = runBlocking(Dispatchers.IO) {
        if (newQueue != null) Timber.debug { "Manual Queue Update" }
        try {
            state.connectionCheck()
            val queue = newQueue ?: mServiceClient!!.getQueue().process() ?: listOf()
            withContext(Dispatchers.Main) { mQueue.send(queue) }
        } catch (e: Exception) {
            Timber.warn(e) { "Queue update failed" }
        }
    }

    private fun updatePlayer(playerState: PlayerState? = null) = runBlocking(Dispatchers.IO) {
        if (playerState != null) Timber.debug { "Manual Player Update" }
        try {
            state.connectionCheck()
            val state = playerState ?: mServiceClient!!.getPlayerState().process()
            ?: PlayerState(PlayerStates.ERROR)
            withContext(Dispatchers.Main) { mPlayerState.send(state) }
        } catch (e: Exception) {
            Timber.warn(e) { "Player state update failed" }
        }
    }
}
