package org.mth.kini

class IniParseException(private val msg: String, private val offset: Int, private val line: Int) : Exception() {
    override val message: String
        get() = "$msg [offset=$offset, line=$line]"
}