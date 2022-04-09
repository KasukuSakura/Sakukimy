/*
 * Copyright (c) 2021-2022 KasukuSakura & Contributors. All rights reserved.
 *
 * Use of this source code is governed by the Apache License 2.0 that can be found through the following link.
 *
 * https://github.com/KasukuSakura/Sakukimy/blob/main/LICENSE
 */

package com.kasukusakura.sakukimy.common;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.internal.Streams;
import com.google.gson.stream.JsonWriter;

import java.io.File;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

public class ProjectPacker {
    public JavaVersion runtime = JavaVersion.VERSION_17;
    public Instant releaseTime = Instant.now();
    public String id = "cus-release-" + System.currentTimeMillis();


    public static class Assets {
        public String url = "https://static-assets.kasukusakura.com/mc/asset/17988dc545309ff5b913c7045b8599c922adffd6/sakukimy.json";
        public String name = "sakukimy";
        public String sha1 = "17988dc545309ff5b913c7045b8599c922adffd6";
        public long size = 20L;
        public long totalSize = 20L;
    }

    public Assets assets = new Assets();

    public String mainClass = "";
    public File application;
    public Dependencies dependencies;


    @SuppressWarnings("ResultOfMethodCallIgnored")
    public void build(BuildingContext ctx) throws Exception {
        var output = new File(ctx.fixup().output, id);
        if (dependencies != null) {
            dependencies.fetchData(ctx);
        }
        output.mkdirs();

        var metadata = new JsonObject();
        {
            var args = new JsonObject();
            metadata.add("arguments", args);

            var game = new JsonArray();
            game.add("--gameDir");
            game.add("${game_directory}");
            game.add("--assetsDir");
            game.add("${assets_root}");
            game.add("--version");
            game.add("${version_name}");
            game.add("--username");
            game.add("${auth_player_name}");
            game.add("--uuid");
            game.add("${auth_uuid}");

            args.add("game", game);


            var jvm = new JsonArray();
            args.add("jvm", jvm);
            jvm.add("-cp");
            jvm.add("${classpath}");
        }
        {
            var assetIndex = new JsonObject();
            metadata.add("assetIndex", assetIndex);
            metadata.addProperty("assets", assets.name);

            assetIndex.addProperty("id", assets.name);
            assetIndex.addProperty("sha1", assets.sha1);
            assetIndex.addProperty("size", assets.size);
            assetIndex.addProperty("totalSize", assets.totalSize);
            assetIndex.addProperty("url", assets.url);
        }
        metadata.addProperty("complianceLevel", 1);
        {
            var downloads = new JsonObject();
            var client = new JsonObject();
            downloads.add("client", client);

            client.addProperty("sha1", Kit.sha1(application));
            client.addProperty("size", application.length());
            client.addProperty("url", "https://static-assets.kasukusakura.com/__not_found");
        }
        metadata.addProperty("id", this.id);
        {
            var javaVer = new JsonObject();
            metadata.add("javaVersion", javaVer);
            var jver = this.runtime;
            if (jver == null) jver = JavaVersion.VERSION_17;

            javaVer.addProperty("component", jver.component);
            javaVer.addProperty("majorVersion", jver.version);
        }
        {
            var libs = new JsonArray();
            metadata.add("libraries", libs);
            if (dependencies != null && dependencies.dependencies != null) {
                for (var dep : dependencies.dependencies) {
                    var slib = new JsonObject();
                    var dwn = new JsonObject();

                    slib.add("downloads", dwn);
                    libs.add(slib);

                    slib.addProperty("name", dep.id);

                    var artifact = new JsonObject();
                    dwn.add("artifact", artifact);
                    artifact.addProperty("path", dep.jarPath);
                    artifact.addProperty("sha1", dep.sha1);
                    artifact.addProperty("size", dep.size);
                    if (dep.url == null) {
                        artifact.addProperty("url", Dependencies.RemoteRepo.MOJANG_SERVER.server.resolve(dep.jarPath).toASCIIString());
                    } else {
                        artifact.addProperty("url", dep.url);
                    }

                }
            }
        }
        metadata.addProperty("mainClass", this.mainClass);
        metadata.addProperty("minimumLauncherVersion", 21);
        {
            var time = releaseTime;
            if (time == null) time = Instant.now();
            var rt = time
                    .atOffset(ZoneOffset.UTC)
                    .truncatedTo(ChronoUnit.SECONDS)
                    .toString();
            rt = rt.replace("Z", "+00:00");
            System.out.println(rt);

            metadata.addProperty("releaseTime", rt);
            metadata.addProperty("time", rt);
        }
        metadata.addProperty("type", "release");

        {
            StringWriter stringWriter = new StringWriter();
            JsonWriter jsonWriter = new JsonWriter(stringWriter);
            jsonWriter.setLenient(true);
            jsonWriter.setHtmlSafe(false);
            jsonWriter.setIndent("  ");
            Streams.write(metadata, jsonWriter);
            var meta = stringWriter.toString();

            Files.writeString(
                    output.toPath().resolve(id + ".json"),
                    meta
            );
        }


        Files.copy(
                application.toPath(),
                output.toPath().resolve(id + ".jar"),
                StandardCopyOption.REPLACE_EXISTING
        );
    }
}
