package com.ivoberger.jmusicbot.client

import com.ivoberger.jmusicbot.client.exceptions.UsernameTakenException
import com.ivoberger.jmusicbot.client.model.Event
import com.ivoberger.jmusicbot.client.testUtils.enterAuthRequiredState
import com.ivoberger.jmusicbot.client.testUtils.testUser
import com.ivoberger.jmusicbot.client.testUtils.testUserName
import com.ivoberger.jmusicbot.client.testUtils.toToken
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import strikt.api.expect
import strikt.api.expectThat
import strikt.api.expectThrows
import strikt.assertions.isEqualTo
import strikt.assertions.isNull
import strikt.assertions.isTrue
import timber.log.Timber

@ExperimentalCoroutinesApi
internal class AuthTest {

    companion object {
        private lateinit var mMockServer: MockWebServer
        private lateinit var mBaseUrl: String
        private var mPort: Int = 0

        @JvmStatic
        @BeforeAll
        fun setUp() {
            Timber.plant(PrintTree())
            mMockServer = MockWebServer()
            mBaseUrl = mMockServer.hostName
            mPort = mMockServer.port
        }

        @JvmStatic
        @AfterAll
        fun tearDown() {
            Timber.uprootAll()
            mMockServer.shutdown()
        }
    }

    @BeforeEach
    fun testSetUp() {
        JMusicBot.stateMachine.enterAuthRequiredState(Event.ServerFound(mBaseUrl, mPort))
        expectThat(JMusicBot.baseUrl).isEqualTo("http://$mBaseUrl:$mPort/")
    }

    @AfterEach
    fun testTearDown() {
        JMusicBot.stateMachine.transition(Event.Disconnect())
    }

    @Test
    fun validRegister() = runBlocking {
        val token = testUser.toToken()
        mMockServer.enqueue(MockResponse().setBody(token))
        JMusicBot.authorize(testUser)
        expect {
            that(JMusicBot.authToken.toString()).isEqualTo(token)
            that(JMusicBot.user?.name).isEqualTo(testUserName)
            that(JMusicBot.user?.permissions).isEqualTo(testUser.permissions)
            that(JMusicBot.state.isConnected).isTrue()
        }
    }

    @Test
    fun invalidRegister() = runBlocking {
        mMockServer.enqueue(MockResponse().setResponseCode(409))
        expectThrows<UsernameTakenException> { JMusicBot.authorize(testUser) }
        expect {
            that(JMusicBot.authToken).isNull()
            that(JMusicBot.user).isNull()
            that(JMusicBot.state.hasServer).isTrue()
        }
    }
}
