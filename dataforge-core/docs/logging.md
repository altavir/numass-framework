---
title: Logging and History 
---

DataForge supports two separate logging system:

1. Generic logging system based on `slf4j` on JVM and possibly any other native logging on remote platform. It is used
for debugging and online information about the process. Logger could be obtained in any point of the program (e.g. via 
`LoggerFactrory.getLogger(...)` in JVM). Critical elements and all `ContextAware` blocks automatically have a pre-defined
logger accessible via `logger` property.

2. Internal logging utilizes hierarchical structure called `History`. Each `Hisotory` object has a reference to `Chronocle`
which stores history entries called `Record`. Also any `History` but the root one which is `Global.history` must have a parent
`History`. Any `Record` entry appended to the history is automatically appended to its parent with appropriate trace element
which specifies where it entry comes from. One can also attach hooks to any chronicle to be triggered on entry addition.
Global history logging behavior is controlled via `Chronicler` context plugin, which is loaded by default into Global context.
History ususally could not be created on sight (only from context chronicler) and should be passed to the process explicitly. 

The key difference between logger and history is that logs are intended for debugging and information and are discarded after 
being read. The history on the other hand is supposed to be the important part of the analysis and should be stored with 
analysis results after it is complete. It is strongly discouraged to use `History` in a performance-sensitive code. Also 
it is bad idea to output  any sensitive information to log since it could be discarded.