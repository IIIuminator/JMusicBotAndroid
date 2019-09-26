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

import com.ivoberger.jmusicbot.client.data.PlayerStates
import com.ivoberger.jmusicbot.client.data.Queues
import com.ivoberger.jmusicbot.client.model.Auth
import com.ivoberger.jmusicbot.client.model.Event
import com.ivoberger.jmusicbot.client.model.MoshiTypes
import com.ivoberger.jmusicbot.client.model.PlayerState
import com.ivoberger.jmusicbot.client.model.QueueEntry
import com.ivoberger.jmusicbot.client.testUtils.enterConnectedState
import com.ivoberger.jmusicbot.client.testUtils.newTestUser
import com.ivoberger.jmusicbot.client.testUtils.toToken
import com.squareup.moshi.Moshi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@ExperimentalCoroutinesApi
internal class ConnectedTest {

    companion object {
        private lateinit var mMockServer: MockWebServer
        private lateinit var mBaseUrl: String
        private var mPort: Int = 0
    }

    private lateinit var authToken: Auth.Token
    private val mMoshi = Moshi.Builder().build()
    private val mQueueAdapter = mMoshi.adapter<List<QueueEntry>>(MoshiTypes.Queue)
    private val mPlayerStateAdapter = mMoshi.adapter(PlayerState::class.java)

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
    fun queueUpdates() {
        mMockServer.enqueue(MockResponse().setBody(mQueueAdapter.toJson(Queues.full)))
        mMockServer.enqueue(MockResponse().setBody("invalid JSON"))
        mMockServer.enqueue(MockResponse().setBody(mQueueAdapter.toJson(Queues.halfFull)))
        mMockServer.enqueue(MockResponse().setBody("[]"))

        runBlocking {
            val queueUpdates = JMusicBot.getQueue()
            assertEquals(Queues.full, queueUpdates.receive())
            assertEquals(Queues.halfFull, queueUpdates.receive())
            assertEquals(Queues.empty, queueUpdates.receive())
        }
    }

    @Test
    fun playerUpdates() {
        mMockServer.enqueue(MockResponse().setBody(mPlayerStateAdapter.toJson(PlayerStates.playingCaliforniacation)))
        mMockServer.enqueue(MockResponse().setBody("invalid JSON"))
        mMockServer.enqueue(MockResponse().setBody(mPlayerStateAdapter.toJson(PlayerStates.stopped)))
        mMockServer.enqueue(MockResponse().setBody(mPlayerStateAdapter.toJson(PlayerStates.paused)))

        runBlocking {
            val playerUpdates = JMusicBot.getPlayerState()
            assertEquals(PlayerStates.playingCaliforniacation, playerUpdates.receive())
            assertEquals(PlayerStates.stopped, playerUpdates.receive())
            assertEquals(PlayerStates.paused, playerUpdates.receive())
        }
    }
}
