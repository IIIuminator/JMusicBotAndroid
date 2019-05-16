package com.ivoberger.jmusicbot.client

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.github.debop.javatimes.plus
import com.ivoberger.jmusicbot.client.model.Auth
import com.ivoberger.jmusicbot.client.model.Event
import com.ivoberger.jmusicbot.client.model.SideEffect
import com.ivoberger.jmusicbot.client.model.State
import com.ivoberger.jmusicbot.client.model.User
import com.ivoberger.jmusicbot.client.model.makeStateMachine
import com.tinder.StateMachine
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import java.util.Calendar

@ExperimentalCoroutinesApi
internal class StateMachineTest {

    private lateinit var stateMachine: StateMachine<State, Event, SideEffect>
    private val state
        get() = stateMachine.state

    private val time = Calendar.getInstance().time
    private val baseUrl = "http://test.url"
    private val user = User("test", id = "testId")
    private val authToken = Auth.Token(
        JWT.create().withIssuer("testIssuer").withIssuedAt(time)
            .withExpiresAt(time + 5 * 60 * 1000 /* add 5 minutes*/)
            .sign(Algorithm.HMAC256("secret"))
    )

    private val serverFoundEvent = Event.ServerFound(baseUrl)
    private val authRequiredEvent = Event.Authorize(user, authToken)

    private val testEvents = listOf(
        Event.StartDiscovery,
        serverFoundEvent,
        authRequiredEvent,
        Event.AuthExpired,
        Event.Disconnect()
    )

    @BeforeEach
    fun setUp() {
        stateMachine = JMusicBot.makeStateMachine()
        JMusicBot.user = null
        JMusicBot.authToken = null
        JMusicBot.baseUrl = null
    }

    @Test
    fun initialState() {
        expectThat(state).isEqualTo(State.Disconnected)
        checkTransitions(listOf(Event.StartDiscovery), State.Disconnected)
        stateMachine.transition(Event.StartDiscovery)
        expectThat(state).isEqualTo(State.Discovering)
    }

    @Test
    fun discoveringState() {
        enterDiscoveringState()
        checkTransitions(listOf(serverFoundEvent, Event.Disconnect()), State.Discovering)
        // server found
        stateMachine.transition(serverFoundEvent)
        expectThat(state).isEqualTo(State.AuthRequired)
        // reset
        setUp()
        // disconnected
        enterDiscoveringState()
        stateMachine.transition(Event.Disconnect())
        expectThat(state).isEqualTo(State.Disconnected)
    }

    @Test
    fun authRequiredState() {
        enterAuthRequiredState()
        checkTransitions(listOf(authRequiredEvent, Event.Disconnect()), State.AuthRequired)
        expectThat(JMusicBot.baseUrl).isEqualTo(baseUrl)
        // authorize
        stateMachine.transition(authRequiredEvent)
        expectThat(state).isEqualTo(State.Connected)
        // reset
        setUp()
        // disconnected
        enterAuthRequiredState()
        stateMachine.transition(Event.Disconnect())
        expectThat(state).isEqualTo(State.Disconnected)
    }

    @Test
    fun connectedState() {
        enterConnectedState()
        checkTransitions(listOf(Event.AuthExpired, Event.Disconnect()), State.Connected)
        expectThat(JMusicBot.baseUrl).isEqualTo(baseUrl)
        expectThat(JMusicBot.user).isEqualTo(user)
        expectThat(JMusicBot.authToken).isEqualTo(authToken)
        // auth expired
        stateMachine.transition(Event.AuthExpired)
        expectThat(state).isEqualTo(State.AuthRequired)
        // reset
        setUp()
        // disconnected
        enterConnectedState()
        stateMachine.transition(Event.Disconnect())
        expectThat(state).isEqualTo(State.Disconnected)
    }

    private fun checkTransitions(validEvents: List<Event>, currentState: State) =
        testEvents.forEach { event ->
            // filter event causing an actual transition
            if (!validEvents.contains(event) && !validEvents.any { it::class != event::class }) {
                stateMachine.transition(event)
                expectThat(state).isEqualTo(currentState)
            }
        }

    fun enterDiscoveringState() {
        stateMachine.transition(Event.StartDiscovery)
    }

    fun enterAuthRequiredState() {
        enterDiscoveringState()
        stateMachine.transition(serverFoundEvent)
    }

    fun enterConnectedState() {
        enterAuthRequiredState()
        stateMachine.transition(authRequiredEvent)
    }
}

