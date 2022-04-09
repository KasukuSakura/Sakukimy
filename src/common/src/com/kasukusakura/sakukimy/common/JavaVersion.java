/*
 * Copyright (c) 2021-2022 KasukuSakura & Contributors. All rights reserved.
 *
 * Use of this source code is governed by the Apache License 2.0 that can be found through the following link.
 *
 * https://github.com/KasukuSakura/Sakukimy/blob/main/LICENSE
 */

package com.kasukusakura.sakukimy.common;

public enum JavaVersion {
    VERSION_1_8("jre-legacy",8),
    VERSION_16("java-runtime-alpha", 16),
    VERSION_17("java-runtime-beta", 17),
    ;

    public final String component;
    public final int version;

    JavaVersion(String c, int v) {
        this.component = c;
        this.version = v;
    }
}
