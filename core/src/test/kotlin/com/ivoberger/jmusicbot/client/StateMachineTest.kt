package com.ivoberger.jmusicbot.client

import com.ivoberger.jmusicbot.client.model.Auth
import com.ivoberger.jmusicbot.client.model.Event
import com.ivoberger.jmusicbot.client.model.State
import com.ivoberger.jmusicbot.client.testUtils.enterAuthRequiredState
import com.ivoberger.jmusicbot.client.testUtils.enterConnectedState
import com.ivoberger.jmusicbot.client.testUtils.enterDiscoveringState
import com.ivoberger.jmusicbot.client.testUtils.testUser
import com.ivoberger.jmusicbot.client.testUtils.toToken
import com.ivoberger.jmusicbot.client.utils.DEFAULT_PORT
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo

@ExperimentalCoroutinesApi
internal class StateMachineTest {

    private val state
        get() = JMusicBot.stateMachine.state

    private val testAddress = "test"
    private val baseUrl = "http://$testAddress:$DEFAULT_PORT/"
    private val authToken = Auth.Token(testUser.toToken())

    private val serverFoundEvent = Event.ServerFound(testAddress, DEFAULT_PORT)
    private val authorizeEvent = Event.Authorize(testUser, authToken)

    private val testEvents = listOf(
        Event.StartDiscovery,
        serverFoundEvent,
        authorizeEvent,
        Event.AuthExpired,
        Event.Disconnect()
    )

    @BeforeEach
    fun setUp() {
        JMusicBot.stateMachine.transition(Event.Disconnect())
    }

    @Test
    fun initialState() {
        expectThat(state).isEqualTo(State.Disconnected)
        checkTransitions(listOf(Event.StartDiscovery), State.Disconnected)
        JMusicBot.stateMachine.transition(Event.StartDiscovery)
        expectThat(state).isEqualTo(State.Discovering)
    }

    @Test
    fun discoveringState() {
        JMusicBot.stateMachine.enterDiscoveringState()
        checkTransitions(listOf(serverFoundEvent, Event.Disconnect()), State.Discovering)
        // server found
        JMusicBot.stateMachine.transition(serverFoundEvent)
        expectThat(state).isEqualTo(State.AuthRequired)
        // reset
        setUp()
        // disconnected
        JMusicBot.stateMachine.enterDiscoveringState()
        JMusicBot.stateMachine.transition(Event.Disconnect())
        expectThat(state).isEqualTo(State.Disconnected)
    }

    @Test
    fun authRequiredState() {
        JMusicBot.stateMachine.enterAuthRequiredState(serverFoundEvent)
        checkTransitions(listOf(authorizeEvent, Event.Disconnect()), State.AuthRequired)
        expectThat(JMusicBot.baseUrl).isEqualTo(baseUrl)
        // authorize
        JMusicBot.stateMachine.transition(authorizeEvent)
        expectThat(state).isEqualTo(State.Connected)
        // reset
        setUp()
        // disconnected
        JMusicBot.stateMachine.enterAuthRequiredState(serverFoundEvent)
        JMusicBot.stateMachine.transition(Event.Disconnect())
        expectThat(state).isEqualTo(State.Disconnected)
    }

    @Test
    fun connectedState() {
        JMusicBot.stateMachine.enterConnectedState(serverFoundEvent, authorizeEvent)
        checkTransitions(listOf(Event.AuthExpired, Event.Disconnect()), State.Connected)
        expectThat(JMusicBot.baseUrl).isEqualTo(baseUrl)
        expectThat(JMusicBot.user).isEqualTo(testUser)
        expectThat(JMusicBot.authToken).isEqualTo(authToken)
        // auth expired
        JMusicBot.stateMachine.transition(Event.AuthExpired)
        expectThat(state).isEqualTo(State.AuthRequired)
        // reset
        setUp()
        // disconnected
        JMusicBot.stateMachine.enterConnectedState(serverFoundEvent, authorizeEvent)
        JMusicBot.stateMachine.transition(Event.Disconnect())
        expectThat(state).isEqualTo(State.Disconnected)
    }

    private fun checkTransitions(validEvents: List<Event>, currentState: State) =
        testEvents.forEach { event ->
            // filter event causing an actual transition
            if (!validEvents.contains(event) && !validEvents.any { it::class != event::class }) {
                JMusicBot.stateMachine.transition(event)
                expectThat(state).isEqualTo(currentState)
            }
        }
}

