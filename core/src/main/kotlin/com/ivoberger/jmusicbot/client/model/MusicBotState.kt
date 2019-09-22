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
package com.ivoberger.jmusicbot.client.model

import com.ivoberger.jmusicbot.client.JMusicBot
import com.ivoberger.jmusicbot.client.di.ServerModule
import com.ivoberger.jmusicbot.client.di.UserModule
import com.tinder.StateMachine
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.sendBlocking
import timber.log.Timber
import timber.log.debug

@ExperimentalCoroutinesApi
internal fun JMusicBot.makeStateMachine(): StateMachine<State, Event, SideEffect> =
    StateMachine.create {
        initialState(State.Disconnected)
        state<State.Disconnected> {
            on<Event.StartDiscovery> { transitionTo(State.Discovering) }
        }
        state<State.Discovering> {
            on<Event.ServerFound> {
                transitionTo(State.AuthRequired, SideEffect.StartServerSession)
            }
            on<Event.Disconnect> { transitionTo(State.Disconnected, SideEffect.EndServerSession) }
        }
        state<State.AuthRequired> {
            on<Event.Authorize> { transitionTo(State.Connected, SideEffect.StartUserSession) }
            on<Event.Disconnect> { transitionTo(State.Disconnected, SideEffect.EndServerSession) }
        }
        state<State.Connected> {
            on<Event.AuthExpired> { transitionTo(State.AuthRequired, SideEffect.EndUserSession) }
            on<Event.Authorize> { transitionTo(State.Connected, SideEffect.StartUserSession) }
            on<Event.Disconnect> { transitionTo(State.Disconnected, SideEffect.EndServerSession) }
        }
        onTransition { transition ->
            val validTransition = transition as? StateMachine.Transition.Valid
            validTransition?.let { trans ->
                Timber.debug { "State transition from ${trans.fromState} to ${trans.toState} by ${trans.event}" }
                when (trans.sideEffect) {
                    is SideEffect.StartServerSession -> {
                        val event = trans.event as Event.ServerFound
                        mServerSession = JMusicBot.mBaseComponent.serverSession(event.serverModule)
                        mServiceClient = mServerSession!!.musicBotService()
                    }
                    is SideEffect.StartUserSession -> {
                        val event = trans.event as Event.Authorize
                        mUserSession = mServerSession!!.userSession(event.userModule)
                        mServiceClient = mUserSession!!.musicBotService()
                        connectionListeners.forEach { it.onConnectionRecovered() }
                    }
                    SideEffect.EndUserSession -> endUserSession()
                    SideEffect.EndServerSession -> {
                        endUserSession()
                        mServerSession = null
                        mServiceClient = null
                        if (trans.fromState is State.Discovering) return@onTransition
                        val event = trans.event as Event.Disconnect
                        connectionListeners.forEach { it.onConnectionLost(event.reason) }
                    }
                }
                stateBroadcast.sendBlocking(trans.toState)
                return@onTransition
            }
            val invalidTransition = transition as? StateMachine.Transition.Invalid
            invalidTransition?.let { Timber.debug { "Attempted internalState transition from ${it.fromState} by ${it.event}" } }
        }
    }

@ExperimentalCoroutinesApi
internal fun JMusicBot.endUserSession() {
    stopPlayerUpdates()
    stopQueueUpdates()
    mUserSession = null
    mServiceClient = mServerSession?.musicBotService()
}

/**
 * Possible states for the musicBotService bot jmusicbot.jmusicbot.client to be in
 */
sealed class State {
    override fun toString(): String = this::class.java.simpleName

    /** Client has no server connection */
    object Disconnected : State()

    /** Client is in the trying to find a host */
    object Discovering : State()

    /** Client has found a server but needs login/new auth token */
    object AuthRequired : State()

    /** Server connection and authentication is successfully established */
    object Connected : State()

    val isDisconnected
        get() = this == Disconnected
    val isDiscovering
        get() = this == Discovering
    val authRequired
        get() = this == AuthRequired
    val hasServer
        get() = authRequired
    val isConnected: Boolean
        get() = this == Connected

    @Throws(IllegalStateException::class)
    fun connectionCheck() {
        check(isConnected) { "Client not connected" }
    }

    @Throws(IllegalStateException::class)
    fun serverCheck() {
        check(hasServer) { "Client has no server" }
    }
}

sealed class Event {
    override fun toString(): String = this::class.java.simpleName

    object StartDiscovery : Event()
    class ServerFound(baseUrl: String, port: Int) : Event() {
        internal val serverModule: ServerModule = ServerModule(baseUrl, port)
    }

    class Authorize(
        user: User,
        authToken: Auth.Token
    ) : Event() {
        internal val userModule: UserModule

        init {
            user.permissions = authToken.permissions
            userModule = UserModule(user, authToken)
        }
    }

    object AuthExpired : Event()
    class Disconnect(val reason: Exception? = null) : Event()
}

sealed class SideEffect {
    override fun toString(): String = this::class.java.simpleName

    object StartServerSession : SideEffect()
    object StartUserSession : SideEffect()
    object EndUserSession : SideEffect()
    object EndServerSession : SideEffect()
}
