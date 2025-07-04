/*
 * Copyright 2019-2024 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

@file:Suppress("PackageDirectoryMismatch")
package kotlinx.datetime

import java.io.*

@PublishedApi // changing the class name would result in serialization incompatibility
internal class Ser(private var typeTag: Int, private var value: Any?) : Externalizable {
    constructor() : this(0, null)

    override fun writeExternal(out: ObjectOutput) {
        out.writeByte(typeTag)
        val value = this.value
        when (typeTag) {
            DATE_TAG -> {
                value as LocalDate
                out.writeLong(value.value.toEpochDay())
            }
            TIME_TAG -> {
                value as LocalTime
                out.writeLong(value.toNanosecondOfDay())
            }
            DATE_TIME_TAG -> {
                value as LocalDateTime
                out.writeLong(value.date.value.toEpochDay())
                out.writeLong(value.time.toNanosecondOfDay())
            }
            UTC_OFFSET_TAG -> {
                value as UtcOffset
                out.writeInt(value.totalSeconds)
            }
            YEAR_MONTH_TAG -> {
                value as YearMonth
                out.writeLong(value.toEpochMonths())
            }
            else -> throw IllegalStateException("Unknown type tag: $typeTag for value: $value")
        }
    }

    override fun readExternal(`in`: ObjectInput) {
        typeTag = `in`.readByte().toInt()
        value = when (typeTag) {
            DATE_TAG ->
                LocalDate(java.time.LocalDate.ofEpochDay(`in`.readLong()))
            TIME_TAG ->
                LocalTime.fromNanosecondOfDay(`in`.readLong())
            DATE_TIME_TAG ->
                LocalDateTime(
                    LocalDate(java.time.LocalDate.ofEpochDay(`in`.readLong())),
                    LocalTime.fromNanosecondOfDay(`in`.readLong())
                )
            UTC_OFFSET_TAG ->
                UtcOffset(seconds = `in`.readInt())
            YEAR_MONTH_TAG ->
                YearMonth.fromEpochMonths(`in`.readLong())
            else -> throw IOException("Unknown type tag: $typeTag")
        }
    }

    private fun readResolve(): Any = value!!

    companion object {
        private const val serialVersionUID: Long = 0L
        const val DATE_TAG = 2
        const val TIME_TAG = 3
        const val DATE_TIME_TAG = 4
        const val UTC_OFFSET_TAG = 10
        const val YEAR_MONTH_TAG = 11
    }
}
