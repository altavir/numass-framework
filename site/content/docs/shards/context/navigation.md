---
content_type: "doc_shard"
title: "Providers and navigation"
ordering: [10,3]
label: "navigation"
version: 1.0
date: 27.08.2015
published: true
---
### Providers
The navigation inside DataForge object hierarchy is done via `Provider` interface. A provider can give access to one of many *targets*. A target is a string that designates specific type of provided object (a type in user understanding, not in computer language one). For given target provider resolves a *name* and returns needed object if it is present (provided).

### Names
The *name* itself could be a plain string or consist of a number of name *tokens* separated by `.` symbol. Multiple name tokens are usually used to describe a name inside tree-like structures. Each of name tokens could also have *query* part enclosed in square brackets: `[]`. The name could look like this:

```
token1.token2[3].token3[key = value]
```

### Paths

A target and a name could be combined into single string using *path* notation: `<target>::<name>`, where `::` is a target separating symbol. If the provided object is provider itself, then one can use *chain path* notation to access objects provided by it. Chain path consists of path segments separated by `/` symbol. Each of the segments is a fully qualified path. The resolution of chain path begins with the first segment and moves forward until path is fully resolved. If some path names not provided or the objects themselves are not providers, the resolution fails.

![Path notation](${img '/naming.svg'})

### Default targets

In order to simplify paths, providers could define default targets. If not target is provided (either path string does not contain `::` or it starts with `::`), the default target is used. Also provider can define default chain target, meaning that this target will be used by default in the next path segment.

For example `Meta` itself is a provider and has two targets: `meta` and `value`. The default target for `Meta` is `meta`, but default chain target is `value`, meaning that chain path `child.grandchild/key` is equivalent of `meta::child.grandchild/value::key` and will point to the value `key` of the node `child.grandchild`.

<hr>

**Note** The same result in case of `Meta` could be achieved by some additional ways:

* `child/meta:grandchild/key` utilizing the fact that each meta in the hierarchy is provider;

* `value::child.grandchild.key` using value search syntax.

Still the path `child.grandchild/key` is the preferred way to access values in meta.

<hr>

### Restrictions and recommendations

Due to chain path notation, there are some restrictions on symbols available in names:

* Symbol `/` is not allowed in names.

* Symbols `[]` are restricted to queries

* Symbol `::` allowed in name only if target is explicitly provided. It is strongly discouraged to use it in name.

* Symbol `.` should be used with care when not using tree structures.

* DataForge uses `UTF-8` encoding, so it is possible to use any non-ASCII symbols in names.


### Implementation

${include 'in_progress.html'}