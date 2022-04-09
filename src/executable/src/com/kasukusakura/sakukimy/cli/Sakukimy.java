/*
 * Copyright (c) 2021-2022 KasukuSakura & Contributors. All rights reserved.
 *
 * Use of this source code is governed by the Apache License 2.0 that can be found through the following link.
 *
 * https://github.com/KasukuSakura/Sakukimy/blob/main/LICENSE
 */

package com.kasukusakura.sakukimy.cli;

import com.kasukusakura.sakukimy.common.BuildingContext;
import com.kasukusakura.sakukimy.common.Dependencies;
import com.kasukusakura.sakukimy.common.JavaVersion;
import com.kasukusakura.sakukimy.common.ProjectPacker;
import org.apache.commons.cli.*;

import java.io.File;
import java.net.URI;
import java.util.Comparator;
import java.util.HashMap;
import java.util.NoSuchElementException;
import java.util.stream.Stream;

public class Sakukimy {
    private static final Exception STUB = new Exception();
    private static final Exception HELP = new Exception();

    private static class TGroup implements Comparator<Option> {
        public final HashMap<Option, Integer> options = new HashMap<>();
        Options options0;

        TGroup addOpt(int group, Option option) {
            options.put(option, group);
            options0.addOption(option);
            return this;
        }

        TGroup addOpt(int group, Option.Builder option) {
            return addOpt(group, option.build());
        }

        @Override
        public int compare(Option o1, Option o2) {
            if (o1 == o2) return 0;
            var g1 = options.getOrDefault(o1, 0);
            var g2 = options.getOrDefault(o2, 0);
            var gcp = g1.compareTo(g2);
            if (gcp != 0) return gcp;

            return getKey(o1).compareToIgnoreCase(getKey(o2));
        }

        private static String getKey(Option opt) {
            return opt.getOpt() == null ? opt.getLongOpt() : opt.getOpt();
        }
    }

    public static void main(String[] args) throws Exception {

        var options = new Options();
        var tgroup = new TGroup();
        tgroup.options0 = options;

        tgroup.addOpt(-30, new Option("h", "help", false, "print help"));
        tgroup
                .addOpt(1, Option.builder("name").hasArg().required().desc("The name of your release"))
                .addOpt(2, new Option("dist", true, "Build output, default: dist"))
                .addOpt(3, new Option("jrt", true, "Java runtime version, (8 / 16 / 17)\ndefault: 17"))

                .addOpt(5, Option.builder("jar").hasArg().required().desc("Application jar"))
                .addOpt(5, Option.builder("main").hasArg().required().desc("The main class"))

                .addOpt(7, Option.builder("d")
                        .longOpt("dependency")
                        .hasArg()
                        .desc("" +
                                "The library that application used\n" +
                                "Minecraft Launcher may not support extra libraries.\n" +
                                "So libraries auto downloading only supported when users using HMCL.\n" +
                                "We suggest you shadow all libraries that you used into your application archive.\n" +
                                "Or drop Microsoft Officially Minecraft Launcher support."
                        )
                )
                .addOpt(8, Option.builder().longOpt("use-server")
                        .argName("server")
                        .numberOfArgs(1)
                        .desc("" +
                                "Use a built in server to fetch library metadata.\n" +
                                "Built in: " +
                                String.join(", ", Dependencies.RemoteRepo.BUILTIN.keySet())
                        )
                )
                .addOpt(9, Option.builder().longOpt("repo-server")
                        .argName("name> <uri")
                        .numberOfArgs(2)
                        .desc("Add a custom repo server")
                )
        ;

        var parser = new DefaultParser();
        CommandLine cli;
        try {
            if (args.length == 1 && args[0].equals("-h")) throw HELP;

            cli = parser.parse(options, args, true);
            if (cli.getOptions().length == 0) {
                throw STUB;
            }
            if (cli.hasOption("help")) {
                throw HELP;
            }
        } catch (Exception throwable) {
            if (throwable != STUB && throwable != HELP) {
                System.out.println(throwable.getMessage());
            }
            var formatter = new HelpFormatter();
            formatter.setOptionComparator(tgroup);
            formatter.setWidth(135);
            formatter.printHelp("sakukimy", options);
            if (throwable != HELP) {
                System.exit(-5);
            }
            return;
        }

        var ctx = new BuildingContext();
        var dependencies = new Dependencies();
        var packer = new ProjectPacker();
        packer.dependencies = dependencies;
        packer.id = cli.getOptionValue("name");
        if (cli.hasOption("dist")) {
            ctx.output = new File(cli.getOptionValue("dist"));
        }
        if (cli.hasOption("jrt")) {
            var jrt = Integer.parseInt(cli.getOptionValue("jrt"));
            packer.runtime = Stream.of(JavaVersion.values()).filter(it -> it.version == jrt).findFirst()
                    .orElseThrow(() -> new RuntimeException("Java runtime " + jrt + " not suppoerted"));
        }
        packer.application = new File(cli.getOptionValue("jar"));
        packer.mainClass = cli.getOptionValue("main");
        if (cli.hasOption("dependency")) {
            for (var d : cli.getOptionValues("dependency")) {
                dependencies.dependencies.add(Dependencies.Dependency.of(d));
            }
        }
        if (cli.hasOption("use-server")) {
            for (var s : cli.getOptionValues("use-server")) {
                var server = Dependencies.RemoteRepo.BUILTIN.get(s);
                if (server == null) {
                    throw new NoSuchElementException("No built server found: " + server);
                }
                dependencies.servers.add(server);
            }
        }
        if (cli.hasOption("repo-server")) {
            var srs = cli.getOptionValues("repo-server");
            for (var i = 0; i < srs.length; i += 2) {
                dependencies.servers.add(new Dependencies.RemoteRepo(
                        URI.create(srs[i + 1]),
                        srs[i]
                ));
            }
        }
        packer.build(ctx);
        System.out.println("Built custom mc version: " + packer.id + " -> " + ctx.output.getAbsolutePath());
    }
}
