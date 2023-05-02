package org.mth.kini

import java.util.*

fun readSample(sampleName: String): String {
    val inputStream = Ini::class.java.getResourceAsStream("samples/$sampleName")
    val builder = StringBuilder()

    inputStream?.let {
        Scanner(it).apply {
            while (hasNext())
                builder.append(nextLine()).append("\n")
        }

    }

    return builder.toString()
}