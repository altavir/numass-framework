---
content_type: "doc_shard"
title: "Context"
ordering: [10,1]
label: "context"
version: 1.0
date: 27.08.2015
published: true
---

The important part of DataForge architecture is the context encapsulation. One of the major problems hindering parallel applications development is existence of mutable global states and environment variables. One of the possible solutions to this problem is to run any process in its own personal sandbox carrying copies of all required values, another one is to make all environment values immutable. Another problem in modular system is dynamic loading of modules and changing of their states.

In DataForge all these problems are solved by `Context` object (the idea is inspired by Android platform contexts). Context holds all global states (called *context properties*), but could in fact be different for different processes. Most of complicated actions require a context as a parameter. Context not only stores values, but also works as a base for [plugin system](#plugin) and could be used as a [dependency injection](https://en.wikipedia.org/wiki/Dependency_injection) base. Context does not store copies of global values and plugins, instead one context could inherit from another. When some code requests value or plugin from the context, the framework checks if this context contains required feature. If it is present, it is returned. If not and context has a parent, then parent will be requested for the same feature.

There is only one context that is allowed not to have parent. It is a [singleton](https://en.wikipedia.org/wiki/Singleton_pattern) called `Global`. Global inherits its values directly form system environment. It is possible to load plugins directly to Global (in this case they will be available for all active context), though it is discouraged in large projects. Each context has its own unique name used for distinction and logging.

The runtime immutability of context is supported via *context locks*. Any runtime object could request to lock context and forbid any changes done to the context. After critical runtime actions are done, object can unlock context back. Context supports unlimited number of simultaneous locks, so it won't be mutable until all locking objects would release it. If one needs to change something in the context while it is locked, the only way to do so is to create an unlocked child context (*fork* it) and work with it.