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
package com.ivoberger.jmusicbot.client

import com.ivoberger.jmusicbot.client.api.DEFAULT_PORT
import com.ivoberger.jmusicbot.client.api.MusicBotService
import com.ivoberger.jmusicbot.client.api.listenForServerMulticast
import com.ivoberger.jmusicbot.client.api.process
import com.ivoberger.jmusicbot.client.di.BaseComponent
import com.ivoberger.jmusicbot.client.di.BaseModule
import com.ivoberger.jmusicbot.client.di.DaggerBaseComponent
import com.ivoberger.jmusicbot.client.di.ServerSession
import com.ivoberger.jmusicbot.client.di.UserSession
import com.ivoberger.jmusicbot.client.exceptions.AuthException
import com.ivoberger.jmusicbot.client.exceptions.InvalidParametersException
import com.ivoberger.jmusicbot.client.exceptions.NotFoundException
import com.ivoberger.jmusicbot.client.exceptions.ServerErrorException
import com.ivoberger.jmusicbot.client.exceptions.UsernameTakenException
import com.ivoberger.jmusicbot.client.listener.ConnectionChangeListener
import com.ivoberger.jmusicbot.client.model.Auth
import com.ivoberger.jmusicbot.client.model.Event
import com.ivoberger.jmusicbot.client.model.MusicBotPlugin
import com.ivoberger.jmusicbot.client.model.PlayerState
import com.ivoberger.jmusicbot.client.model.PlayerStates
import com.ivoberger.jmusicbot.client.model.QueueEntry
import com.ivoberger.jmusicbot.client.model.Song
import com.ivoberger.jmusicbot.client.model.State
import com.ivoberger.jmusicbot.client.model.User
import com.ivoberger.jmusicbot.client.model.VersionInfo
import com.ivoberger.jmusicbot.client.model.makeStateMachine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import okhttp3.logging.HttpLoggingInterceptor
import timber.log.Timber
import timber.log.debug
import timber.log.warn
import java.util.Timer
import kotlin.concurrent.timer

@Suppress("unused")
@ExperimentalCoroutinesApi
object JMusicBot {

    internal val stateMachine = makeStateMachine()

    val state: State
        get() = stateMachine.state
    val stateBroadcast: BroadcastChannel<State> = ConflatedBroadcastChannel(State.Disconnected)
    val isConnected: Boolean
        get() = state.isConnected

    internal val mBaseComponent: BaseComponent = DaggerBaseComponent.builder()
        .baseModule(BaseModule(HttpLoggingInterceptor.Level.BASIC)).build()

    internal var mServerSession: ServerSession? = null
    internal var mUserSession: UserSession? = null

    var baseUrl: String? = null
        get() = mServerSession?.baseUrl() ?: field
        internal set

    internal var mServiceClient: MusicBotService? = null

    val connectionListeners: MutableList<ConnectionChangeListener> = mutableListOf()

    private val mQueue: BroadcastChannel<List<QueueEntry>> = ConflatedBroadcastChannel()
    private val mPlayerState: BroadcastChannel<PlayerState> = ConflatedBroadcastChannel()

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
        }

    @Throws(
        InvalidParametersException::class, NotFoundException::class,
        ServerErrorException::class, IllegalStateException::class
    )
    suspend fun connect(authUser: User, host: String) = withContext(Dispatchers.IO) {
        Timber.debug { "Quick connect" }
        if (!state.hasServer) discoverHost(
            host
        )
        if (!state.isConnected) authorize(
            authUser
        )
    }

    suspend fun discoverHost(knownHost: String? = null, port: Int = DEFAULT_PORT) =
        withContext(Dispatchers.IO) {
            if (state.isDiscovering) return@withContext
            Timber.debug { "Discovering host" }
            if (!state.isDisconnected) stateMachine.transition(
                Event.Disconnect()
            )
            stateMachine.transition(Event.StartDiscovery)
            val hostAddress = knownHost ?: listenForServerMulticast(port)
            hostAddress?.let {
                baseUrl = knownHost ?: it
                Timber.debug { "Found host: $baseUrl" }
                stateMachine.transition(Event.ServerFound(baseUrl!!, port))
                return@withContext
            }
            Timber.debug { "No host found" }
            stateMachine.transition(Event.Disconnect())
        }

    @Throws(
        InvalidParametersException::class, NotFoundException::class,
        ServerErrorException::class, IllegalStateException::class
    )
    suspend fun authorize(authUser: User, token: String? = null) = withContext(Dispatchers.IO) {
        state.serverCheck()
        Timber.debug { "Starting authorization" }
        if (!state.authRequired) stateMachine.transition(Event.AuthExpired)
        user = authUser
        token?.let { authToken = Auth.Token(it) }
        if (tokenValid()) return@withContext
        try {
            register(authUser)
            if (!authUser.password.isNullOrBlank()) changePassword(
                authUser.password!!
            )
            return@withContext
        } catch (e: UsernameTakenException) {
            Timber.warn(e) { e.message ?: "" }
            if (user?.password.isNullOrBlank()) {
                Timber.debug { "No passwords found, throwing exception" }
                throw e
            }
        }
        try {
            login(authUser)
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
                stateMachine.transition(
                    Event.Authorize(
                        user!!, authToken!!
                    )
                )
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
    private suspend fun register(user: User) = withContext(Dispatchers.IO) {
        Timber.debug { "Registering ${user.name}" }
        state.serverCheck()
        val credentials = Auth.Register(user)
        val token = mServiceClient!!.registerUser(credentials).process()!!
        Timber.debug { "Registered ${user.name}" }
        authToken = Auth.Token(token)
        stateMachine.transition(Event.Authorize(user, authToken!!))
    }

    @Throws(
        InvalidParametersException::class, AuthException::class,
        NotFoundException::class, ServerErrorException::class, IllegalStateException::class
    )
    private suspend fun login(user: User) = withContext(Dispatchers.IO) {
        Timber.debug { "Logging in ${user.name}" }
        state.serverCheck()
        val credentials = Auth.Basic(user).toAuthHeader()
        Timber.debug { "Auth: $credentials" }
        val token = mServiceClient!!.loginUser(credentials).process()!!
        Timber.debug { "Logged in ${user.name}" }
        authToken = Auth.Token(token)
        stateMachine.transition(Event.Authorize(user, authToken!!))
    }

    @Throws(InvalidParametersException::class, AuthException::class)
    suspend fun changePassword(newPassword: String) = withContext(Dispatchers.IO) {
        state.connectionCheck()
        authToken = Auth.Token(
            mServiceClient!!.changePassword(Auth.PasswordChange((newPassword))).process()!!
        )
        authToken?.also { user?.password = newPassword }
    }

    suspend fun logout() = withContext(Dispatchers.IO) {
        stopQueueUpdates()
        stopPlayerUpdates()
        stateMachine.transition(Event.AuthExpired)
    }

    suspend fun reloadPermissions() = withContext(Dispatchers.IO) {
        stateMachine.transition(Event.AuthExpired)
        user?.let {
            authorize(
                it
            )
        }
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
        updateQueue(
            mServiceClient!!.enqueue(
                song.id,
                song.provider.id
            ).process()
        )
    }

    @Throws(InvalidParametersException::class, AuthException::class, NotFoundException::class)
    suspend fun dequeue(song: Song) = withContext(Dispatchers.IO) {
        state.connectionCheck()
        updateQueue(
            mServiceClient!!.dequeue(
                song.id,
                song.provider.id
            ).process()
        )
    }

    suspend fun moveEntry(entry: QueueEntry, providerId: String, songId: String, newPosition: Int) =
        withContext(Dispatchers.IO) {
            state.connectionCheck()
            updateQueue(
                mServiceClient!!.moveEntry(
                    entry,
                    providerId,
                    songId,
                    newPosition
                ).process()
            )
        }

    suspend fun search(providerId: String, query: String): List<Song> =
        withContext(Dispatchers.IO) {
            state.connectionCheck()
            return@withContext mServiceClient!!.searchForSong(providerId, query).process()
                ?: listOf()
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

    fun getQueue(period: Long = 500): BroadcastChannel<List<QueueEntry>> {
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

    fun getPlayerState(period: Long = 500): BroadcastChannel<PlayerState> {
        startPlayerUpdates(period)
        return mPlayerState
    }

    fun startPlayerUpdates(period: Long = 500) {
        if (mPlayerUpdateTimer == null) mPlayerUpdateTimer =
            timer(period = period) { updatePlayer() }
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
