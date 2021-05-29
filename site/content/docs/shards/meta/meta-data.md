---
content_type: "doc_shard"
title: "Meta-data processor"
chapter: "meta"
label: "meta"
version: 2.0
date: 27.08.2015
published: true
---
The main and the most distinguishing feature of DataForge framework is a concept of data analysis as a meta-data processor.
First of all, one need to define main terms:

* **Data**. Any information from experiment or simulation provided by user. The data format is not governed by DataForge so it could be presented in any form. The important point concerning data is that data is by default immutable. Meaning the program could create its modified copies (which is not recommended), but could not modify initial input in any way.

* **Meta-data**. Meta-data contrary to data has a fixed internal representation provided by Meta object. Meta is simple [tree-like object](#meta_structure) that can conveniently store values and Meta nodes. Meta-data is either provided by user or generated in analysis process. Inside the analysis process Meta is immutable, but for some purposes, changeable meta nodes could be used during analysis configuration and results representation (see [Configuration](#configuration)).

The fundamental point of the whole DataForge philosophy is that every external input could be either data or meta-data. This point could seem dull at first glance, but in fact, it have a number of very important consequences:

* *No scripts*. DataForge encourages user not to use imperative code to manipulate data. Any procedure to be performed on data should be presented either by some hard-coded meta-data processor rule (the function that takes data and metadata and produces some output without any additional external information) or as declarative process definition in form of meta-data. Since the most of data analysis nowadays is made by different scripts, the loss of scripting capability could seem to be a tremendous blow to framework functionality, but in fact every thing one could do with script, one also can do with declaration and declaration processing rules. Also purely declarative description allows for easy error check and analysis scaling (via parallelism).

    <hr>

    **Note:** Those, who still like scripting, can still either use DataForge as a library, or use [GRIND](#grind)

    <hr>

* *Automatic scaling*. Since particular analysis of some piece of data depends only on that piece and on it's meta-data, such analysis could be easily scaled for any number of such data pieces.

* *Meta composition*. Since the configuration of analysis and meta-data ara both have fixed representation, one could easily combine specific analysis configuration from different parts taken from different sources. For example, one could have a general configuration for the whole data set and specific changes for some specific point. One do not have to write a specific script to work with this particular point, just override its meta-data!

* *No global state*. Since neither data, nor meta-data account for global states, there are no global states! The closest to the global states as you get is context variables (see [context](#context)), which are used to connect analysis process to system environment, but these states are not used in the analysis itself.

* *Automatic data flow organisation*. Since user does not in general control the order and time of actions performed by the framework, the work of arranging data flow, result caching, etc. could be actually done by the framework itself.