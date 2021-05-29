package hep.dataforge.actions

import hep.dataforge.context.Context
import hep.dataforge.data.DataFilter
import hep.dataforge.data.DataNode
import hep.dataforge.data.DataSet
import hep.dataforge.data.NamedData
import hep.dataforge.exceptions.AnonymousNotAlowedException
import hep.dataforge.goals.Goal
import hep.dataforge.goals.join
import hep.dataforge.goals.pipe
import hep.dataforge.io.history.Chronicle
import hep.dataforge.meta.Laminate
import hep.dataforge.meta.Meta
import hep.dataforge.meta.MetaBuilder
import hep.dataforge.names.Name
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.plus
import kotlinx.coroutines.runBlocking
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.stream.Collectors
import java.util.stream.Stream


class ActionEnv(val context: Context, val name: String, val meta: Meta, val log: Chronicle)


/**
 * Action environment
 */
class PipeBuilder<T, R>(val context: Context, val actionName: String, var name: String, var meta: MetaBuilder) {
    lateinit var result: suspend ActionEnv.(T) -> R;

    var logger: Logger = LoggerFactory.getLogger("${context.name}[$actionName:$name]")

    /**
     * Calculate the result of goal
     */
    fun result(f: suspend ActionEnv.(T) -> R) {
        result = f;
    }
}

/**
 * Coroutine based pipe action.
 * KPipe supports custom CoroutineContext which allows to override specific way coroutines are created.
 * KPipe is executed inside {@link PipeBuilder} object, which holds name of given data, execution context, meta and log.
 * Notice that name and meta could be changed. Output object receives modified name and meta.
 */
class KPipe<T : Any, R : Any>(
        actionName: String,
        inputType: Class<T>,
        outputType: Class<R>,
        private val action: PipeBuilder<T, R>.() -> Unit) : GenericAction<T, R>(actionName, inputType, outputType) {


    override fun run(context: Context, data: DataNode<out T>, actionMeta: Meta): DataNode<R> {
        if (!this.inputType.isAssignableFrom(data.type)) {
            throw RuntimeException("Type mismatch in action $name. $inputType expected, but ${data.type} received")
        }
        val builder = DataSet.edit(outputType)
        data.dataStream(true).forEach { item ->
            val laminate = Laminate(item.meta, actionMeta)

            val prefix = actionMeta.getString("@namePrefix", "")
            val suffix = actionMeta.getString("@nameSuffix", "")

            val pipe = PipeBuilder<T, R>(
                    context,
                    name,
                    prefix + item.name + suffix,
                    laminate.builder
            ).apply(action)

            val env = ActionEnv(
                    context,
                    pipe.name,
                    pipe.meta,
                    context.history.getChronicle(Name.joinString(pipe.name, name))
            )

            val dispatcher = context + getExecutorService(context, laminate).asCoroutineDispatcher()

            val goal = item.goal.pipe(dispatcher) { goalData ->
                pipe.logger.debug("Starting action ${this.name} on ${pipe.name}")
                pipe.result.invoke(env, goalData).also {
                    pipe.logger.debug("Finished action ${this.name} on ${pipe.name}")
                }
            }
            val res = NamedData(env.name, outputType, goal, env.meta)
            builder.add(res)
        }

        return builder.build();
    }
}


class JoinGroup<T : Any, R : Any>(val context: Context, name: String? = null, internal val node: DataNode<out T>) {
    var name: String = name ?: node.name;
    var meta: MetaBuilder = node.meta.builder

    lateinit var result: suspend ActionEnv.(Map<String, T>) -> R

    fun result(f: suspend ActionEnv.(Map<String, T>) -> R) {
        this.result = f;
    }

}


class JoinGroupBuilder<T : Any, R : Any>(val context: Context, val meta: Meta) {


    private val groupRules: MutableList<(Context, DataNode<out T>) -> List<JoinGroup<T, R>>> = ArrayList();

    /**
     * introduce grouping by value name
     */
    fun byValue(tag: String, defaultTag: String = "@default", action: JoinGroup<T, R>.() -> Unit) {
        groupRules += { context, node ->
            GroupBuilder.byValue(tag, defaultTag).group(node).map {
                JoinGroup<T, R>(context, null, node).apply(action)
            }
        }
    }

    /**
     * Add a single fixed group to grouping rules
     */
    fun group(groupName: String, filter: DataFilter, action: JoinGroup<T, R>.() -> Unit) {
        groupRules += { context, node ->
            listOf(
                    JoinGroup<T, R>(context, groupName, filter.filter(node)).apply(action)
            )
        }
    }

    /**
     * Apply transformation to the whole node
     */
    fun result(resultName: String, f: suspend ActionEnv.(Map<String, T>) -> R) {
        groupRules += { context, node ->
            listOf(JoinGroup<T, R>(context, resultName, node).apply {
                //TODO Meta mutator could be inserted here
                result(f)
            })
        }
    }

    internal fun buildGroups(context: Context, input: DataNode<out T>): Stream<JoinGroup<T, R>> {
        return groupRules.stream().flatMap { it.invoke(context, input).stream() }
    }

}


/**
 * The same rules as for KPipe
 */
class KJoin<T : Any, R : Any>(
        actionName: String,
        inputType: Class<T>,
        outputType: Class<R>,
        private val action: JoinGroupBuilder<T, R>.() -> Unit) : GenericAction<T, R>(actionName, inputType, outputType) {

    override fun run(context: Context, data: DataNode<out T>, actionMeta: Meta): DataNode<R> {
        if (!this.inputType.isAssignableFrom(data.type)) {
            throw RuntimeException("Type mismatch in action $name. $inputType expected, but ${data.type} received")
        }

        val builder = DataSet.edit(outputType)

        JoinGroupBuilder<T, R>(context, actionMeta).apply(action).buildGroups(context, data).forEach { group ->

            val laminate = Laminate(group.meta, actionMeta)

            val goalMap: Map<String, Goal<out T>> = group.node
                    .dataStream()
                    .filter { it.isValid }
                    .collect(Collectors.toMap({ it.name }, { it.goal }))

            val groupName: String = group.name;

            if (groupName.isEmpty()) {
                throw AnonymousNotAlowedException("Anonymous groups are not allowed");
            }

            val env = ActionEnv(
                    context,
                    groupName,
                    laminate.builder,
                    context.history.getChronicle(Name.joinString(groupName, name))
            )

            val dispatcher = context + getExecutorService(context, group.meta).asCoroutineDispatcher()

            val goal = goalMap.join(dispatcher) { group.result.invoke(env, it) }
            val res = NamedData(env.name, outputType, goal, env.meta)
            builder.add(res)
        }

        return builder.build();
    }

}


class FragmentEnv<T : Any, R : Any>(val context: Context, val name: String, var meta: MetaBuilder, val log: Chronicle) {
    lateinit var result: suspend (T) -> R

    fun result(f: suspend (T) -> R) {
        result = f;
    }
}


class SplitBuilder<T : Any, R : Any>(val context: Context, val name: String, val meta: Meta) {
    internal val fragments: MutableMap<String, FragmentEnv<T, R>.() -> Unit> = HashMap()

    /**
     * Add new fragment building rule. If the framgent not defined, result won't be available even if it is present in the map
     * @param name the name of a fragment
     * @param rule the rule to transform fragment name and meta using
     */
    fun fragment(name: String, rule: FragmentEnv<T, R>.() -> Unit) {
        fragments[name] = rule
    }
}

class KSplit<T : Any, R : Any>(
        actionName: String,
        inputType: Class<T>,
        outputType: Class<R>,
        private val action: SplitBuilder<T, R>.() -> Unit) : GenericAction<T, R>(actionName, inputType, outputType) {

    override fun run(context: Context, data: DataNode<out T>, actionMeta: Meta): DataNode<R> {
        if (!this.inputType.isAssignableFrom(data.type)) {
            throw RuntimeException("Type mismatch in action $name. $inputType expected, but ${data.type} received")
        }

        val builder = DataSet.edit(outputType)


        runBlocking {
            data.dataStream(true).forEach {

                val laminate = Laminate(it.meta, actionMeta)

                val split = SplitBuilder<T, R>(context, it.name, actionMeta).apply(action)


                val dispatcher = context + getExecutorService(context, laminate).asCoroutineDispatcher()

                // Create a map of results in a single goal
                //val commonGoal = it.goal.pipe(dispatcher) { split.result.invoke(env, it) }

                // apply individual fragment rules to result
                split.fragments.forEach { name, rule ->
                    val env = FragmentEnv<T, R>(
                            context,
                            name,
                            laminate.builder,
                            context.history.getChronicle(Name.joinString(it.name, name))
                    )

                    rule.invoke(env)

                    val goal = it.goal.pipe(dispatcher, env.result)

                    val res = NamedData(env.name, outputType, goal, env.meta)
                    builder.add(res)
                }
            }
        }

        return builder.build();
    }
}

inline fun <reified T : Any, reified R : Any> DataNode<T>.pipe(context: Context, meta: Meta, name: String = "pipe", noinline action: PipeBuilder<T, R>.() -> Unit): DataNode<R> {
    return KPipe(name, T::class.java, R::class.java, action).run(context, this, meta);
}