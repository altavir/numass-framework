---
content_type: "doc_shard"
title: "Configuration"
chapter: "meta"
ordering: 6
label: "configuration"
version: 1.0
date: 27.08.2015
published: true
---
The configuration is a very important extension of basic [Meta](#meta_structure) class. 
It is basically a mutable meta which incorporates 
external observers. It is also important that while simple Meta knows its children knows its children, 
but could be attached freely to any ancestor, configuration has one designated ancestor that is 
notified than configuration is changed.

<hr>
**Note** that putting elements or values to configuration follows the same naming convention as getting from it. 
Meaning putting to `some_name.something` will actually create or put to the node `some_name` if it exists. 
Otherwise, new node is created.
<hr>
