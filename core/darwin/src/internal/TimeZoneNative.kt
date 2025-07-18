/*
 * Copyright 2019-2025 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

@file:OptIn(ExperimentalForeignApi::class)

package kotlinx.datetime.internal

import kotlinx.cinterop.*
import kotlinx.datetime.TimeZone
import platform.Foundation.*

internal actual fun timeZoneById(zoneId: String): TimeZone =
    if (tzdb.isSuccess)
        RegionTimeZone(tzdb.getOrThrow().rulesForId(zoneId), zoneId)
    else
        timeZoneByIdFoundation(zoneId)

internal fun timeZoneByIdFoundation(zoneId: String): TimeZone =
    RegionTimeZone(TimeZoneRulesFoundation(zoneId), zoneId)

internal actual fun getAvailableZoneIds(): Set<String> =
    if (tzdb.isSuccess)
        tzdb.getOrThrow().availableTimeZoneIds()
    else
        getAvailableZoneIdsFoundation()

internal fun getAvailableZoneIdsFoundation(): Set<String> =
    NSTimeZone.knownTimeZoneNames.mapTo(mutableSetOf("UTC")) { it.toString() }

private val tzdb = runCatching { TzdbOnFilesystem(Path.fromString(defaultTzdbPath())) }

internal expect fun defaultTzdbPath(): String

internal actual fun currentSystemDefaultZone(): Pair<String, TimeZone?> {
    /* The framework has its own cache of the system timezone. Calls to
    [NSTimeZone systemTimeZone] do not reflect changes to the system timezone
    and instead just return the cached value. Thus, to acquire the current
    system timezone, first, the cache should be cleared.

    This solution is not without flaws, however. In particular, resetting the
    system timezone also resets the default timezone ([NSTimeZone default]) if
    it's the same as the cached system timezone:

        NSTimeZone.defaultTimeZone = [NSTimeZone
            timeZoneWithName: [[NSTimeZone systemTimeZone] name]];
        NSLog(@"%@", NSTimeZone.defaultTimeZone.name);
        NSLog(@"Change the system time zone, then press Enter");
        getchar();
        [NSTimeZone resetSystemTimeZone];
        NSLog(@"%@", NSTimeZone.defaultTimeZone.name); // will also change

    This is a fairly marginal problem:
        * It is only a problem when the developer deliberately sets the default
          timezone to the region that just happens to be the one that the user
          is in, and then the user moves to another region, and the app also
          uses the system timezone.
        * Since iOS 11, the significance of the default timezone has been
          de-emphasized. In particular, it is not included in the API for
          Swift: https://forums.swift.org/t/autoupdating-type-properties/4608/4

    Another possible solution could involve using [NSTimeZone localTimeZone].
    This is documented to reflect the current, uncached system timezone on
    iOS 11 and later:
    https://developer.apple.com/documentation/foundation/nstimezone/1387209-localtimezone
    However:
        * Before iOS 11, this was the same as the default timezone and did not
          reflect the system timezone.
        * Worse, on a Mac (10.15.5), I failed to get it to work as documented.
              NSLog(@"%@", NSTimeZone.localTimeZone.name);
              NSLog(@"Change the system time zone, then press Enter");
              getchar();
              // [NSTimeZone resetSystemTimeZone]; // uncomment to make it work
              NSLog(@"%@", NSTimeZone.localTimeZone.name);
          The printed strings are the same even if I wait for good 10 minutes
          before pressing Enter, unless the line with "reset" is uncommented--
          then the timezone is updated, as it should be. So, for some reason,
          NSTimeZone.localTimeZone, too, is cached.
          With no iOS device to test this on, it doesn't seem worth the effort
          to avoid just resetting the system timezone due to one edge case
          that's hard to avoid.
    */
    NSTimeZone.resetSystemTimeZone()
    return NSTimeZone.systemTimeZone.name to null
}
