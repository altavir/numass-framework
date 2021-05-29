/*
 * Copyright  2018 Alexander Nozik.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package hep.dataforge.description

import hep.dataforge.context.Global
import hep.dataforge.exceptions.NameNotFoundException
import hep.dataforge.io.MetaFileReader
import hep.dataforge.io.render
import hep.dataforge.listAnnotations
import hep.dataforge.meta.Meta
import hep.dataforge.meta.MetaBuilder
import hep.dataforge.meta.buildMeta
import hep.dataforge.providers.Path
import hep.dataforge.utils.Misc
import hep.dataforge.values.ValueFactory
import org.slf4j.LoggerFactory
import java.io.IOException
import java.lang.reflect.AnnotatedElement
import java.net.URISyntaxException
import java.nio.file.Paths
import java.text.ParseException
import kotlin.reflect.KAnnotatedElement
import kotlin.reflect.KClass
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberFunctions
import kotlin.reflect.full.memberProperties

object Descriptors {

    private val descriptorCache = Misc.getLRUCache<Any, NodeDescriptor>(1000)

    private fun buildMetaFromResource(name: String, resource: String): MetaBuilder {
        try {
            val file = Paths.get(Descriptors::class.java.classLoader.getResource(resource)!!.toURI())
            return buildMetaFromFile(name, file)
        } catch (ex: IOException) {
            throw RuntimeException("Can't read resource file for descriptor", ex)
        } catch (ex: URISyntaxException) {
            throw RuntimeException("Can't read resource file for descriptor", ex)
        } catch (ex: ParseException) {
            throw RuntimeException("Can't parse resource file for descriptor", ex)
        }

    }

    @Throws(IOException::class, ParseException::class)
    private fun buildMetaFromFile(name: String, file: java.nio.file.Path): MetaBuilder {
        return MetaFileReader.read(file).builder.rename(name)
    }

    /**
     * Find a class or method designated by NodeDef `target` value
     *
     * @param path
     * @return
     */
    private fun findAnnotatedElement(path: Path): KAnnotatedElement? {
        try {
            when {
                path.target.isEmpty() || path.target == "class" -> return Class.forName(path.name.toString()).kotlin
                path.target == "method" -> {
                    val className = path.name.cutLast().toString()
                    val methodName = path.name.last.toString()
                    val dClass = Class.forName(className).kotlin
                    val res = dClass.memberFunctions.find { it.name == methodName }
                    if (res == null) {
                        LoggerFactory.getLogger(Descriptors::class.java).error("Annotated method not found by given path: $path")
                    }
                    return res

                }
                path.target == "property" -> {
                    val className = path.name.cutLast().toString()
                    val methodName = path.name.last.toString()
                    val dClass = Class.forName(className).kotlin
                    val res = dClass.memberProperties.find { it.name == methodName }
                    if (res == null) {
                        LoggerFactory.getLogger(Descriptors::class.java).error("Annotated property not found by given path: $path")
                    }
                    return res
                }
                else -> {
                    LoggerFactory.getLogger(Descriptors::class.java).error("Unknown target for descriptor finder: " + path.target)
                    return null
                }
            }
        } catch (ex: ClassNotFoundException) {
            LoggerFactory.getLogger(Descriptors::class.java).error("Class not fond by given path: $path", ex)
            return null
        }
    }

    private fun KAnnotatedElement.listNodeDefs(): Iterable<NodeDef>{
            return (listAnnotations<NodeDefs>(true).flatMap { it.value.asIterable() } + listAnnotations<NodeDef>(true)).distinctBy { it.key }
    }

    private fun KAnnotatedElement.listValueDefs(): Iterable<ValueDef>{
        return (listAnnotations<ValueDefs>(true).flatMap { it.value.asIterable() } + listAnnotations<ValueDef>(true)).distinctBy { it.key }
    }

    private fun describe(name: String, element: KAnnotatedElement): DescriptorBuilder {
        val reference = element.findAnnotation<Descriptor>()

        if (reference != null) {
            return forReference(name, reference.value).builder()
        }

        val builder = DescriptorBuilder(name)

        //Taking a group annotation if it is present and individual annotations if not
        element.listNodeDefs()
                .filter { it -> !it.key.startsWith("@") }
                .forEach {
                    builder.node(it)
                }

        //Filtering hidden values
        element.listValueDefs()
                .filter { it -> !it.key.startsWith("@") }
                .forEach { valueDef ->
                    builder.value(ValueDescriptor.build(valueDef))
                }

        element.findAnnotation<Description>()?.let {
            builder.info = it.value
        }

        if (element is KClass<*>) {
            element.declaredMemberProperties.forEach { property ->
                try {
                    property.findAnnotation<ValueProperty>()?.let {
                        val propertyName = if (it.name.isEmpty()) {
                            property.name
                        } else {
                            it.name
                        }
                        builder.value(
                                name = propertyName,
                                info = property.description,
                                multiple = it.multiple,
                                defaultValue = ValueFactory.parse(it.def),
                                required = it.def.isEmpty(),
                                allowedValues = if (it.enumeration == Any::class) {
                                    emptyList()
                                } else {
                                    it.enumeration.java.enumConstants.map { it.toString() }
                                },
                                types = it.type.toList()
                        )
                    }

                    property.findAnnotation<NodeProperty>()?.let {
                        val nodeName = if (it.name.isEmpty()) property.name else it.name
                        builder.node(describe(nodeName, property).build())
                    }
                } catch (ex: Exception) {
                    LoggerFactory.getLogger(Descriptors::class.java).warn("Failed to construct descriptor from property {}", property.name)
                }
            }
        }


        return builder
    }

    private fun describe(name: String, element: AnnotatedElement): NodeDescriptor {
        val reference = element.getAnnotation(Descriptor::class.java)

        if (reference != null) {
            return forReference(name, reference.value)
        }
        val builder = DescriptorBuilder(name)

        element.listAnnotations(NodeDef::class.java, true)
                .stream()
                .filter { it -> !it.key.startsWith("@") }
                .forEach { nodeDef ->
                    builder.node(nodeDef)
                }

        //Filtering hidden values
        element.listAnnotations(ValueDef::class.java, true)
                .stream()
                .filter { it -> !it.key.startsWith("@") }
                .forEach { valueDef ->
                    builder.value(ValueDescriptor.build(valueDef))
                }

        return builder.build()
    }

    private val KAnnotatedElement.description: String
        get() = findAnnotation<Description>()?.value ?: ""

    /**
     * Build Meta that contains all the default nodes and values from given node
     * descriptor
     *
     * @param descriptor
     * @return
     */
    @JvmStatic
    fun buildDefaultNode(descriptor: NodeDescriptor): Meta {
        val builder = MetaBuilder(descriptor.name)
        descriptor.valueDescriptors().values.stream().filter { vd -> vd.hasDefault() }.forEach { vd ->
            if (vd.hasDefault()) {
                builder.setValue(vd.name, vd.default)
            }
        }

        descriptor.childrenDescriptors().values.forEach { nd: NodeDescriptor ->
            if (nd.hasDefault()) {
                builder.setNode(nd.name, nd.default)
            } else {
                val defaultNode = buildDefaultNode(nd)
                if (!defaultNode.isEmpty) {
                    builder.setNode(defaultNode)
                }
            }
        }
        return builder
    }


    fun forDef(def: NodeDef): NodeDescriptor {
        val element = when {
            def.type == Any::class -> if (def.descriptor.isEmpty()) {
                null
            } else {
                findAnnotatedElement(Path.of(def.descriptor))
            }
            else -> def.type
        }
        return (element?.let { describe(def.key, it) } ?: DescriptorBuilder(def.key)).apply {
            info = def.info
            multiple = def.multiple
            required = def.required
            this.tags = def.tags.toList()
            def.values.forEach {
                value(it)
            }
        }.build()

    }

    /**
     * Build a descriptor for given Class or Method using Java annotations or restore it from cache if it was already used recently
     *
     * @param element
     * @return
     */
    @JvmStatic
    fun forType(name: String, element: KAnnotatedElement): NodeDescriptor {
        return descriptorCache.getOrPut(element) { describe(name, element).build() }
    }

    @JvmStatic
    fun forJavaType(name: String, element: AnnotatedElement): NodeDescriptor {
        return descriptorCache.getOrPut(element) { describe(name, element) }
    }

    @JvmStatic
    fun forReference(name: String, reference: String): NodeDescriptor {
        return try {
            val path = Path.of(reference)
            when (path.target) {
                "", "class", "method", "property" -> {
                    val target = findAnnotatedElement(path)
                            ?: throw RuntimeException("Target element $path not found")
                    forType(name, target)
                }
                "file" -> descriptorCache.getOrPut(reference) {
                    NodeDescriptor(MetaFileReader.read(Global.getFile(path.name.toString()).absolutePath).builder.setValue("name", name))
                }
                "resource" -> descriptorCache.getOrPut(reference) {
                    NodeDescriptor(buildMetaFromResource("node", path.name.toString()).builder.setValue("name", name))
                }
                else -> throw NameNotFoundException("Cant create descriptor from given target", reference)
            }
        } catch (ex: Exception) {
            LoggerFactory.getLogger(Descriptors::class.java).error("Failed to build descriptor", ex)
            NodeDescriptor(Meta.empty())
        }
    }

    /**
     * Debug function to print all descriptors currently in cache
     */
    fun printDescriptors(){

        Global.output.render(buildMeta {
            descriptorCache.forEach{
                node("descriptor"){
                    "reference" to it.key
                    "value" to it.value.meta
                }
            }
        })
    }

}