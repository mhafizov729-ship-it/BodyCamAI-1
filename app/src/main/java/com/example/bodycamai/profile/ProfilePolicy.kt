package com.example.bodycamai.profile

import com.example.bodycamai.core.AppProfile
import com.example.bodycamai.core.Module

/** Safe, deterministic defaults for each profile. Users can override modules afterwards. */
object ProfilePolicy {
    fun defaults(profile: AppProfile): Set<Module> = when (profile) {
        AppProfile.BODYCAM -> setOf(Module.AI, Module.PEOPLE, Module.EVENTS, Module.LIVE, Module.MAP, Module.ARCHIVE, Module.DIAGNOSTICS, Module.ENERGY)
        AppProfile.CIVILIAN -> setOf(Module.AI, Module.PEOPLE, Module.VEHICLES, Module.EVENTS, Module.MAP, Module.ARCHIVE, Module.ENERGY)
        AppProfile.POLICE -> setOf(Module.AI, Module.PEOPLE, Module.FACES, Module.VEHICLES, Module.PLATES, Module.OCR, Module.EVENTS, Module.MAP, Module.ARCHIVE, Module.DIAGNOSTICS, Module.ENERGY)
        AppProfile.TACTICAL -> setOf(Module.AI, Module.PEOPLE, Module.VEHICLES, Module.AUDIO, Module.EVENTS, Module.MAP, Module.NIGHT, Module.LIVE, Module.ARCHIVE, Module.ENERGY)
        AppProfile.AIRSOFT -> setOf(Module.AI, Module.PEOPLE, Module.VEHICLES, Module.EVENTS, Module.MAP, Module.AR, Module.GROUP, Module.ARCHIVE, Module.ENERGY)
        AppProfile.TRAINING -> setOf(Module.AI, Module.PEOPLE, Module.EVENTS, Module.ARCHIVE, Module.VOICE, Module.DIAGNOSTICS, Module.ENERGY)
        AppProfile.SEARCH_RESCUE -> setOf(Module.AI, Module.PEOPLE, Module.VEHICLES, Module.EVENTS, Module.MAP, Module.GROUP, Module.SOS, Module.LIVE, Module.ARCHIVE, Module.DIAGNOSTICS, Module.ENERGY)
        AppProfile.XR -> setOf(Module.AI, Module.PEOPLE, Module.VEHICLES, Module.EVENTS, Module.MAP, Module.AR, Module.DRONES, Module.LIVE, Module.VOICE, Module.ENERGY)
        AppProfile.COMMAND -> setOf(Module.AI, Module.EVENTS, Module.DRONES, Module.LIVE, Module.MAP, Module.GROUP, Module.SOS, Module.SYNC, Module.DIAGNOSTICS, Module.ENERGY)
        AppProfile.CUSTOM -> emptySet()
        AppProfile.ALL -> Module.entries.toSet()
    }
}
