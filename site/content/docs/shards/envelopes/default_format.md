---
content_type: "doc_shard"
title: "Streaming envelope format"
chapter: envelope
ordering: 1
label: "envelope_format"
published: true
---

The default envelope format is developed for storage of binary data or transferring data via byte stream. The structure of this format is the following:

* **Tag**. First 20 bytes of file or stream is reserved for envelope properties binary representation:
    1.	`#~` - two ASCII symbols, beginning of binary string.
    2.	4 bytes - properties `type` field: envelope format type and version. For default format the string `DF02` is used, but in principle other envelope types could use the same format.
    3.  2 bytes - properties `metaType` field: metadata encoding type.
    4.	4 bytes - properties `metaLength` field: metadata length in bytes including new lines and other separators.
    5.	4 bytes - properties `dataLength` field: the data length in bytes.
    6.	`~#` -  two ASCII symbols, end of binary string.
    7.	`\r\n` - two bytes, new line.

  The values are read as binary and transformed into 4-byte unsigned tag codes (Big endian).

* **Metadata block**. Metadata in any accepted format. Additional formats could be provided by modules. The default metadata format is *UTF-8* encoded *XML* (tag code 0x584d). *JSON* format is provided by storage module.

  One must note that `metaLength` property is very important and in most cases is mandatory. It could be set to `0xffffffff` or `-1` value in order to force envelope reader to derive meta length automatically, but different readers do it in a different ways, so it strongly not recommended to do it if data block is not empty.

* **Data block**. Any other data. If `dataLength` property is set to `0xffffffff` or `-1`, then it is supposed that data block ends with the end of file or stream. It is discouraged to use infinite data length for streaming data. Data block does not have any limitations for its content. It could even contain envelopes inside it!

### Meta encoding

DataForge server supports following metadata encoding types:

1. **XML** encoding. The full name for this encoding is `XML`, the tag code is `XM`.
2. **JSON** encoding (currently supported only with `storage` module attached). The full name is `JSON`, the tag code is `JS`.
3. **Binary** encoding. DataForge own binary meta representation. The full name is `binary`, the tag code is `BI`.

To avoid confusion. All full names are case insensitive. All meta is supposed to always use **UTF-8** character encoding.