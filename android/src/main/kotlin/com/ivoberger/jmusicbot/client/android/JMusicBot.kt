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

import com.ivoberger.jmusicbot.client.JMusicBot
import com.ivoberger.jmusicbot.client.utils.DEFAULT_PORT
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.withContext
import splitties.systemservices.wifiManager

private const val LOCK_TAG = "enq_broadcast"

@ExperimentalCoroutinesApi
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
