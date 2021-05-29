/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hep.dataforge.goals

import hep.dataforge.utils.ReferenceRegistry
import java.util.concurrent.CompletableFuture
import java.util.stream.Stream
import kotlin.streams.toList

/**
 * A goal with no result which is completed when all its dependencies are
 * completed. Stopping this goal does not stop dependencies. Staring goal does start dependencies.
 *
 *
 * On start hooks works only if this group was specifically started. All of its dependencies could be started and completed without triggering it.
 *
 * @author Alexander Nozik
 */
class GoalGroup(private val dependencies: Collection<Goal<*>>) : Goal<Void> {
    private val listeners = ReferenceRegistry<GoalListener<*>>()

    private var res: CompletableFuture<Void> = CompletableFuture
            .allOf(*dependencies.stream().map<CompletableFuture<*>> { it.asCompletableFuture() }.toList().toTypedArray())
            .whenComplete { aVoid, throwable ->
                if (throwable != null) {
                    listeners.forEach { l -> l.onGoalFailed(throwable) }
                } else {
                    listeners.forEach { l -> l.onGoalComplete(null) }
                }
            }

    override fun dependencies(): Stream<Goal<*>> {
        return dependencies.stream()
    }

    override fun run() {
        listeners.forEach { it.onGoalStart() }
        dependencies.forEach { it.run() }
    }

    override fun asCompletableFuture(): CompletableFuture<Void>? {
        return res
    }

    override fun isRunning(): Boolean {
        return dependencies.stream().anyMatch { it.isRunning }
    }

    override fun registerListener(listener: GoalListener<Void>) {
        listeners.add(listener, true)
    }

}
