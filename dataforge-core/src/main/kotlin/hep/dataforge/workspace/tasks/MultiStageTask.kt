/* 
 * Copyright 2015 Alexander Nozik.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package hep.dataforge.workspace.tasks

import hep.dataforge.data.DataNode
import java.util.*

/**
 * A generic implementation of task with 4 phases:
 *
 *  * gathering
 *  * transformation
 *  * reporting
 *  * result generation
 *
 *
 * @author Alexander Nozik
 */
abstract class MultiStageTask<R : Any>(type: Class<R>) : AbstractTask<R>(type) {

    override fun run(model: TaskModel, data: DataNode<Any>): DataNode<R> {
        val state = MultiStageTaskState(data)
        val logger = model.logger
        //        Work work = getWork(model, data.getName());

        logger.debug("Starting transformation phase")
        //        work.setStatus("Data transformation...");
        transform(model, state)
        if (!state.isFinished) {
            logger.warn("Task state is not finalized. Using last applied state as a result")
            state.finish()
        }
        logger.debug("Starting result phase")

        //        work.setStatus("Task result generation...");

        return result(model, state)
    }

    /**
     * The main task body
     *
     * @param model
     * @param state
     */
    protected abstract fun transform(model: TaskModel, state: MultiStageTaskState): MultiStageTaskState

    /**
     * Generating finish and storing it in workspace.
     *
     * @param state
     * @return
     */
    protected fun result(model: TaskModel, state: MultiStageTaskState): DataNode<R> {
        return state.getResult()!!.checked(type)
    }

    /**
     * The mutable data content of a task.
     *
     * @author Alexander Nozik
     */
    protected class MultiStageTaskState {

        /**
         * list of stages results
         */
        private val stages = LinkedHashMap<String, DataNode<*>>()
        internal var isFinished = false
        /**
         * final finish of task
         */
        private var result: DataNode<*>? = null

        /**
         * Return initial data
         *
         * @return
         */
        val data: DataNode<*>
            get() = getData(INITAIL_DATA_STAGE)!!

        private constructor() {}

        constructor(data: DataNode<*>) {
            this.stages[INITAIL_DATA_STAGE] = data
        }

        fun getData(stage: String): DataNode<*>? {
            return stages[stage]
        }

        fun getResult(): DataNode<*>? {
            return result
        }

        fun setData(stage: String, data: DataNode<*>): MultiStageTaskState {
            if (isFinished) {
                throw IllegalStateException("Can't edit task state after result is finalized")
            } else {
                this.stages[stage] = data
                result = data
                return this
            }
        }

        @Synchronized
        fun finish(result: DataNode<*>): MultiStageTaskState {
            if (isFinished) {
                throw IllegalStateException("Can't edit task state after result is finalized")
            } else {
                this.result = result
                isFinished = true
                return this
            }
        }

        fun finish(): MultiStageTaskState {
            this.isFinished = true
            return this
        }

        companion object {

            private val INITAIL_DATA_STAGE = "@data"
        }

    }
}
