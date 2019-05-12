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
package com.ivoberger.client.android

import com.ivoberger.client.JMusicBot
import splitties.systemservices.wifiManager
import timber.log.Timber
import timber.log.debug
import java.io.IOException
import java.net.DatagramPacket
import java.net.InetAddress
import java.net.MulticastSocket

private const val GROUP_ADDRESS = "224.0.0.142"
internal const val PORT = 42945
private const val LOCK_TAG = "enq_broadcast"
private const val SOCKET_TIMEOUT = 4000

internal fun JMusicBot.listenForServerMulticast(): String? {
    val lock = wifiManager?.createMulticastLock(LOCK_TAG)
    if (lock?.isHeld != false) return null
    lock.acquire()
    Timber.debug { "Multicast lock acquired" }
    return try {
        MulticastSocket(PORT).use { socket ->
            val groupAddress = InetAddress.getByName(GROUP_ADDRESS)
            socket.joinGroup(groupAddress)
            socket.soTimeout = SOCKET_TIMEOUT
            val buffer = ByteArray(8)
            val packet = DatagramPacket(buffer, buffer.size)
            socket.broadcast = true
            socket.receive(packet)
            socket.leaveGroup(groupAddress)
            packet.address.hostAddress
        }
    } catch (e: IOException) {
        null
    } finally {
        lock.release()
    }
}
