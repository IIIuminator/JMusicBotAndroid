package com.ivoberger.jmusicbot.client

import com.ivoberger.jmusicbot.client.exceptions.AuthException
import com.ivoberger.jmusicbot.client.exceptions.NotFoundException
import com.ivoberger.jmusicbot.client.exceptions.UsernameTakenException
import com.ivoberger.jmusicbot.client.model.Auth
import com.ivoberger.jmusicbot.client.model.AuthExpectation
import com.ivoberger.jmusicbot.client.model.AuthExpectationJsonAdapter
import com.ivoberger.jmusicbot.client.model.AuthType
import com.ivoberger.jmusicbot.client.model.Event
import com.ivoberger.jmusicbot.client.testUtils.enterAuthRequiredState
import com.ivoberger.jmusicbot.client.testUtils.testUser
import com.ivoberger.jmusicbot.client.testUtils.testUserName
import com.ivoberger.jmusicbot.client.testUtils.toExpiredToken
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
        mMockServer.enqueue(MockResponse().setResponseCode(201).setBody(token))
        JMusicBot.authorize(testUser)
        checkForAuthSuccess(token)
    }

    @Test
    fun usernameTakenRegister() = runBlocking {
        mMockServer.enqueue(MockResponse().setResponseCode(409))
        expectThrows<UsernameTakenException> { JMusicBot.authorize(testUser) }
        checkForAuthFailure()
    }

    @Test
    fun validLogin() = runBlocking {
        val token = testUser.toToken()
        val user = testUser.copy().apply { password = "password" }
        mMockServer.enqueue(MockResponse().setResponseCode(409))
        mMockServer.enqueue(MockResponse().setBody(token))
        JMusicBot.authorize(user)
        checkForAuthSuccess(token)
    }

    @Test
    fun unknownUserLogin() = runBlocking {
        val user = testUser.copy().apply { password = "password" }
        mMockServer.enqueue(MockResponse().setResponseCode(409))
        mMockServer.enqueue(MockResponse().setResponseCode(404))
        expectThrows<NotFoundException> { JMusicBot.authorize(user) }
            .get { type }.isEqualTo(NotFoundException.Type.USER)
        checkForAuthFailure()
    }

    @Test
    fun wrongPasswordLogin() = runBlocking {
        val msg = """{ "format": "Basic", "type": "Full" }"""
        val user = testUser.copy().apply { password = "password" }
        mMockServer.enqueue(MockResponse().setResponseCode(409))
        mMockServer.enqueue(MockResponse().setResponseCode(401).setBody(msg))
        expectThrows<AuthException> { JMusicBot.authorize(user) }.and {
            get { reason }.isEqualTo(AuthException.Reason.NEEDS_AUTH)
            get { AuthExpectationJsonAdapter(JMusicBot.mBaseComponent.moshi).fromJson(message!!) }
                .isEqualTo(AuthExpectation(AuthType.BASIC, "Full", null))
        }
        checkForAuthFailure()
    }

    @Test
    fun authorizeWithValidToken() = runBlocking {
        val msg = """{ "name": "$testUserName", "permissions" : ["enqueue"] }"""
        val token = testUser.toToken()
        mMockServer.enqueue(MockResponse().setResponseCode(200).setBody(msg))
        JMusicBot.authorize(testUser, Auth.Token(token))
        checkForAuthSuccess(token)
    }

    @Test
    fun authorizeWithWrongUsersToken() = runBlocking {
        val msg = """{ "name": "Wroooong", "permissions" : ["enqueue"] }"""
        mMockServer.enqueue(MockResponse().setResponseCode(200).setBody(msg))
        mMockServer.enqueue(MockResponse().setResponseCode(409))
        expectThrows<UsernameTakenException> {
            JMusicBot.authorize(testUser, Auth.Token(testUser.toToken()))
        }
        checkForAuthFailure()
    }

    @Test
    fun authorizeWithInvalidToken() = runBlocking {
        mMockServer.enqueue(MockResponse().setResponseCode(401).setBody("Invalid token"))
        mMockServer.enqueue(MockResponse().setResponseCode(409))
        expectThrows<UsernameTakenException> {
            JMusicBot.authorize(testUser, Auth.Token(testUser.toToken()))
        }
        checkForAuthFailure()
    }

    @Test
    fun authorizeWithExpiredToken() = runBlocking {
        mMockServer.enqueue(MockResponse().setResponseCode(409))
        expectThrows<UsernameTakenException> {
            JMusicBot.authorize(testUser, Auth.Token(testUser.toExpiredToken()))
        }
        checkForAuthFailure()
    }

    // Util funcs

    private fun checkForAuthSuccess(token: String) = expect {
        that(JMusicBot.authToken.toString()).isEqualTo(token)
        that(JMusicBot.user?.name).isEqualTo(testUserName)
        that(JMusicBot.user?.permissions).isEqualTo(testUser.permissions)
        that(JMusicBot.state.isConnected).isTrue()
    }

    private fun checkForAuthFailure() = expect {
        that(JMusicBot.authToken).isNull()
        that(JMusicBot.user).isNull()
        that(JMusicBot.state.hasServer).isTrue()
    }
}
