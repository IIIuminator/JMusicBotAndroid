package com.ivoberger.jmusicbot.client.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class UserInfo(
    @Json(name = "name") val name: String,
    @Json(name = "permissions") var permissions: List<Permissions>
)
