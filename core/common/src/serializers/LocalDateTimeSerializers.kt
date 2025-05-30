/*
 * Copyright 2019-2023 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.serializers

import kotlinx.datetime.*
import kotlinx.datetime.format.DateTimeFormat
import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*

/**
 * A serializer for [LocalDateTime] that uses the ISO 8601 representation.
 *
 * JSON example: `"2007-12-31T23:59:01"`
 *
 * @see LocalDateTime.Formats.ISO
 */
public object LocalDateTimeIso8601Serializer : KSerializer<LocalDateTime>
by LocalDateTime.Formats.ISO.asKSerializer("kotlinx.datetime.LocalDateTime/ISO")

/**
 * A serializer for [LocalDateTime] that represents a value as its components.
 *
 * JSON example: `{"year":2008,"month":7,"day":5,"hour":2,"minute":1}`
 */
public object LocalDateTimeComponentSerializer: KSerializer<LocalDateTime> {

    override val descriptor: SerialDescriptor =
        buildClassSerialDescriptor("kotlinx.datetime.LocalDateTime/components") {
            element<Int>("year")
            element<Short>("month")
            element<Short>("day")
            element<Short>("hour")
            element<Short>("minute")
            element<Short>("second", isOptional = true)
            element<Int>("nanosecond", isOptional = true)
        }

    @OptIn(ExperimentalSerializationApi::class)
    override fun deserialize(decoder: Decoder): LocalDateTime =
        decoder.decodeStructure(descriptor) {
            var year: Int? = null
            var month: Short? = null
            var day: Short? = null
            var hour: Short? = null
            var minute: Short? = null
            var second: Short = 0
            var nanosecond = 0
            loop@while (true) {
                when (val index = decodeElementIndex(descriptor)) {
                    0 -> year = decodeIntElement(descriptor, 0)
                    1 -> month = decodeShortElement(descriptor, 1)
                    2 -> day = decodeShortElement(descriptor, 2)
                    3 -> hour = decodeShortElement(descriptor, 3)
                    4 -> minute = decodeShortElement(descriptor, 4)
                    5 -> second = decodeShortElement(descriptor, 5)
                    6 -> nanosecond = decodeIntElement(descriptor, 6)
                    CompositeDecoder.DECODE_DONE -> break@loop // https://youtrack.jetbrains.com/issue/KT-42262
                    else -> throw SerializationException("Unexpected index: $index")
                }
            }
            if (year == null) throw MissingFieldException(missingField = "year", serialName = descriptor.serialName)
            if (month == null) throw MissingFieldException(missingField = "month", serialName = descriptor.serialName)
            if (day == null) throw MissingFieldException(missingField = "day", serialName = descriptor.serialName)
            if (hour == null) throw MissingFieldException(missingField = "hour", serialName = descriptor.serialName)
            if (minute == null) throw MissingFieldException(missingField = "minute", serialName = descriptor.serialName)
            LocalDateTime(year, month.toInt(), day.toInt(), hour.toInt(), minute.toInt(), second.toInt(), nanosecond)
        }

    override fun serialize(encoder: Encoder, value: LocalDateTime) {
        encoder.encodeStructure(descriptor) {
            encodeIntElement(descriptor, 0, value.year)
            encodeShortElement(descriptor, 1, value.month.number.toShort())
            encodeShortElement(descriptor, 2, value.day.toShort())
            encodeShortElement(descriptor, 3, value.hour.toShort())
            encodeShortElement(descriptor, 4, value.minute.toShort())
            if (value.second != 0 || value.nanosecond != 0) {
                encodeShortElement(descriptor, 5, value.second.toShort())
                if (value.nanosecond != 0) {
                    encodeIntElement(descriptor, 6, value.nanosecond)
                }
            }
        }
    }

}

/**
 * An abstract serializer for [LocalDateTime] values that uses
 * a custom [DateTimeFormat] to serialize and deserialize the value.
 *
 * [name] is the name of the serializer.
 * The [SerialDescriptor.serialName] of the resulting serializer is `kotlinx.datetime.LocalDateTime/serializer/`[name].
 * [SerialDescriptor.serialName] must be unique across all serializers in the same serialization context.
 * When defining a serializer in a library, it is recommended to use the fully qualified class name in [name]
 * to avoid conflicts with serializers defined by other libraries and client code.
 *
 * This serializer is abstract and must be subclassed to provide a concrete serializer.
 * Example:
 * ```
 * // serializes LocalDateTime(2020, 1, 4, 12, 30) as the string "2020-01-04 12:30"
 * object PythonDateTimeSerializer : FormattedLocalDateTimeSerializer("my.package.PythonDateTime",
 *     LocalDateTime.Format {
 *         date(LocalDate.Formats.ISO)
 *         char(' ')
 *         time(LocalTime.Formats.ISO)
 *     }
 * )
 * ```
 *
 * Note that [LocalDateTime] is [kotlinx.serialization.Serializable] by default,
 * so it is not necessary to create custom serializers when the format is not important.
 * Additionally, [LocalDateTimeIso8601Serializer] is provided for the ISO 8601 format.
 */
public abstract class FormattedLocalDateTimeSerializer(
    name: String, format: DateTimeFormat<LocalDateTime>
) : KSerializer<LocalDateTime> by format.asKSerializer("kotlinx.datetime.LocalDateTime/serializer/$name")

/**
 * A serializer for [LocalDateTime] that uses the default [LocalDateTime.toString]/[LocalDateTime.parse].
 *
 * JSON example: `"2007-12-31T23:59:01"`
 */
@PublishedApi
internal object LocalDateTimeSerializer: KSerializer<LocalDateTime> {

    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("kotlinx.datetime.LocalDateTime", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): LocalDateTime =
        LocalDateTime.parse(decoder.decodeString())

    override fun serialize(encoder: Encoder, value: LocalDateTime) {
        encoder.encodeString(value.toString())
    }

}
