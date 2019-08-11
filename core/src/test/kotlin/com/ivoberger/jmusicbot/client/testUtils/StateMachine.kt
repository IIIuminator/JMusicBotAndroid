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
package com.ivoberger.jmusicbot.client.testUtils

import com.ivoberger.jmusicbot.client.model.Event
import com.ivoberger.jmusicbot.client.model.SideEffect
import com.ivoberger.jmusicbot.client.model.State
import com.tinder.StateMachine

fun StateMachine<State, Event, SideEffect>.enterDiscoveringState() =
    transition(Event.StartDiscovery)

fun StateMachine<State, Event, SideEffect>.enterAuthRequiredState(serverFoundEvent: Event.ServerFound) {
    enterDiscoveringState()
    transition(serverFoundEvent)
}

fun StateMachine<State, Event, SideEffect>.enterConnectedState(
    serverFoundEvent: Event.ServerFound,
    authorizeEvent: Event.Authorize
) {
    enterAuthRequiredState(serverFoundEvent)
    transition(authorizeEvent)
}
