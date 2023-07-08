package com.nmmedit.protect;

import java.io.IOException;
import java.util.Arrays;

public class Main {

    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            System.err.println("<apk|aab|aar> subCommand");
            final String[] newArgs = new String[0];
            System.err.println("apk:");
            ApkMain.main(newArgs);
            System.err.println("aab:");
            AabMain.main(newArgs);
            System.err.println("aar:");
            AarMain.main(newArgs);
            System.exit(-1);
            return;
        }
        final String[] newArgs = Arrays.copyOfRange(args, 1, args.length);
        switch (args[0]) {
            case "apk":
                ApkMain.main(newArgs);
                break;
            case "aab":
                AabMain.main(newArgs);
                break;
            case "aar":
                AarMain.main(newArgs);
                break;
            default:
                System.err.println("Unknown subcommand");
                System.exit(-1);
        }
    }
}
