/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package inr.numass.scripts

import hep.dataforge.io.ColumnedDataReader
import hep.dataforge.io.ColumnedDataWriter
import hep.dataforge.tables.Table

File file = new File("D:\\Work\\Numass\\sterile2016\\empty.dat" )
Table referenceTable = new ColumnedDataReader(file).toTable();
ColumnedDataWriter.writeTable(System.out, referenceTable,"")