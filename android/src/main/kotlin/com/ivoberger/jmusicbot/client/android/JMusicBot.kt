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
package com.ivoberger.jmusicbot.client.android

import android.net.wifi.WifiManager.MulticastLock
import androidx.lifecycle.LiveData
import androidx.lifecycle.liveData
import com.ivoberger.jmusicbot.client.JMusicBot
import com.ivoberger.jmusicbot.client.model.State
import com.ivoberger.jmusicbot.client.utils.DEFAULT_PORT
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import splitties.systemservices.wifiManager

private const val LOCK_TAG = "enq_broadcast"

/**
 * Discover function for Android, calls [JMusicBot.discoverHost] while setting and releasing the required [MulticastLock]
 */
suspend fun JMusicBot.discoverHostWithMulticastLock(
    knownHost: String? = null,
    port: Int = DEFAULT_PORT
) =
    withContext(Dispatchers.IO) {
        val lock = wifiManager?.createMulticastLock(LOCK_TAG)
        if (lock?.isHeld != false) return@withContext
        lock.acquire()
        discoverHost(knownHost, port)
        lock.release()
    }

private val stateLiveData: LiveData<State> by lazy {
    liveData { for (state in JMusicBot.state) emit(state) }
}

private val playerStateLiveData by lazy {
    liveData { for (playerState in JMusicBot.getPlayerState(startUpdates = false)) emit(playerState) }
}

private val queueLiveData by lazy {
    liveData { for (queue in JMusicBot.getQueue(startUpdates = false)) emit(queue) }
}

/**
 * [LiveData] delivering client state updates
 */
val JMusicBot.stateLiveData
    get() = com.ivoberger.jmusicbot.client.android.stateLiveData

/**
 * [LiveData] delivering player state updates
 * Call [JMusicBot.startPlayerUpdates] to start periodic updates
 */
val JMusicBot.playerStateLiveData
    get() = com.ivoberger.jmusicbot.client.android.playerStateLiveData

/**
 * [LiveData] delivering queue updates
 * Call [JMusicBot.startQueueUpdates] to start periodic updates
 */
val JMusicBot.queueLiveData
    get() = com.ivoberger.jmusicbot.client.android.queueLiveData
