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
