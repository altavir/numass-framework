---
content_type: "doc_shard"
title: "Control plugin"
ordering: [2000]
label: "control"
version: 1.0
date: 08.02.2016
published: true
---
DataForge control subsystem defines a general api for data acquisition processes. It could be used to issue commands to devices, read data and communicate with storage system or other devices.

The center of control API is a `Device` class.
The device has following important features:
<ul>
    <li>
        <strong>States:</strong> each device has a number of states that could be accessed by `getStatus` method. States could be either stored as some internal variables or calculated on demand. States calculation is synchronous!
    </li>
    <li>
        <strong>Listeners:</strong> some external class which listens device state changes and events. By default listeners are represented by weak references so they could be finalized any time if not used.
    <li>
        <strong>Connections:</strong> any external device connectors which are used by device. The difference between listener and connection is that device is obligated to notify all registered listeners about all changes, but connection is used by device at its own discretion. Also usually only one connection is used for each single purpose.
    </li>
</ul>
