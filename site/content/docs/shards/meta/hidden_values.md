---
content_type: "doc_shard"
title: "Hidden values"
chapter: "meta"
ordering: 10
label: "hidden_values"
version: 1.0
published: true
---

Meta structure supports so-called *hidden* values and nodes. These values and nodes exist in the meta and could be requested either by user or framework, but are not shown by casual listing. These values and nodes are considered *system* and are not intended to be defined by user in most cases.

Hidden values and nodes names starts with symbol `@` like `@node`. Not all meta-data representations allow this symbol so in could be replaced by escaped sequence `_at_` in text format (`<_at_node/>` in XML).