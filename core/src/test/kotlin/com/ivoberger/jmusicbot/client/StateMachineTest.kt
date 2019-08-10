package com.ivoberger.jmusicbot.client

import com.ivoberger.jmusicbot.client.model.Auth
import com.ivoberger.jmusicbot.client.model.Event
import com.ivoberger.jmusicbot.client.model.State
import com.ivoberger.jmusicbot.client.testUtils.enterAuthRequiredState
import com.ivoberger.jmusicbot.client.testUtils.enterConnectedState
import com.ivoberger.jmusicbot.client.testUtils.enterDiscoveringState
import com.ivoberger.jmusicbot.client.testUtils.newTestUser
import com.ivoberger.jmusicbot.client.testUtils.toToken
import com.ivoberger.jmusicbot.client.utils.DEFAULT_PORT
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@ExperimentalCoroutinesApi
internal class StateMachineTest {

    private val state
        get() = JMusicBot.stateMachine.state

    private val testAddress = "test"
    private val baseUrl = "http://$testAddress:$DEFAULT_PORT/"
    private val authToken = Auth.Token(newTestUser.toToken())

    private val serverFoundEvent = Event.ServerFound(testAddress, DEFAULT_PORT)
    private val authorizeEvent = Event.Authorize(newTestUser, authToken)

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
        assertEquals(State.Disconnected, state)
        checkTransitions(listOf(Event.StartDiscovery), State.Disconnected)
        JMusicBot.stateMachine.transition(Event.StartDiscovery)
        assertEquals(State.Discovering, state)
    }

    @Test
    fun discoveringState() {
        JMusicBot.stateMachine.enterDiscoveringState()
        checkTransitions(listOf(serverFoundEvent, Event.Disconnect()), State.Discovering)
        // server found
        JMusicBot.stateMachine.transition(serverFoundEvent)
        assertEquals(State.AuthRequired, state)
        // reset
        setUp()
        // disconnected
        JMusicBot.stateMachine.enterDiscoveringState()
        JMusicBot.stateMachine.transition(Event.Disconnect())
        assertEquals(State.Disconnected, state)
    }

    @Test
    fun authRequiredState() {
        JMusicBot.stateMachine.enterAuthRequiredState(serverFoundEvent)
        checkTransitions(listOf(authorizeEvent, Event.Disconnect()), State.AuthRequired)
        assertEquals(baseUrl, JMusicBot.baseUrl)
        // authorize
        JMusicBot.stateMachine.transition(authorizeEvent)
        assertEquals(State.Connected, state)
        // reset
        setUp()
        // disconnected
        JMusicBot.stateMachine.enterAuthRequiredState(serverFoundEvent)
        JMusicBot.stateMachine.transition(Event.Disconnect())
        assertEquals(State.Disconnected, state)
    }

    @Test
    fun connectedState() {
        JMusicBot.stateMachine.enterConnectedState(serverFoundEvent, authorizeEvent)
        checkTransitions(listOf(Event.AuthExpired, Event.Disconnect()), State.Connected)
        assertEquals(baseUrl, JMusicBot.baseUrl)
        assertEquals(newTestUser, JMusicBot.user)
        assertEquals(authToken, JMusicBot.authToken)
        // auth expired
        JMusicBot.stateMachine.transition(Event.AuthExpired)
        assertEquals(State.AuthRequired, state)
        // reset
        setUp()
        // disconnected
        JMusicBot.stateMachine.enterConnectedState(serverFoundEvent, authorizeEvent)
        JMusicBot.stateMachine.transition(Event.Disconnect())
        assertEquals(State.Disconnected, state)
    }

    private fun checkTransitions(validEvents: List<Event>, currentState: State) =
        testEvents.forEach { event ->
            // filter event causing an actual transition
            if (!validEvents.contains(event) && !validEvents.any { it::class != event::class }) {
                JMusicBot.stateMachine.transition(event)
                assertEquals(currentState, state)
            }
        }
}

