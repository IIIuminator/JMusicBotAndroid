package com.ivoberger.jmusicbot.client

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.github.debop.javatimes.plus
import com.ivoberger.jmusicbot.client.model.Auth
import com.ivoberger.jmusicbot.client.model.Event
import com.ivoberger.jmusicbot.client.model.State
import com.ivoberger.jmusicbot.client.model.User
import com.ivoberger.jmusicbot.client.model.makeStateMachine
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import java.util.Calendar

@ExperimentalCoroutinesApi
internal class StateMachineTest {

    private val stateMachine = JMusicBot.makeStateMachine()
    private val state
        get() = stateMachine.state
    private val time = Calendar.getInstance().time

    private val baseUrl = "http://test.url"
    private val user = User("test", id = "testId")
    private val token =
        JWT.create().withIssuer("testIssuer").withIssuedAt(time)
            .withExpiresAt(time + 5 * 60 * 1000 /* add 5 minutes*/)
            .sign(Algorithm.HMAC256("secret"))

    private val testEvents =
        listOf(
            Event.StartDiscovery,
            Event.ServerFound(baseUrl),
            Event.Authorize(user, Auth.Token(token)),
            Event.AuthExpired,
            Event.Disconnect()
        )

    @Test
    fun stateMachineTest() {
        // test initial state
        expectThat(state).isEqualTo(State.Disconnected)
        checkInvalidTransitions(Event.StartDiscovery, State.Disconnected)

        stateMachine.transition(Event.StartDiscovery)
        expectThat(state).isEqualTo(State.Discovering)
    }

    private fun checkInvalidTransitions(validEvent: Event, validState: State) =
        testEvents.forEach { event ->
            if (validEvent::class != event::class) {
                stateMachine.transition(event)
                expectThat(state).isEqualTo(validState)
            }
        }
}

