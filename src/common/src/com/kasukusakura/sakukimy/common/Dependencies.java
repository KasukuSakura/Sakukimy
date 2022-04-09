/*
 * Copyright (c) 2021-2022 KasukuSakura & Contributors. All rights reserved.
 *
 * Use of this source code is governed by the Apache License 2.0 that can be found through the following link.
 *
 * https://github.com/KasukuSakura/Sakukimy/blob/main/LICENSE
 */

package com.kasukusakura.sakukimy.common;

import java.io.FileNotFoundException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Dependencies {
    public static class RemoteRepo {
        public final URI server;
        public final String name;

        public RemoteRepo(URI server, String name) {
            this.server = server;
            this.name = name;
        }

        public static final RemoteRepo MOJANG_SERVER = new RemoteRepo(
                URI.create("https://libraries.minecraft.net/"),
                "mojang"
        );
        public static final RemoteRepo MAVEN_CENTER = new RemoteRepo(
                URI.create("https://repo1.maven.org/maven2/"),
                "mvn-center"
        );
        public static final RemoteRepo PROXY_ALIYUN = new RemoteRepo(
                URI.create("https://maven.aliyun.com/repository/public/"),
                "aliyun"
        );

        public static final Map<String, RemoteRepo> BUILTIN = Map.of(
                MOJANG_SERVER.name, MOJANG_SERVER,
                MAVEN_CENTER.name, MAVEN_CENTER,
                PROXY_ALIYUN.name, PROXY_ALIYUN
        );
    }

    public static class Dependency {
        public String id;
        public String sha1;
        public long size;

        public static Dependency of(String id) {
            var rsp = new Dependency();
            rsp.id = id;
            return rsp;
        }

        public static Dependency of(String id, long size, String sha1) {
            var rsp = new Dependency();
            rsp.id = id;
            rsp.size = size;
            rsp.sha1 = sha1;
            return rsp;
        }

        public transient RemoteRepo server;
        public transient String jarPath;
        public transient String url;

    }

    public List<RemoteRepo> servers = new ArrayList<>();
    public List<Dependency> dependencies = new ArrayList<>();


    public void fetchData(BuildingContext ctx) throws Exception {
        ctx.fixup();
        if (dependencies == null) return;
        if (dependencies.isEmpty()) return;

        depLoop:
        for (var dep : dependencies) {
            if (dep.jarPath != null) continue;
            // group:artifact:version
            var idx1 = dep.id.indexOf(':');
            var idx2 = dep.id.lastIndexOf(':');

            var group = dep.id.substring(0, idx1);
            var artifact = dep.id.substring(idx1 + 1, idx2);
            var version = dep.id.substring(idx2 + 1);

            var path = group.replace('.', '/') + '/' + artifact + '/' + version + '/' + artifact + '-' + version + ".jar";
            dep.jarPath = path;

            if (ctx.settings_dependency_select_server && servers != null) {
                for (var server : servers) {
                    try {
                        var uri = server.server.resolve(path);
                        var rsp = ctx.http.send(
                                HttpRequest.newBuilder(uri).HEAD().build(),
                                HttpResponse.BodyHandlers.discarding()
                        );
                        if (rsp.statusCode() == 200) {
                            dep.server = server;
                            dep.url = uri.toASCIIString();
                            var headers = rsp.headers();
                            dep.size = headers.firstValueAsLong("Content-Length").orElse(dep.size);
                            dep.sha1 = headers.firstValue("x-checksum-sha1").orElse(dep.sha1);
                            if (dep.sha1 == null) {
                                try {
                                    var data = ctx.http.send(
                                            HttpRequest.newBuilder(
                                                    server.server.resolve(path + ".sha1")
                                            ).GET().build(),
                                            HttpResponse.BodyHandlers.ofString()
                                    );
                                    if (data.statusCode() != 200) {
                                        throw new FileNotFoundException("status: " + data.statusCode() + ", headers=" + data.headers().map());
                                    }
                                    dep.sha1 = data.body().strip();
                                } catch (Throwable e) {
                                    System.out.println("Error when fetching sha1 of " + dep.id + " from " + server.name + " - " + server.server);
                                    e.printStackTrace(System.out);
                                }
                            }
                            continue depLoop;
                        }
                    } catch (Throwable e) {
                        //noinspection ThrowablePrintedToSystemOut
                        System.out.println(e);
                    }
                }
                System.out.println("[WARNING] Failed to fetch metadata of " + dep.id);
            }
        }
    }
}
