---
content_type: "doc_shard"
title: "Binary meta"
chapter: envelope
ordering: 50
label: "binary_meta"
published: false
---
Binary meta format is made to limit size and simplify parsing to automatic messages. The notation for the node looks the following way (all numeric values use BigEndian encoding):
                                                                                          
                                                                                          <br>
                                                                                          All strings are encoded in a following way: 2 bytes for string length, byte array of given length representing UTF-8 encoded string.
                                                                                          <br>
                                                                                          
                                                                                          1. Meta name as string.
                                                                                          2. 2 bytes unsigned integer representing the number of values in the node. Could be zero.
                                                                                          3. For each of values an encoded name of the value and then:
                                                                                              * `0` for Null value.
                                                                                              * `T` for `TIME` value, followed by two unsigned long values representing epoch second and nanosecond adjustment.
                                                                                              * `S` for `STRING` value followed by encoded string.
                                                                                              * `D` for double based `NUMBER` followed by double (double encoding specified by [Java serialization](https://docs.oracle.com/javase/8/docs/api/java/io/DataOutput.html#writeDouble-double-)).
                                                                                              * `I` for integer based `NUMBER` followed by signed 4-bytes integer value.
                                                                                              * `B` for [`BigDecimal`](https://docs.oracle.com/javase/8/docs/api/java/math/BigDecimal.html) based `NUMBER` followed by 2-byte unsigned integer defining the number of bytes in representation, bytes themselves and 4-byte scale factor.
                                                                                              * `+` for boolean `true`.
                                                                                              * `-` for boolean `false`.
                                                                                              * `L` for list value and then 2-byte list size followed by sequence of values (as in item 3, but without a name).
                                                                                          4. 2 bytes unsigned integer representing the number of child nodes in this node. Could be zero.
                                                                                          5. For each of child nodes:
                                                                                              * child node name,
                                                                                              * 2-byte unsigned integer representing number of children with this name.
                                                                                              * The sequence of nodes with given name encoding like the root node, but without name.