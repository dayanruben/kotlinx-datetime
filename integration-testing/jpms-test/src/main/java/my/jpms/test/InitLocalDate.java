/*
 * Copyright 2019-2025 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package my.jpms.test;

import kotlinx.datetime.*;

public class InitLocalDate {
    LocalDate newLocalDate() {
        return new LocalDate(2025, 3, 31);
    }
}
