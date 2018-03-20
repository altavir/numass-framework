package inr.numass.scripts.tristan

import inr.numass.data.storage.ProtoNumassPoint

fun main(args: Array<String>) {

    val file = ProtoNumassPoint.readFile("D:\\Work\\Numass\\data\\TRISTAN_11_2017\\df\\gun_16_19.df ")
    val filtered = file.filter {  it.channel == 4  }


}