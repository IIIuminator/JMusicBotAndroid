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
package com.ivoberger.client.di

import com.ivoberger.client.api.MusicBotService
import com.ivoberger.client.model.Auth
import com.ivoberger.client.model.User
import dagger.Subcomponent
import javax.inject.Named

@Subcomponent(modules = [UserModule::class])
internal interface UserSession {
    val user: User
    val authToken: Auth.Token

    @Named(NameKeys.SERVICE_AUTHENTICATED)
    fun musicBotService(): MusicBotService
}
