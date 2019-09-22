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

import com.ivoberger.jmusicbot.client.exceptions.AuthException
import com.ivoberger.jmusicbot.client.exceptions.NotFoundException
import com.ivoberger.jmusicbot.client.exceptions.UsernameTakenException
import com.ivoberger.jmusicbot.client.model.Auth
import com.ivoberger.jmusicbot.client.model.AuthExpectation
import com.ivoberger.jmusicbot.client.model.AuthExpectationJsonAdapter
import com.ivoberger.jmusicbot.client.model.AuthType
import com.ivoberger.jmusicbot.client.model.Event
import com.ivoberger.jmusicbot.client.model.User
import com.ivoberger.jmusicbot.client.testUtils.PrintTree
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
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
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
        assertEquals("http://$mBaseUrl:$mPort/", JMusicBot.baseUrl)
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
    fun usernameTakenRegister() {
        mMockServer.enqueue(MockResponse().setResponseCode(409))
        assertThrows(UsernameTakenException::class.java) {
            runBlocking { JMusicBot.authorize(newTestUser) }
        }
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
    fun unknownUserLogin() {
        mMockServer.enqueue(MockResponse().setResponseCode(409))
        mMockServer.enqueue(MockResponse().setResponseCode(404))
        val exception = assertThrows(NotFoundException::class.java) {
            runBlocking { JMusicBot.authorize(existingTestUser) }
        }
        assertEquals(NotFoundException.Type.USER, exception.type)
        checkForRegisterRequest()
        checkForLoginRequest(existingTestUser)
        checkForAuthFailure()
    }

    @Test
    fun wrongPasswordLogin() {
        val msg = """{ "format": "Basic", "type": "Full" }"""
        mMockServer.enqueue(MockResponse().setResponseCode(409))
        mMockServer.enqueue(MockResponse().setResponseCode(401).setBody(msg))
        val exception = assertThrows(AuthException::class.java) {
            runBlocking {
                JMusicBot.authorize(existingTestUser)
            }
        }
        assertEquals(AuthException.Reason.NEEDS_AUTH, exception.reason)
        assertEquals(
            AuthExpectation(AuthType.BASIC, "Full", null),
            AuthExpectationJsonAdapter(JMusicBot.mBaseComponent.moshi).fromJson(exception.message!!)
        )

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
    fun authorizeWithWrongUsersToken() {
        val msg = """{ "name": "Wroooong", "permissions" : ["enqueue"] }"""
        val token = newTestUser.toToken()
        mMockServer.enqueue(MockResponse().setResponseCode(200).setBody(msg))
        mMockServer.enqueue(MockResponse().setResponseCode(409))
        assertThrows(UsernameTakenException::class.java) {
            runBlocking { JMusicBot.authorize(newTestUser, Auth.Token(token)) }
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
        assertThrows(UsernameTakenException::class.java) {
            runBlocking { JMusicBot.authorize(newTestUser, Auth.Token(token)) }
        }
        checkForTokenValidityRequest(token)
        checkForRegisterRequest()
        checkForAuthFailure()
    }

    @Test
    fun authorizeWithExpiredToken() {
        mMockServer.enqueue(MockResponse().setResponseCode(409))
        assertThrows(UsernameTakenException::class.java) {
            runBlocking {
                JMusicBot.authorize(newTestUser, Auth.Token(newTestUser.toExpiredToken()))
            }
        }
        checkForRegisterRequest()
        checkForAuthFailure()
    }

    // Util funcs

    private fun checkForAuthSuccess(token: String) {
        assertEquals(token, JMusicBot.authToken.toString())
        assertEquals(testUserName, JMusicBot.user?.name)
        assertEquals(newTestUser.permissions, JMusicBot.user?.permissions)
        assertTrue(JMusicBot.internalState.isConnected)
    }

    private fun checkForAuthFailure() {
        assertNull(JMusicBot.authToken)
        assertNull(JMusicBot.user)
        assertTrue(JMusicBot.internalState.hasServer)
    }

    private fun checkForRegisterRequest() {
        val request = mMockServer.takeRequest()
        assertEquals("/user", request.path)
        assertEquals("POST", request.method)
        assertNull(request.getHeader(KEY_AUTHORIZATION))
    }

    private fun checkForTokenValidityRequest(token: String) {
        val request = mMockServer.takeRequest()
        assertEquals("/user", request.path)
        assertEquals("GET", request.method)
        assertEquals(Auth.Token(token).toAuthHeader(), request.getHeader(KEY_AUTHORIZATION))
    }

    private fun checkForLoginRequest(user: User) {
        val request = mMockServer.takeRequest()
        assertEquals("/token", request.path)
        assertEquals("GET", request.method)
        assertEquals(Auth.Basic(user).toAuthHeader(), request.getHeader(KEY_AUTHORIZATION))
    }

    private fun checkForLoginSequence(user: User, token: String) {
        checkForRegisterRequest()
        checkForLoginRequest(user)
        checkForTokenValidityRequest(token)
    }
}
