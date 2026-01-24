package com.dashinspector.routes

import com.dashinspector.handlers.DatabaseHandler
import io.ktor.server.application.call
import io.ktor.server.routing.*

/**
 * Defines routes for Database API endpoints.
 */
fun Route.databaseRoutes(handler: DatabaseHandler) {
    route("/api/database") {
        get {
            handler.getAllDatabases(call)
        }

        post("/schema") {
            handler.getSchema(call)
        }

        post("/erd") {
            handler.getERD(call)
        }

        post("/query") {
            handler.executeQuery(call)
        }

        route("/table") {
            post("/data") {
                handler.getTableData(call)
            }

            post("/update") {
                handler.updateRow(call)
            }

            post("/delete") {
                handler.deleteRow(call)
            }

            post("/insert") {
                handler.insertRow(call)
            }
        }
    }
}
