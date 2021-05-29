# Purpose #

This repository contains tools and utilities for [Trotsk nu-mass](http://www.inr.ru/~trdat/) experiment.

# Set-up #

This project build using [DataForge](http://www.inr.ru/~nozik/dataforge/) framework. Currently in order to compile numass tools, one need to download dataforge gradle project [here](https://bitbucket.org/Altavir/dataforge). If both projects (numass and dataforge) are in the same directory, everything will work out of the box, otherwise, one needs to edit `gradle.properties` file in the root of numass project and set `dataforgePath` to the relative path of dataforge directory. 

It is intended to fix this problem with public maven repository later.

# Requirements #
Project requires JDK 8 with JavaFX for desktop controls (https://www.azul.com/downloads/?version=java-8-lts&package=jdk-fx)