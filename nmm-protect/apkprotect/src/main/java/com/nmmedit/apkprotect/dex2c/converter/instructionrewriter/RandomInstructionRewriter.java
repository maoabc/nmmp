package com.nmmedit.apkprotect.dex2c.converter.instructionrewriter;

import org.jf.dexlib2.Opcode;
import org.jf.dexlib2.Opcodes;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Random;

public class RandomInstructionRewriter extends InstructionRewriter {
    private final List<Opcode> opcodeList = new ArrayList<>(256);
    private final EnumMap<Opcode, Integer> opcodeMap = new EnumMap<>(Opcode.class);

    public RandomInstructionRewriter() {
        //虚拟机使用39版本的opcode,所以这里需要使用同样版本
        super(Opcodes.forDexVersion(39));
        final ArrayList<Opcode> randOpcodes = new ArrayList<>();
        for (int i = 0; i < 256; i++) {
            final Opcode opcode = opcodes.getOpcodeByValue(i);
            opcodeList.add(opcode);
            if (opcode != null && opcode != Opcode.NOP) {//只处理dex指令,忽略odex
                randOpcodes.add(opcode);
            }
        }
        //随机opcode
        for (int i = 1; i < opcodeList.size(); i++) {
            final Opcode opcode = opcodeList.get(i);
            if (opcode != null) {
                final int randIdx = new Random().nextInt(randOpcodes.size());
                final Opcode remove = randOpcodes.remove(randIdx);
                opcodeList.set(i, remove);
            }
        }


        for (int i = 0; i < opcodeList.size(); i++) {
            final Opcode opcode = opcodeList.get(i);
            if (opcode != null) opcodeMap.put(opcode, i);
        }

    }

    @Override
    public int replaceOpcode(Opcode opcode) {
        return opcodeMap.get(opcode);
    }

    @Nonnull
    @Override
    protected List<Opcode> getOpcodeList() {
        return opcodeList;
    }
}
