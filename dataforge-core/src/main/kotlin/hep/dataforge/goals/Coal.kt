package hep.dataforge.goals

import hep.dataforge.await
import hep.dataforge.context.Context
import hep.dataforge.utils.ReferenceRegistry
import kotlinx.coroutines.*
import kotlinx.coroutines.future.asCompletableFuture
import kotlinx.coroutines.time.withTimeout
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import java.util.stream.Stream

/**
 * Coroutine implementation of Goal
 * @param id - string id of the Coal
 * @param deps - dependency goals
 * @param scope custom coroutine dispatcher. By default common pool
 * @param block execution block. Could be suspending
 */
class Coal<R>(
        val scope: CoroutineScope,
        private val deps: Collection<Goal<*>> = Collections.emptyList(),
        val id: String = "",
        block: suspend () -> R) : Goal<R> {

//    /**
//     * Construct using context
//     */
//    constructor(deps: Collection<Goal<*>> = Collections.emptyList(),
//                context: Context,
//                id: String = "",
//                block: suspend () -> R) : this(context.coroutineContext, deps, id, block)

    private val listeners = ReferenceRegistry<GoalListener<R>>();

    private var deferred: Deferred<R> = scope.async(start = CoroutineStart.LAZY) {
        try {
            notifyListeners { onGoalStart() }
            if (!id.isEmpty()) {
                Thread.currentThread().name = "Goal:$id"
            }
            block.invoke().also {
                notifyListeners { onGoalComplete(it) }
            }
        } catch (ex: Throwable) {
            notifyListeners { onGoalFailed(ex) }
            //rethrow exception
            throw ex
        }
    }

    private fun CoroutineScope.notifyListeners(action: suspend GoalListener<R>.() -> Unit) {
        listeners.forEach {
            scope.launch {
                try {
                    action.invoke(it)
                } catch (ex: Exception) {
                    LoggerFactory.getLogger(javaClass).error("Failed to notify goal listener", ex)
                }
            }
        }
    }


    suspend fun await(): R {
        run()
        return deferred.await();
    }

    override fun run() {
        deps.forEach { it.run() }
        deferred.start()
    }

    override fun get(): R {
        return runBlocking { await() }
    }

    override fun get(timeout: Long, unit: TimeUnit): R {
        return runBlocking {
            withTimeout(Duration.ofMillis(timeout)) { await() }
        }
    }

    override fun cancel(mayInterruptIfRunning: Boolean): Boolean {
        deferred.cancel()
        return true
    }

    override fun isCancelled(): Boolean {
        return deferred.isCancelled;
    }

    override fun isDone(): Boolean {
        return deferred.isCompleted
    }

    override fun isRunning(): Boolean {
        return deferred.isActive
    }

    override fun asCompletableFuture(): CompletableFuture<R> {
        return deferred.asCompletableFuture();
    }

    override fun registerListener(listener: GoalListener<R>) {
        listeners.add(listener, true)
    }

    override fun dependencies(): Stream<Goal<*>> {
        return deps.stream()
    }
}


fun <R> Context.goal(deps: Collection<Goal<*>> = Collections.emptyList(), id: String = "", block: suspend () -> R): Coal<R> {
    return Coal(this, deps, id, block);
}

/**
 * Create a simple generator Coal (no dependencies)
 */
fun <R> Context.generate(id: String = "", block: suspend () -> R): Coal<R> {
    return Coal(this, Collections.emptyList(), id, block);
}

/**
 * Join a uniform list of goals
 */
fun <T, R> List<Goal<out T>>.join(scope: CoroutineScope, block: suspend (List<T>) -> R): Coal<R> {
    return Coal(scope, this) {
        block.invoke(this.map {
            it.await()
        })
    }
}

/**
 * Transform using map of goals as a dependency
 */
fun <T, R> Map<String, Goal<out T>>.join(scope: CoroutineScope, block: suspend (Map<String, T>) -> R): Coal<R> {
    return Coal(scope, this.values) {
        block.invoke(this.mapValues { it.value.await() })
    }
}


/**
 * Pipe goal
 */
fun <T, R> Goal<T>.pipe(scope: CoroutineScope, block: suspend (T) -> R): Coal<R> {
    return Coal(scope, listOf(this)) {
        block.invoke(this.await())
    }
}

fun Collection<Goal<out Any>>.group(): GoalGroup {
    return GoalGroup(this);
}
