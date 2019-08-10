package com.ivoberger.jmusicbot.client

import com.ivoberger.jmusicbot.client.model.Auth
import com.ivoberger.jmusicbot.client.model.Event
import com.ivoberger.jmusicbot.client.testUtils.PrintTree
import com.ivoberger.jmusicbot.client.testUtils.enterConnectedState
import com.ivoberger.jmusicbot.client.testUtils.newTestUser
import com.ivoberger.jmusicbot.client.testUtils.toToken
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import timber.log.Timber

@ExperimentalCoroutinesApi
internal class ConnectedTest {

    companion object {
        private lateinit var mMockServer: MockWebServer
        private lateinit var mBaseUrl: String
        private var mPort: Int = 0

        @JvmStatic
        @BeforeAll
        fun setUp() {
            Timber.plant(PrintTree())
        }

        @JvmStatic
        @AfterAll
        fun tearDown() {
            Timber.uprootAll()
        }
    }

    private lateinit var authToken: Auth.Token

    @BeforeEach
    fun testSetUp() {
        mMockServer = MockWebServer()
        mBaseUrl = mMockServer.hostName
        mPort = mMockServer.port
        authToken = Auth.Token(newTestUser.toToken())
        JMusicBot.stateMachine.enterConnectedState(
            Event.ServerFound(mBaseUrl, mPort), Event.Authorize(newTestUser, authToken)
        )
        assertEquals("http://$mBaseUrl:$mPort/", JMusicBot.baseUrl)
        assertEquals(newTestUser, JMusicBot.user)
        assertEquals(authToken, JMusicBot.authToken)
    }

    @AfterEach
    fun testTearDown() {
        JMusicBot.stateMachine.transition(Event.Disconnect())
        mMockServer.shutdown()
    }

    @Test
    fun queueUpdates() = runBlocking {
        
        mMockServer.enqueue(MockResponse().setBody(""))
        for (queue in JMusicBot.getQueue().openSubscription()) {

        }
    }

    @Test
    fun playerUpdates() {
    }
}
