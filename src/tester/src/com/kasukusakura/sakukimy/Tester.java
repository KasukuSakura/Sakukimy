/*
 * Copyright (c) 2021-2022 KasukuSakura & Contributors. All rights reserved.
 *
 * Use of this source code is governed by the Apache License 2.0 that can be found through the following link.
 *
 * https://github.com/KasukuSakura/Sakukimy/blob/main/LICENSE
 */

package com.kasukusakura.sakukimy;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.lang.management.ManagementFactory;

public class Tester {
    public static void main(String[] args) {

        System.out.println("OOPS");
        var jframe = new JFrame("Booted!");
        Font usedFont;
        {
            var crtFont = jframe.getFont();
            var ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            for (var f : ge.getAllFonts()) {
                if (f.getName().equals("JetBrains Mono Regular")) {
                    crtFont = f;
                }
            }
            jframe.setFont(usedFont = crtFont.deriveFont(20.0f));
        }
        var textArea = new JTextArea();
        textArea.append("If you see this message. It means Sakukimy Tester started.\n");
        textArea.append("Working Dir: ");
        textArea.append(new File(".").getAbsolutePath());
        textArea.append("\n\n");
        textArea.append("ClassPath:\n");
        {
            var cp = ManagementFactory.getRuntimeMXBean();
            for (var c : cp.getClassPath().split(File.pathSeparator)) {
                textArea.append("`- ");
                textArea.append(c);
                textArea.append("\n");
            }
        }
        textArea.append("\n\n");
        for (var arg : args) {
            textArea.append(arg);
            textArea.append("\n");
        }
        textArea.setFont(usedFont);
        jframe.add(textArea);
        jframe.pack();
        jframe.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        jframe.setLocationRelativeTo(null);
        jframe.setVisible(true);
    }
}
