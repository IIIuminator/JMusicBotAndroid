package com.ivoberger.jmusicbot.client.data

import com.ivoberger.jmusicbot.client.model.QueueEntry
import com.ivoberger.jmusicbot.client.testUtils.newTestUser
import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalCoroutinesApi
object Queues {
    val filled = listOf(
        QueueEntry(Songs.californication, newTestUser.name),
        QueueEntry(Songs.nothingElseMatters, newTestUser.name),
        QueueEntry(Songs.stairwayToHeaven, newTestUser.name)
    )
    val empty = listOf<QueueEntry>()
}
