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

import com.squareup.moshi.Types
import java.lang.reflect.Type

sealed class MoshiTypes {
    companion object {
        val Queue: Type = Types.newParameterizedType(List::class.java, QueueEntry::class.java)
        val SongList: Type = Types.newParameterizedType(List::class.java, Song::class.java)
        val UserList: Type = Types.newParameterizedType(List::class.java, User::class.java)
    }
}
