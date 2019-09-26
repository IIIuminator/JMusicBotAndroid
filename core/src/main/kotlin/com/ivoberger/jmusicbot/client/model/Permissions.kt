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
package com.ivoberger.jmusicbot.client.model

import com.auth0.jwt.interfaces.Claim
import com.ivoberger.jmusicbot.client.logger
import com.squareup.moshi.FromJson
import com.squareup.moshi.Json
import java.util.Locale

enum class Permissions(val label: String) {
    /**
     * The permission to skip the current song or remove songs from the queue.
     * Note that a user is always allowed to remove a song from the queue which he added himself.
     */
    @Json(name = "skip")
    SKIP("skip"),

    /**
     * The permission to remove songs from the upcoming suggestions of a suggester.
     */
    @Json(name = "dislike")
    DISLIKE("dislike"),

    /**
     * The permission to move songs around in the queue.
     */
    @Json(name = "move")
    MOVE("move"),

    /**
     * Pause/resume current song.
     */
    @Json(name = "pause")
    PAUSE("pause"),

    /**
     * Put new songs into the queue.
     */
    @Json(name = "enqueue")
    ENQUEUE("enqueue"),

    /**
     * Songs enqueued by users without this permission do not affect suggestions.
     */
    @Json(name = "alter_suggestions")
    ALTER_SUGGESTIONS("alter_suggestions"),

    /**
     * Songs enqueued by users without this permission do not affect suggestions.
     */
    @Json(name = "change_volume")
    CHANGE_VOLUME("change_volume");

    companion object {
        fun fromClaims(claims: Map<String, Claim>): MutableList<Permissions> {
            val permissions = mutableListOf<Permissions>()
            claims["permissions"]?.asList(String::class.java)?.forEach {
                try {
                    permissions.add(valueOf(it.toUpperCase(Locale.getDefault())))
                } catch (e: IllegalArgumentException) {
                    logger.error(e) { "Unknown Permission: ${it.toUpperCase(Locale.getDefault())}" }
                }
            }
            logger.debug { "Retrieved permissions from token claims: $permissions" }
            return permissions
        }

        @FromJson
        fun fromJson(permString: String) = valueOf(permString.toUpperCase(Locale.getDefault()))
    }
}
