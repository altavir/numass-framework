---
content_type: "doc_shard"
title: "Context plugins"
ordering: [10,2]
label: "plugins"
version: 1.0
published: true
---

Plugin system allows to dynamically adjust what modules are used in the specific computation. `Plugin` is an object that could be loaded into the context. Plugins fully support *context inheritance* system, meaning that if requested plugin is not found in current context, the request is moved up to parent context. It is possible to store different instances of the same plugin in child and parent context. In this case actual context plugin will be used (some plugins are able to sense the same plugin in the parent context and use it).

Context can provide plugins either by its type (class) or by string tag, consisting of plugin group, name and version (using gradle-like notation `<group>:<name>:<version>`).

<hr>

**Note:** It is possible to have different plugins implementing the same type in the context. In this case request plugin by type becomes ambiguous. Framework will throw an exception if two ore more plugins satisfy plugin resolution criterion in the same context.

<hr>

A plugin has a mutable state, but is automatically locked alongside with owning context. Plugin resolution by default uses Java SPI tool, but it is possible to implement a plugin repository and use it to load plugins.

<hr>

**Note:** While most of modules provide their own plugins, there is no rule that module has strictly one plugin. Some modules could export a number of plugins, while some of them could export none.

<hr>