package hep.dataforge.fx

import hep.dataforge.context.Context
import hep.dataforge.context.Global
import hep.dataforge.goals.Coal
import hep.dataforge.goals.Goal
import javafx.application.Platform
import javafx.beans.property.BooleanProperty
import javafx.beans.property.SimpleDoubleProperty
import javafx.beans.property.SimpleStringProperty
import javafx.scene.Node
import javafx.scene.control.ToggleButton
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.layout.Region
import javafx.scene.paint.Color
import javafx.stage.Stage
import kotlinx.coroutines.plus
import tornadofx.*
import java.util.*
import java.util.concurrent.Executor
import java.util.function.BiConsumer
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

val dfIcon: Image = Image(Global::class.java.getResourceAsStream("/img/df.png"))
val dfIconView = ImageView(dfIcon)

val uiExecutor = Executor { command -> Platform.runLater(command) }

class GoalMonitor {
    val titleProperty = SimpleStringProperty("")
    var title: String by titleProperty

    val messageProperty = SimpleStringProperty("")
    var message: String by messageProperty

    val progressProperty = SimpleDoubleProperty(1.0)
    var progress by progressProperty

    val maxProgressProperty = SimpleDoubleProperty(1.0)
    var maxProgress by maxProgressProperty

    fun updateProgress(progress: Double, maxProgress: Double) {
        this.progress = progress
        this.maxProgress = maxProgress
    }
}

private val monitors: MutableMap<UIComponent, MutableMap<String, GoalMonitor>> = HashMap();

/**
 * Get goal monitor for give UI component
 */
fun UIComponent.getMonitor(id: String): GoalMonitor {
    synchronized(monitors) {
        return monitors.getOrPut(this) {
            HashMap()
        }.getOrPut(id) {
            GoalMonitor()
        }
    }
}

/**
 * Clean up monitor
 */
private fun removeMonitor(component: UIComponent, id: String) {
    synchronized(monitors) {
        monitors[component]?.remove(id)
        if (monitors[component]?.isEmpty() == true) {
            monitors.remove(component)
        }
    }
}

fun <R> UIComponent.runGoal(
    context: Context,
    id: String,
    coroutineContext: CoroutineContext = EmptyCoroutineContext,
    block: suspend GoalMonitor.() -> R,
): Coal<R> {
    val monitor = getMonitor(id);
    return Coal(context + coroutineContext, Collections.emptyList(), id) {
        monitor.progress = -1.0
        block(monitor).also {
            monitor.progress = 1.0
        }
    }.apply {
        onComplete { _, _ -> removeMonitor(this@runGoal, id) }
        run()
    }
}

infix fun <R> Goal<R>.ui(action: (R) -> Unit): Goal<R> {
    return this.apply {
        onComplete(uiExecutor, BiConsumer { res, ex ->
            if (res != null) {
                action(res);
            }
            //Always print stack trace if goal is evaluated on UI
            ex?.printStackTrace()
        })
    }
}

infix fun <R> Goal<R>.except(action: (Throwable) -> Unit): Goal<R> {
    return this.apply {
        onComplete(uiExecutor, BiConsumer { _, ex ->
            if (ex != null) {
                action(ex);
            }
        })
    }
}

/**
 * Add a listener that performs some update action on any window size change
 *
 * @param component
 * @param action
 */
fun addWindowResizeListener(component: Region, action: Runnable) {
    component.widthProperty().onChange { action.run() }
    component.heightProperty().onChange { action.run() }
}

fun colorToString(color: Color): String {
    return String.format("#%02X%02X%02X",
        (color.red * 255).toInt(),
        (color.green * 255).toInt(),
        (color.blue * 255).toInt())
}

/**
 * Check if current thread is FX application thread to avoid runLater from
 * UI thread.
 *
 * @param r
 */
fun runNow(r: Runnable) {
    if (Platform.isFxApplicationThread()) {
        r.run()
    } else {
        Platform.runLater(r)
    }
}

/**
 * A display window that could be toggled
 */
class ToggleUIComponent(
    val component: UIComponent,
    val owner: Node,
    val toggle: BooleanProperty,
) {
    val stage: Stage by lazy {
        val res = component.modalStage ?: component.openWindow(owner = owner.scene.window)
        ?: throw RuntimeException("Can'topen window for $component")
        res.showingProperty().onChange {
            toggle.set(it)
        }
        res
    }

    init {
        toggle.onChange {
            if (it) {
                stage.show()
            } else {
                stage.hide()
            }
        }

    }
}

fun UIComponent.bindWindow(owner: Node, toggle: BooleanProperty): ToggleUIComponent {
    return ToggleUIComponent(this, owner, toggle)
}

fun UIComponent.bindWindow(button: ToggleButton): ToggleUIComponent {
    return bindWindow(button, button.selectedProperty())
}

//fun TableView<Values>.table(table: Table){
//
//}