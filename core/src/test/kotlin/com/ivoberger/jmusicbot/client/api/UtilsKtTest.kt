package com.ivoberger.jmusicbot.client.api

import com.ivoberger.jmusicbot.client.exceptions.InvalidParametersException
import com.ivoberger.jmusicbot.client.model.VersionInfo
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runBlockingTest
import okhttp3.MediaType
import okhttp3.ResponseBody
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import retrofit2.Response

@ExperimentalCoroutinesApi
internal class UtilsKtTest {

    @Test
    fun withToken() {
    }

    @Test
    fun processResponseNotNull() = runBlockingTest {
        val versionInfo = VersionInfo(
            "0.1.0",
            VersionInfo.ImplementationInfo(
                "testImpl",
                "v1.0.0"
            )
        )
        val response = async { Response.success(versionInfo) }
        assertEquals(versionInfo, response.process())
    }

    @Test
    fun processResponseNull() = runBlockingTest {
        val response = async { Response.success<VersionInfo>(null) }
        assertEquals(null, response.process())
    }

    @Test
    fun processError400() = runBlockingTest {
        val response = async {
            Response.error<VersionInfo>(
                400,
                ResponseBody.create(MediaType.get("application/json"), "")
            )
        }
        assertThrows(InvalidParametersException::class.java) {
            runBlockingTest { response.process() }
        }
    }
}
