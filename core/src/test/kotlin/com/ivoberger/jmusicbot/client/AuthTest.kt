package com.ivoberger.jmusicbot.client

import com.ivoberger.jmusicbot.client.exceptions.AuthException
import com.ivoberger.jmusicbot.client.exceptions.NotFoundException
import com.ivoberger.jmusicbot.client.exceptions.UsernameTakenException
import com.ivoberger.jmusicbot.client.model.Auth
import com.ivoberger.jmusicbot.client.model.AuthExpectation
import com.ivoberger.jmusicbot.client.model.AuthExpectationJsonAdapter
import com.ivoberger.jmusicbot.client.model.AuthType
import com.ivoberger.jmusicbot.client.model.Event
import com.ivoberger.jmusicbot.client.model.User
import com.ivoberger.jmusicbot.client.testUtils.enterAuthRequiredState
import com.ivoberger.jmusicbot.client.testUtils.existingTestUser
import com.ivoberger.jmusicbot.client.testUtils.newTestUser
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
        }

        @JvmStatic
        @AfterAll
        fun tearDown() {
            Timber.uprootAll()
        }
    }

    @BeforeEach
    fun testSetUp() {
        mMockServer = MockWebServer()
        mBaseUrl = mMockServer.hostName
        mPort = mMockServer.port
        JMusicBot.stateMachine.enterAuthRequiredState(Event.ServerFound(mBaseUrl, mPort))
        expectThat(JMusicBot.baseUrl).isEqualTo("http://$mBaseUrl:$mPort/")
    }

    @AfterEach
    fun testTearDown() {
        JMusicBot.stateMachine.transition(Event.Disconnect())
        mMockServer.shutdown()
    }

    @Test
    fun validRegister() = runBlocking {
        val token = newTestUser.toToken()
        mMockServer.enqueue(MockResponse().setResponseCode(201).setBody(token))
        JMusicBot.authorize(newTestUser)
        checkForRegisterRequest()
        checkForAuthSuccess(token)
    }

    @Test
    fun usernameTakenRegister() = runBlocking {
        mMockServer.enqueue(MockResponse().setResponseCode(409))
        expectThrows<UsernameTakenException> { JMusicBot.authorize(newTestUser) }
        checkForRegisterRequest()
        checkForAuthFailure()
    }

    @Test
    fun registerWithPassword() = runBlocking {
        val token = newTestUser.toToken()
        mMockServer.enqueue(MockResponse().setResponseCode(201).setBody(token))
    }

    @Test
    fun validLogin() = runBlocking {
        val msg = """{ "name": "$testUserName", "permissions" : ["enqueue"] }"""
        val token = existingTestUser.toToken()
        mMockServer.enqueue(MockResponse().setResponseCode(409))
        mMockServer.enqueue(MockResponse().setBody(token))
        mMockServer.enqueue(MockResponse().setResponseCode(200).setBody(msg))
        JMusicBot.authorize(existingTestUser)
        checkForLoginSequence(existingTestUser, token)
        checkForAuthSuccess(token)
    }

    @Test
    fun unknownUserLogin() = runBlocking {
        mMockServer.enqueue(MockResponse().setResponseCode(409))
        mMockServer.enqueue(MockResponse().setResponseCode(404))
        expectThrows<NotFoundException> { JMusicBot.authorize(existingTestUser) }
            .get { type }.isEqualTo(NotFoundException.Type.USER)
        checkForRegisterRequest()
        checkForLoginRequest(existingTestUser)
        checkForAuthFailure()
    }

    @Test
    fun wrongPasswordLogin() = runBlocking {
        val msg = """{ "format": "Basic", "type": "Full" }"""
        mMockServer.enqueue(MockResponse().setResponseCode(409))
        mMockServer.enqueue(MockResponse().setResponseCode(401).setBody(msg))
        expectThrows<AuthException> { JMusicBot.authorize(existingTestUser) }.and {
            get { reason }.isEqualTo(AuthException.Reason.NEEDS_AUTH)
            get { AuthExpectationJsonAdapter(JMusicBot.mBaseComponent.moshi).fromJson(message!!) }
                .isEqualTo(AuthExpectation(AuthType.BASIC, "Full", null))
        }
        checkForRegisterRequest()
        checkForLoginRequest(existingTestUser)
        checkForAuthFailure()
    }

    @Test
    fun authorizeWithValidToken() = runBlocking {
        val msg = """{ "name": "$testUserName", "permissions" : ["enqueue"] }"""
        val token = newTestUser.toToken()
        mMockServer.enqueue(MockResponse().setResponseCode(200).setBody(msg))
        JMusicBot.authorize(newTestUser, Auth.Token(token))
        checkForTokenValidityRequest(token)
        checkForAuthSuccess(token)
    }

    @Test
    fun authorizeWithWrongUsersToken() = runBlocking {
        val msg = """{ "name": "Wroooong", "permissions" : ["enqueue"] }"""
        val token = newTestUser.toToken()
        mMockServer.enqueue(MockResponse().setResponseCode(200).setBody(msg))
        mMockServer.enqueue(MockResponse().setResponseCode(409))
        expectThrows<UsernameTakenException> {
            JMusicBot.authorize(newTestUser, Auth.Token(token))
        }
        checkForTokenValidityRequest(token)
        checkForRegisterRequest()
        checkForAuthFailure()
    }

    @Test
    fun authorizeWithInvalidToken() = runBlocking {
        val token = newTestUser.toToken()
        mMockServer.enqueue(MockResponse().setResponseCode(401).setBody("Invalid token"))
        mMockServer.enqueue(MockResponse().setResponseCode(409))
        expectThrows<UsernameTakenException> {
            JMusicBot.authorize(newTestUser, Auth.Token(token))
        }
        checkForTokenValidityRequest(token)
        checkForRegisterRequest()
        checkForAuthFailure()
    }

    @Test
    fun authorizeWithExpiredToken() = runBlocking {
        mMockServer.enqueue(MockResponse().setResponseCode(409))
        expectThrows<UsernameTakenException> {
            JMusicBot.authorize(newTestUser, Auth.Token(newTestUser.toExpiredToken()))
        }
        checkForRegisterRequest()
        checkForAuthFailure()
    }

    // Util funcs

    private fun checkForAuthSuccess(token: String) = expect {
        that(JMusicBot.authToken.toString()).isEqualTo(token)
        that(JMusicBot.user?.name).isEqualTo(testUserName)
        that(JMusicBot.user?.permissions).isEqualTo(newTestUser.permissions)
        that(JMusicBot.state.isConnected).isTrue()
    }

    private fun checkForAuthFailure() = expect {
        that(JMusicBot.authToken).isNull()
        that(JMusicBot.user).isNull()
        that(JMusicBot.state.hasServer).isTrue()
    }

    private fun checkForRegisterRequest() = expectThat(mMockServer.takeRequest()).and {
        get { path }.isEqualTo("/user")
        get { method }.isEqualTo("POST")
        get { getHeader(KEY_AUTHORIZATION) }.isNull()
    }

    private fun checkForTokenValidityRequest(token: String) =
        expectThat(mMockServer.takeRequest()).and {
            get { path }.isEqualTo("/user")
            get { method }.isEqualTo("GET")
            get { getHeader(KEY_AUTHORIZATION) }.isEqualTo(Auth.Token(token).toAuthHeader())
        }

    private fun checkForLoginRequest(user: User) = expectThat(mMockServer.takeRequest()).and {
        get { path }.isEqualTo("/token")
        get { method }.isEqualTo("GET")
        get { getHeader(KEY_AUTHORIZATION) }.isEqualTo(Auth.Basic(user).toAuthHeader())
    }

    private fun checkForLoginSequence(user: User, token: String) {
        checkForRegisterRequest()
        checkForLoginRequest(user)
        checkForTokenValidityRequest(token)
    }
}
