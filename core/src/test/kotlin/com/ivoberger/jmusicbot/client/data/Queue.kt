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
package com.ivoberger.jmusicbot.client.data

import com.ivoberger.jmusicbot.client.model.QueueEntry
import com.ivoberger.jmusicbot.client.testUtils.newTestUser
import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalCoroutinesApi
object Queues {
    val full = listOf(
        QueueEntry(Songs.californication, newTestUser.name),
        QueueEntry(Songs.nothingElseMatters, newTestUser.name),
        QueueEntry(Songs.stairwayToHeaven, newTestUser.name),
        QueueEntry(Songs.smokeOnTheWater, newTestUser.name)
    )
    val halfFull = full.slice(2..3)
    val empty = listOf<QueueEntry>()
}
