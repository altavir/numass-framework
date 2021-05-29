---
content_type: "doc_shard"
title: "Meta-data composition"
chapter: "meta"
ordering: 5
label: "meta-layers"
version: 1.0
published: true
---

An important part of working with meta is *composition*. Let us work with two use-cases:

1. There is a data supplied with meta and one needs modified version of the meta.
2. There is a list of data with the same meta and one needs to change meta only for one data piece.

DataForge provides instruments to easily modify meta (or, since meta is immutable, create a modified instance), but it is not a good solution. A much better way is to use instrument called `Laminate`. Laminate implements meta specification, but stores not one tree of values, but multiple layers of meta. When one requests a Laminate for a value ore meta, it automatically forwards request to the first meta layer. If required node or value is not found in the first layer, the request is forwarded to second layer etc. Laminate also contains meta descriptor, which could be used for default values. Laminate functionality also allows to use information from all of its layers, for example join lists of nodes instead of replacing them. Laminate layers also could be merged together to create classical meta and increase performance. Of course, in this case special features like custom use of all layers is lost.

Using Laminate in case 1 looks like this: if one needs just to change or add some value, one creates a Laminate with initial meta as second layer and override layer containing only values to be changed as first.

For case 2 the solution is even simpler: `DataNode` structures automatically uses laminates to define meta for specific data pieces. So one needs just to define meta for specific data, it will be automatically layered with node meta (or multiple meta elements if node data node structure has many levels).

The typical usage of data layering could be demonstrated on `Actions` (push data flow). Action has 3 arguments: `Context`, `DataNode` and `Meta`. The actual configuration for specific `Data` is defined as a laminate containing data meta layer, node meta layer(s) and action meta layer. Meaning that actual action meta is used only if appropriate positions are not defined in data meta (data knows best about how it should be analyzed).
