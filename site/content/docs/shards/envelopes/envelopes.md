---
content_type: "doc_shard"
title: "Envelopes"
chapter: envelope
label: "envelopes"
version: 1.0
date: 08.02.2016
published: true
---

The DataForge functionality is largely based on metadata exchange and therefore the main medium for messages between different parts of the system is `Meta` object and its derivatives. But sometimes one needs not only to transfer metadata but some binary or object data as well. For this DataForge supports an 'Envelope' entity, which contains both meta block and binary data block. Envelope could be automatically serialized to or from a byte stream.

DataForge supports an extensible list of Envelope encoding methods. Specific method is defined by so-called encoding *properties* - the list of key-value pairs that define the specific way the meta and data are encoded. Meta-data itself also could have different encoding. Out of the box DataForge server supports two envelope formats and three meta formats.

<!-- In order to do so one should use an `Envelope` entity. It is a combined format for both text metadata and data in single block. An `Envelope` container consists of three main components: -->

<!-- 0. **Properties**. A set of key-value bindings defining envelope format: metadata format and length, data length and general envelope version. Properties in fact are not considered to be a part of envelope itself, but it is usually a part of envelope container. -->
<!-- 1. **Meta**. A text metadata in any supported format. -->
<!-- 2. **Data**. Ant binary or textual data. The rules to read this data could be derived either from properties header or from envelope meta. -->