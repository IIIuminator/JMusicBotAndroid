package com.ivoberger.jmusicbot.client

import com.ivoberger.jmusicbot.client.testUtils.PrintTree
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import timber.log.Timber

internal class ConnectedTest {

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

    @Test
    fun queueUpdates() {
    }

    @Test
    fun playerUpdates() {
    }
}
