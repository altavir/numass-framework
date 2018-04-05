package inr.numass.server

import hep.dataforge.control.DeviceManager
import hep.dataforge.providers.Path
import hep.dataforge.server.*
import hep.dataforge.storage.api.TableLoader
import hep.dataforge.storage.commons.StorageManager
import hep.dataforge.storage.commons.StorageUtils
import hep.dataforge.values.Value
import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.http.ContentType
import io.ktor.response.respondText
import io.ktor.routing.get
import javax.json.JsonObjectBuilder


private suspend fun ApplicationCall.error(type: String, message: String) {
    this.respondText(ContentType("application", "json")) {
        jsonObject {
            add("status", "ERROR")
            add("type", type)
            add("message", message)
        }.render()
    }
}

private suspend fun ApplicationCall.json(json: suspend JsonObjectBuilder.() -> Unit) {
    this.respondText(ContentType("application", "json")) {
        jsonObject(json).add("status", "OK").render()
    }
}

val storageInterceptor = InterceptorFactory { context, meta ->
    val storageManager = context.get(StorageManager::class.java);
    val storage = storageManager.buildStorage(meta);
    ServerInterceptor("storage") {
        get("listStorage") {
            val path = call.request.queryParameters["path"] ?: ""
            val shelf = storage.optShelf(path)
            if (shelf.isPresent) {
                call.json {
                    val loaders = jsonArray();
                    for (loader in StorageUtils.loaderStream(shelf.get())) {
                        loaders.add(jsonObject {
                            add("name", loader.name)
                            add("path", loader.path.toString())
                            add("type", loader.type)
                            add("getMeta", loader.laminate.asJson())
                        })
                    }
                    add("loaders", loaders)
                }
            } else {
                call.error("storage.shelfNotFound", "The shelf with path '$path' not found")
            }
        }
        get("getPlotData") {
            val path = call.request.queryParameters["path"]
            if (path == null) {
                call.error("storage.missingParameter", "Missing request parameter 'path'")
            } else {
                val loaderObject = storage.provide(Path.of(path))
                if (loaderObject.isPresent) {
                    val loader = loaderObject.get();
                    if (loader is TableLoader) {
                        val from = Value.of(call.request.queryParameters["from"] ?: "")
                        val to = Value.of(call.request.queryParameters["to"] ?: "")
                        val maxItems = (call.request.queryParameters["maxItems"] ?: "1000").toInt()
                        call.json {
                            add("path", loader.path.toString())
                            val data = jsonArray()
                            for (point in loader.index.pull(from, to, maxItems)) {
                                data.add(point.asJson())
                            }
                        }
                    } else {
                        call.error("storage.incorrectLoaderType", "Loader $path is not a TableLoader")
                    }
                } else {
                    call.error("storage.loaderNotFound", "Can't find TableLoader  with path = '$path'")
                }
            }
        }
    }
}

val deviceInterceptor = InterceptorFactory { context, meta ->
    val deviceManager = context.get(DeviceManager::class.java);
    ServerInterceptor("devices") {
        get("listDevices") {
            call.json {
                val devices = jsonArray();
                for (name in deviceManager.getDeviceNames()) {
                    val device = deviceManager.optDevice(name).get();
                    devices.add(jsonObject {
                        add("name", name.toUnescaped())
                        add("type", device.type)
                        add("getMeta", device.meta.asJson())
                    })
                }
            }
        }
        get("getDeviceState") {
            val deviceName = call.request.queryParameters["name"] ?: "";
            if (deviceName.isEmpty()) {
                call.error("devices.missingParameter", "Parameter 'name' is required")
            } else {
                val deviceOpt = deviceManager.optDevice(deviceName);
                if (deviceOpt.isPresent) {
                    val device = deviceOpt.get();
                    call.json {
                        add("name", deviceName)
                        add("type", device.type)
                        add("getMeta", device.meta.asJson())
                        add("state", jsonObject {
                            device.states.forEach {
                                add(it.name,it.toString())
                            }
                        })
                    }
                } else {
                    call.error("devices.deviceNotFound", "Device with name: $deviceName not found in the system.")
                }
            }
        }
    }
}

