---
content_type: "doc_shard"
title: "Envelope description"
chapter: envelope
ordering: 20
label: "envelope_description"
published: true
---
Interpretation of envelope contents is basically left for user, but for convenience purposes there is convention for envelope description encoded inside meta block.

The envelope description is placed into hidden `@envelope` meta node. The description could contain following values:

- `@envelope.type` (STRING): Type of the envelope content
- `@envelope.dataType` (STRING): Type of the envelope data
- `@envelope.description` (STRING): Description of the envelope content
- `@envelope.time` (TIME): Time of envelope creation

Both envelope type and data type are supposed to be presented in reversed Internet domain name like java packages.
