package com.nmmedit.dex2c.converter.instructionrewriter;

import com.nmmedit.apkprotect.dex2c.converter.instructionrewriter.InstructionRewriter;
import com.nmmedit.apkprotect.dex2c.converter.instructionrewriter.RandomInstructionRewriter;
import org.junit.Test;

import java.io.IOException;
import java.io.StringWriter;

public class RandomInstructionRewriterTest {

    @Test
    public void testInsRewriter() throws IOException {
        final InstructionRewriter rewriter = new RandomInstructionRewriter();
        final StringWriter opcodeWriter = new StringWriter();
        final StringWriter gotoTable = new StringWriter();
        rewriter.generateConfig(opcodeWriter, gotoTable);
        System.out.println(gotoTable.toString());
    }

}