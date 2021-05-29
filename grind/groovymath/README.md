# Groovy maths #
This project contains some useful tools to work with mathematical objects using Groovy. Some tools are just thin dynamic layer over [commons-maths library](https://commons.apache.org/proper/commons-math/) and some are completely new. The project is primarily intended for [Beaker](http://beakernotebook.com/) but also will be later used in [GrindStone DataForge module](http://www.inr.ru/~nozik/dataforge/modules.html).


## Groovy table ##
Few groovy objects which allow to easily work with columns and rows. The work with columns should be [Origin](http://www.originlab.com/)-like.

### Column ###
Since the most data manipulation is done with column, the column is the main dynamic structure. One can perform arithmetical operations on columns like `+`, `-` or `*`. The outcome of operation depends on right hand argument. If it is value, it is applied to each value in column, if it is column, then element-by-element operation is performed. More complex operations could be performed by `Column::transform {value, index ->... }` method. 

The values in column could be closures  without parameters`{->...}`. **NOT TESTED**

In addition to data, column has a name and general type. Both optional.

### Table ###
Table is basically a list of columns, which could be accessed by index as well as name. Also table allows to access rows one by one or all together. Row information is not stored in table and accessed on-demand.

### Row ###
Row is immutable list of values (latter it will also contain value names). Rows could be added to table and extracted from table but could not be modified at this moment. Later it will probably be good to replace hard copied rows by soft references to appropriate column values.