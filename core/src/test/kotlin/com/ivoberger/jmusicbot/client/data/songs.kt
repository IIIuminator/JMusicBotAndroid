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

import com.ivoberger.jmusicbot.client.model.Song

object Songs {
    val californication = Song(
        "1",
        "Californication",
        "Red Hot Chili Peppers",
        null,
        180,
        Provider.gplaymusic
    )
    val nothingElseMatters = Song(
        "2",
        "Nothing Else Matters",
        "Metallica",
        null,
        200,
        Provider.gplaymusic
    )
    val stairwayToHeaven = Song(
        "3",
        "Stairway To Heaven",
        "Led Zeppelin",
        null,
        240,
        Provider.gplaymusic
    )
}
