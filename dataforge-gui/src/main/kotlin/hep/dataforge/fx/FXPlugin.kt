package hep.dataforge.fx

import hep.dataforge.context.*
import hep.dataforge.description.ValueDef
import hep.dataforge.description.ValueDefs
import hep.dataforge.meta.Meta
import hep.dataforge.values.ValueType.BOOLEAN
import javafx.application.Application
import javafx.application.Platform
import javafx.collections.FXCollections
import javafx.collections.ObservableSet
import javafx.collections.SetChangeListener
import javafx.scene.Scene
import javafx.stage.Stage
import tornadofx.*

/**
 * Plugin holding JavaFX application instance and its root stage
 * Created by darksnake on 28-Oct-16.
 */
@PluginDef(name = "fx", group = "hep.dataforge", info = "JavaFX window manager")
@ValueDefs(
        ValueDef(key = "consoleMode", type = [BOOLEAN], def = "true", info = "Start an application surrogate if actual application not found")
)
class FXPlugin(meta: Meta = Meta.empty()) : BasicPlugin(meta) {

    private val stages: ObservableSet<Stage> = FXCollections.observableSet()

    val consoleMode: Boolean = meta.getBoolean("consoleMode", true)

    init {
        if (consoleMode) {
            stages.addListener(SetChangeListener { change ->
                if (change.set.isEmpty()) {
                    Platform.setImplicitExit(true)
                } else {
                    Platform.setImplicitExit(false)
                }
            })
        }
    }
    /**
     * Wait for application and toolkit to start if needed
     */
    override fun attach(context: Context) {
        super.attach(context)
        if (FX.getApplication(DefaultScope) == null) {
            if (consoleMode) {
                Thread {
                    context.logger.debug("Starting FX application surrogate")
                    launch<ApplicationSurrogate>()
                }.apply {
                    name = "${context.name} FX application thread"
                    start()
                }

                while (!FX.initialized.get()) {
                    if (Thread.interrupted()) {
                        throw RuntimeException("Interrupted application start")
                    }
                }
                Platform.setImplicitExit(false)
            } else {
                throw RuntimeException("FX Application not defined")
            }
        }
    }

    /**
     * Define an application to use in this context
     */
    fun setApp(app: Application, stage: Stage) {
        FX.registerApplication(DefaultScope, app, stage)
    }

    /**
     * Show something in a pre-constructed stage. Blocks thread until stage is created
     *
     * @param cons
     */
    fun display(action: Stage.() -> Unit) {
        runLater {
            val stage = Stage()
            stage.initOwner(FX.primaryStage)
            stage.action()
            stage.show()
            stages.add(stage)
            stage.setOnCloseRequest { stages.remove(stage) }
        }
    }

    fun display(component: UIComponent, width: Double = 800.0, height: Double = 600.0) {
        display {
            scene = Scene(component.root, width, height)
            title = component.title
        }
    }

    class Factory : PluginFactory() {
        override val type: Class<out Plugin> = FXPlugin::class.java

        override fun build(meta: Meta): Plugin {
            return FXPlugin(meta)
        }
    }

}

/**
 * An application surrogate without any visible primary stage
 */
class ApplicationSurrogate : App() {
    override fun start(stage: Stage) {
        FX.registerApplication(this, stage)
        FX.initialized.value = true
    }
}

fun Context.display(width: Double = 800.0, height: Double = 600.0, component: () -> UIComponent) {
    this.load<FXPlugin>().display(component(), width, height)
}