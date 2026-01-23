package com.dashinspector.routes

import com.dashinspector.handlers.PreferencesHandler
import io.ktor.server.routing.*

/**
 * Defines routes for SharedPreferences API endpoints.
 */
fun Route.preferencesRoutes(handler: PreferencesHandler) {
    route("/api/preferences") {
        get {
            handler.getAllPreferences(call)
        }

        post("/create") {
            handler.createPreference(call)
        }

        post("/remove") {
            handler.removePreference(call)
        }

        route("/entry") {
            post("/add") {
                handler.addEntry(call)
            }

            post("/update") {
                handler.updateEntry(call)
            }

            post("/remove") {
                handler.removeEntry(call)
            }
        }
    }
}
