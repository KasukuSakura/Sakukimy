/*
 * Copyright (c) 2021-2022 KasukuSakura & Contributors. All rights reserved.
 *
 * Use of this source code is governed by the Apache License 2.0 that can be found through the following link.
 *
 * https://github.com/KasukuSakura/Sakukimy/blob/main/LICENSE
 */

package com.kasukusakura.sakukimy.common;

import java.io.File;
import java.net.http.HttpClient;

public class BuildingContext {
    public HttpClient http;
    public File output;
    public boolean settings_dependency_select_server = true;

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public BuildingContext fixup() {
        if (http == null) {
            http = HttpClient.newBuilder()
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .build();
        }
        if (output == null) {
            output = new File("dist");
        }
        output.mkdirs();
        return this;
    }
}
