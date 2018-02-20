/*
 * Copyright (c) 2014 Jeffrey Bian
 * 
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 * 
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 * 
 */

package com.taibaisoft.chip8.assemblers;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;

import com.taibaisoft.chip8.platform.Arg;
import com.taibaisoft.chip8.platform.ArgDef;
import com.taibaisoft.chip8.platform.CmdArgs;
import com.taibaisoft.chip8.platform.Platforms;
import com.taibaisoft.chip8.platform.Util;

public class AsmMain {

    final static String ASM = "asm";
    final static String DSM = "dsm";
    final static String OUT = "out";
    final static String ORG = "base";
    final static String PIN = "print";
    final static String HLP = "help";

    static ArgDef[] argDefArray = new ArgDef[]{
            new ArgDef(ASM, "a", "Assemble a source file.", 0, 0),
            new ArgDef(OUT, "o", "Specifies output file", 0, 1),
            new ArgDef(DSM, "d", "Disassemble a binary file.", 0, 0),
            new ArgDef(ORG, "b", "Base loading address, default to 200 (hex).", 0, 1),
            new ArgDef(PIN, "p", "Print the intermediate Assembled lines to STDOUT. Good for debugging the assembler.", 0, 0),
            new ArgDef(HLP, "h", "Print this quick help message.", 0, 0),
    };

    public static void run(String[] args) {


        CmdArgs cmdArgs = new CmdArgs(argDefArray, args);

        int i = 0;
        Arg arg;
        boolean isAssemble = true;
        String output = "stdout";
        String input = "";
        int startAddress = 0x200;
        boolean showHelp = false;
        boolean print = false;


        if (!Platforms.isJavaVersionOK()) {
            System.err.println("Cannot run on JRE version less than 1.7.");
        }


        outter:
        while ((arg = cmdArgs.getNext(i)) != null) {
            i = arg.nextIndex;
            switch (arg.argName) {
                // Java 7, string switch
                case ASM:
                    break;
                case DSM:
                    isAssemble = false;
                    break;
                case OUT:
                    output = arg.argVal;
                    break;
                case ORG:
                    try {
                        startAddress = Integer.parseInt(arg.argVal, 16);
                    } catch (NumberFormatException e) {
                        System.err.println("Wrong address format. Must be an integer.");
                    }
                    break;
                case HLP:
                    showHelp = true;
                    break outter;
                case PIN:
                    print = true;
                    break;
                default:
                    if (input.length() == 0) {
                        // First free argument is considered as input
                        input = arg.argVal;
                    }
                    break;
            }
        }

        // Logo
        System.out.println(Util.getVersionString("Assembler/Disassembler"));

        if (showHelp) {
            StringBuilder sb = new StringBuilder("Usage: " + Util.NEW_LINE);
            for (ArgDef def : argDefArray) {
                String s = String.format("  --%s, -%s  %s", def.name, def.alias, def.desc);
                sb.append(s).append(Util.NEW_LINE);
            }
            System.out.println(sb.toString());
            System.exit(0);
        }


        // Check errors.
        String error = cmdArgs.getLastError();
        if (error.length() > 0) {
            System.err.println(error);
            System.exit(1);
        }

        if (print && !isAssemble) {
            System.err.println("Option --" + PIN + " only available when in Assemble mode.");
            System.exit(1);
        }

        try {
            byte[] readBytes = Files.readAllBytes(Paths.get(input));

            if (isAssemble) {

                String asciiString = new String(readBytes, Charset.forName("ASCII"));

                Assembler asm = new Assembler();

                System.out.println("Assembling: [" + input + "] => [" + output + "].\n");
                byte[] outputContent = asm.assemble(asciiString, startAddress);

                if (outputContent != null) {
                    if (print) {
                        asm.outputLastAssembledIntermediate();
                    }
                    if (0 == output.compareToIgnoreCase("stdout")) {
                        Assembler.printBinData(outputContent, false);
                    } else {
                        Files.write(Paths.get(output), outputContent);
                        System.out.println(outputContent.length + " bytes written. Done.");
                    }
                } else {
                    throw new Exception("Empty output. Unknown exceptions occurred during assembling.");
                }
            } else {
                Disassembler d = new Disassembler();

                System.out.println("Disassembling: [" + input + "] => [" + output + "].\n");

                int len = (readBytes == null ? 0 : readBytes.length);
                if (len == 0) {
                    throw new Exception("Empty input.");
                }

                byte[] loadedCode = new byte[len + startAddress];
                System.arraycopy(readBytes, 0, loadedCode, startAddress, len);

                d.disassemble(loadedCode, startAddress, null);

                String outputContent = d.getDisassembleResultAsString();
                if (outputContent == null) {
                    throw new Exception("Empty output. Unknown exceptions occurred during disassembling.");
                }
                if (0 == output.compareToIgnoreCase("stdout")) {
                    System.out.println(outputContent);
                } else {
                    byte[] bytesToWrite = outputContent.getBytes(Charset.forName("ASCII"));

                    if (bytesToWrite == null) {
                        throw new Exception("Empty output. Unknown exceptions occurred during disassembling.");
                    }

                    Files.write(Paths.get(output), bytesToWrite);
                    System.out.println(bytesToWrite.length + " bytes written. Done.");
                }
            }
        } catch (IOException e) {
            System.err.print("Cannot read input file, reason : ");
            System.err.println(e.getMessage());
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }
}
