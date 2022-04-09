/*
 * Copyright (c) 2021-2022 KasukuSakura & Contributors. All rights reserved.
 *
 * Use of this source code is governed by the Apache License 2.0 that can be found through the following link.
 *
 * https://github.com/KasukuSakura/Sakukimy/blob/main/LICENSE
 */

package com.kasukusakura.sakukimy.common;

import java.io.*;
import java.security.MessageDigest;
import java.util.Optional;
import java.util.function.Consumer;

public class Kit {
    private static final String HEX = "0123456789abcdef";

    public static Consumer<Optional<byte[]>> writeTo(PrintStream out) {
        return data -> {
            if (data.isPresent()) {
                var raw = data.get();
                out.write(raw, 0, raw.length);
            }
        };
    }

    public static String sha1(File application) throws Exception {
        var md = MessageDigest.getInstance("SHA-1");
        try (var fis = new FileInputStream(application)) {
            fis.transferTo(new OutputStream() {
                @Override
                public void write(int b) throws IOException {
                    md.update((byte) b);
                }

                @Override
                public void write(byte[] b, int off, int len) throws IOException {
                    md.update(b, off, len);
                }
            });
        }
        var rsp = md.digest();
        return hex(rsp);
    }

    public static String hex(byte[] data) {
        var rsp = new StringBuilder(data.length * 2);
        for (var byte0 : data) {
            var hex = Integer.toHexString(((int) byte0) & 0xFF);
            if (hex.length() == 1) {
                rsp.append('0');
            }
            rsp.append(hex);
        }
        return rsp.toString();
    }

}
