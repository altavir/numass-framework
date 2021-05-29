---
content_type: "doc_shard"
title: "Tagless envelope format"
chapter: envelope
ordering: 2
label: "tagless_format"
version: 1.0
published: true
---
Tagless format is developed to store textual data without need for binary block at the beginning. It is not recommended to use it for binary data or for streaming. The structure of this format is the following:


1. **The header line**. `#~DFTL~#`. The line is used only for identify the DataForge envelope. Standard reader is also configured to skip any lines starting with # before this line, so it is compatible with [shebang](https://en.wikipedia.org/wiki/Shebang_(Unix)). All header lines in tagless format must have at least new line `\n` character after them (DOS/Windows new line `\r\n` is also supported).

2. **Properties**. Properties are defined in a textual form. Each property is defined in its own lined in a following way:

    ```#? <property key> : <property value>; <new line>```

    Any whitespaces before `<property value>` begin are ignored. The `;` symbol is optional, but everything after it is ignored. Every property **must** be on a separate line. The end of line is defined by `\n` character so both Windows and Linux line endings are valid.
    Properties are accepted both in their textual representation or tag code.

3. **Meta block start**. Meta block start string is defined by `metaSeparator` property. The default value is `#~META~#`. Meta block start could be omitted if meta is empty.
4. **Meta block**. Everything between meta block start and data block start (or end of file) is treated like meta. `metaLength` property is ignored. It is not recommended to use binary meta encoding in this format.
5. **Data start block**. Data block start string is defined by `dataSeparator` property. The default value is `#~DATA~#`. Data block start could be omitted if data is empty.
6. **Data block**. The data itself. If `dataLength` property is defined, then it is used to trim the remaining bytes in the stream or file. Otherwise the end of stream or file is used to define the end of data.