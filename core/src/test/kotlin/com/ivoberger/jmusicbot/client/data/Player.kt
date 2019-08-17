package com.ivoberger.jmusicbot.client.data

import com.ivoberger.jmusicbot.client.model.PlayerState
import com.ivoberger.jmusicbot.client.model.PlayerStates
import com.ivoberger.jmusicbot.client.model.SongEntry
import com.ivoberger.jmusicbot.client.testUtils.newTestUser

object PlayerStates {
    val playingCaliforniacation =
        PlayerState(
            PlayerStates.PLAY,
            SongEntry(Songs.californication, newTestUser.name),
            50
        )
    val paused = PlayerState(
        PlayerStates.STOP,
        SongEntry(Songs.nothingElseMatters, newTestUser.name),
        80
    )
    val stopped = PlayerState(PlayerStates.STOP)
    val error = PlayerState(PlayerStates.ERROR)
}
