//
// Created by mao on 20-7-28.
//

#include <cstring>
#include <cmath>
#include <jni.h>
#include "DexOpcodes.h"
#include "ScopedLocalRef.h"
#include "Exception.h"
#include "Interp.h"
#include "vm.h"






/*
 * If enabled, log instructions as we execute them.
 */
#ifdef LOG_INSTR

void dvmDumpRegs(const jmethodID method, const u4 *framePtr, bool inOnly) {

}

# define ILOGD(...) ILOG(LOG_DEBUG, __VA_ARGS__)
# define ILOGV(...) ILOG(LOG_VERBOSE, __VA_ARGS__)
# define ILOG(_level, ...) do {                                             \
        char debugStrBuf[128];                                              \
        snprintf(debugStrBuf, sizeof(debugStrBuf), __VA_ARGS__);            \
        ALOG(_level, LOG_TAG"i", "####%s", debugStrBuf);                    \
    } while(false)

void dvmDumpRegs(const jmethodID method, const u4 *framePtr, bool inOnly);

# define DUMP_REGS(_meth, _frame, _inOnly) dvmDumpRegs(_meth, _frame, _inOnly)
static const char kSpacing[] = "            ";
#else
# define ILOGD(...) ((void)0)
# define ILOGV(...) ((void)0)
# define DUMP_REGS(_meth, _frame, _inOnly) ((void)0)
#endif


#define GET_REGISTER_FLAGS(_idx)           ((u1)fp_flags[(_idx)])
#define SET_REGISTER_FLAGS(_idx, _val)     (fp_flags[(_idx)] =(u1) (_val))


#define GET_REGISTER_AS_OBJECT(_idx)       ((jobject) fp[(_idx)])

#define DELETE_LOCAL_REF(_idx)                         \
if(GET_REGISTER_FLAGS(_idx)){                          \
    env->DeleteLocalRef(GET_REGISTER_AS_OBJECT(_idx)); \
}

#define SET_REGISTER_AS_OBJECT(_idx, _val)  \
DELETE_LOCAL_REF(_idx);                     \
(fp[(_idx)] = (regptr_t) (_val));           \
SET_REGISTER_FLAGS(_idx, 1)


#define GET_REGISTER(_idx)                 ((u4)fp[(_idx)])

#define SET_REGISTER(_idx, _val)            \
DELETE_LOCAL_REF(_idx);                     \
(fp[(_idx)] =(u4) (_val));                  \
SET_REGISTER_FLAGS(_idx, 0)


#define GET_REGISTER_INT(_idx)             ((s4)GET_REGISTER(_idx))

#define SET_REGISTER_INT(_idx, _val)        SET_REGISTER(_idx, (s4)(_val));

#define GET_REGISTER_FLOAT(_idx)           (*((float*) &fp[(_idx)]))

#define SET_REGISTER_FLOAT(_idx, _val)      \
DELETE_LOCAL_REF(_idx);                     \
(*((float*) &fp[(_idx)]) = (_val));         \
SET_REGISTER_FLAGS(_idx, 0)


//#ifdef _LP64

#define GET_REGISTER_WIDE(_idx)            ((s8)fp[(_idx)])

#define SET_REGISTER_WIDE(_idx, _val)       \
DELETE_LOCAL_REF(_idx);                     \
(fp[(_idx)] =(s8) (_val));                  \
SET_REGISTER_FLAGS(_idx, 0)

#define GET_REGISTER_DOUBLE(_idx)          (*((double*) &fp[(_idx)]))

#define SET_REGISTER_DOUBLE(_idx, _val)     \
DELETE_LOCAL_REF(_idx);                     \
(*((double*) &fp[(_idx)]) = (_val));        \
SET_REGISTER_FLAGS(_idx, 0)
//#else

//#define GET_REGISTER_WIDE(_idx)            getLongFromArray(fp,_idx)
//
//#define SET_REGISTER_WIDE(_idx, _val)      \
//DELETE_LOCAL_REF(_idx);                    \
//putLongToArray(fp,_idx,_val);              \
//SET_REGISTER_FLAGS(_idx, 0)
//
//
//#define GET_REGISTER_DOUBLE(_idx)          getDoubleFromArray(fp,_idx)
//
//#define SET_REGISTER_DOUBLE(_idx, _val)    \
//DELETE_LOCAL_REF(_idx);                    \
//putDoubleToArray(fp,_idx,_val);            \
//SET_REGISTER_FLAGS(_idx, 0)
//
//#endif
//




/*
 * Get 16 bits from the specified offset of the program counter.  We always
 * want to load 16 bits at a time from the instruction stream -- it's more
 * efficient than 8 and won't have the alignment problems that 32 might.
 *
 * Assumes existence of "const u2* pc".
 */
#define FETCH(_offset)     (pc[(_offset)])

/*
 * Extract instruction byte from 16-bit fetch (_inst is a u2).
 */
#define INST_INST(_inst)    ((_inst) & 0xff)

/*
 * Replace the opcode (used when handling breakpoints).  _opcode is a u1.
 */
#define INST_REPLACE_OP(_inst, _opcode) (((_inst) & 0xff00) | _opcode)

/*
 * Extract the "vA, vB" 4-bit registers from the instruction word (_inst is u2).
 */
#define INST_A(_inst)       (((_inst) >> 8) & 0x0f)
#define INST_B(_inst)       ((_inst) >> 12)

/*
 * Get the 8-bit "vAA" 8-bit register index from the instruction word.
 * (_inst is u2)
 */
#define INST_AA(_inst)      ((_inst) >> 8)



/*
 * Adjust the program counter.  "_offset" is a signed int, in 16-bit units.
 *
 * Assumes the existence of "const u2* pc" and "const u2* insns".
 *
 * We don't advance the program counter until we finish an instruction or
 * branch, because we do want to have to unroll the PC if there's an
 * exception.
 */

# define ADJUST_PC(_offset) do {                                            \
        pc += _offset;                                                      \
} while (false)


/* File: portable/stubdefs.cpp */
/*
 * In the C mterp stubs, "goto" is a function call followed immediately
 * by a return.
 */

#define GOTO_TARGET(_target, ...) _target:

#define GOTO_TARGET_END



/*
 * Instruction framing.  For a switch-oriented implementation this is
 * case/break, for a threaded implementation it's a goto label and an
 * instruction fetch/computed goto.
 *
 * Assumes the existence of "const u2* pc" and (for threaded operation)
 * "u2 inst".
 */
# define H(_op)             &&op_##_op
# define HANDLE_OPCODE(_op) op_##_op:
# define FINISH(_offset) {                                                  \
        ADJUST_PC(_offset);                                                 \
        inst = FETCH(0);                                                    \
        goto *handlerTable[INST_INST(inst)];                                \
    }
# define FINISH_BKPT(_opcode) {                                             \
        goto *handlerTable[_opcode];                                        \
    }

#define OP_END

/*
 * The "goto" targets just turn into goto statements.  The "arguments" are
 * passed through local variables.
 */

#define GOTO_exceptionThrown() goto exceptionThrown;

#define GOTO_returnFromMethod() goto returnFromMethod;

#define GOTO_invoke(_target, _methodCallRange)                              \
    do {                                                                    \
        methodCallRange = _methodCallRange;                                 \
        goto _target;                                                       \
    } while(false)


#define GOTO_bail() goto bail;


/*
 * Periodically check for thread suspension.
 *
 * While we're at it, see if a debugger has attached or the profiler has
 * started.  If so, switch to a different "goto" table.
 */
#define PERIODIC_CHECKS(_pcadj) ((void)0)


/*
 * ===========================================================================
 *
 * What follows are opcode definitions shared between multiple opcodes with
 * minor substitutions handled by the C pre-processor.  These should probably
 * use the mterp substitution mechanism instead, with the code here moved
 * into common fragment files (like the asm "binop.S"), although it's hard
 * to give up the C preprocessor in favor of the much simpler text subst.
 *
 * ===========================================================================
 */

#define HANDLE_NUMCONV(_opcode, _opname, _fromtype, _totype)                \
    HANDLE_OPCODE(_opcode /*vA, vB*/)                                       \
        vdst = INST_A(inst);                                                \
        vsrc1 = INST_B(inst);                                               \
        ILOGV("|%s v%d,v%d", (_opname), vdst, vsrc1);                       \
        SET_REGISTER##_totype(vdst,                                         \
            GET_REGISTER##_fromtype(vsrc1));                                \
        FINISH(1);

#define HANDLE_FLOAT_TO_INT(_opcode, _opname, _fromvtype, _fromrtype, \
        _tovtype, _tortype)                                                 \
    HANDLE_OPCODE(_opcode /*vA, vB*/)                                       \
    {                                                                       \
        /* spec defines specific handling for +/- inf and NaN values */     \
        _fromvtype val;                                                     \
        _tovtype intMin, intMax, result;                                    \
        vdst = INST_A(inst);                                                \
        vsrc1 = INST_B(inst);                                               \
        ILOGV("|%s v%d,v%d", (_opname), vdst, vsrc1);                       \
        val = GET_REGISTER##_fromrtype(vsrc1);                              \
        intMin = (_tovtype) 1 << (sizeof(_tovtype) * 8 -1);                 \
        intMax = ~intMin;                                                   \
        result = (_tovtype) val;                                            \
        if (val >= intMax)          /* +inf */                              \
            result = intMax;                                                \
        else if (val <= intMin)     /* -inf */                              \
            result = intMin;                                                \
        else if (val != val)        /* NaN */                               \
            result = 0;                                                     \
        else                                                                \
            result = (_tovtype) val;                                        \
        SET_REGISTER##_tortype(vdst, result);                               \
    }                                                                       \
    FINISH(1);

#define HANDLE_INT_TO_SMALL(_opcode, _opname, _type)                        \
    HANDLE_OPCODE(_opcode /*vA, vB*/)                                       \
        vdst = INST_A(inst);                                                \
        vsrc1 = INST_B(inst);                                               \
        ILOGV("|int-to-%s v%d,v%d", (_opname), vdst, vsrc1);                \
        SET_REGISTER(vdst, (_type) GET_REGISTER(vsrc1));                    \
        FINISH(1);

/* NOTE: the comparison result is always a signed 4-byte integer */
#define HANDLE_OP_CMPX(_opcode, _opname, _varType, _type, _nanVal)          \
    HANDLE_OPCODE(_opcode /*vAA, vBB, vCC*/)                                \
    {                                                                       \
        int result;                                                         \
        u2 regs;                                                            \
        _varType val1, val2;                                                \
        vdst = INST_AA(inst);                                               \
        regs = FETCH(1);                                                    \
        vsrc1 = regs & 0xff;                                                \
        vsrc2 = regs >> 8;                                                  \
        ILOGV("|cmp%s v%d,v%d,v%d", (_opname), vdst, vsrc1, vsrc2);         \
        val1 = GET_REGISTER##_type(vsrc1);                                  \
        val2 = GET_REGISTER##_type(vsrc2);                                  \
        if (val1 == val2)                                                   \
            result = 0;                                                     \
        else if (val1 < val2)                                               \
            result = -1;                                                    \
        else if (val1 > val2)                                               \
            result = 1;                                                     \
        else                                                                \
            result = (_nanVal);                                             \
        ILOGV("+ result=%d", result);                                       \
        SET_REGISTER(vdst, result);                                         \
    }                                                                       \
    FINISH(2);

#define HANDLE_OP_IF_XX(_opcode, _opname, _cmp)                             \
    HANDLE_OPCODE(_opcode /*vA, vB, +CCCC*/)                                \
        vsrc1 = INST_A(inst);                                               \
        vsrc2 = INST_B(inst);                                               \
        if ((s4) GET_REGISTER(vsrc1) _cmp (s4) GET_REGISTER(vsrc2)) {       \
            int branchOffset = (s2)FETCH(1);    /* sign-extended */         \
            ILOGV("|if-%s v%d,v%d,+0x%04x", (_opname), vsrc1, vsrc2,        \
                branchOffset);                                              \
            ILOGV("> branch taken");                                        \
            if (branchOffset < 0)                                           \
                PERIODIC_CHECKS(branchOffset);                              \
            FINISH(branchOffset);                                           \
        } else {                                                            \
            ILOGV("|if-%s v%d,v%d,-", (_opname), vsrc1, vsrc2);             \
            FINISH(2);                                                      \
        }

#define HANDLE_OP_IF_XXZ(_opcode, _opname, _cmp)                            \
    HANDLE_OPCODE(_opcode /*vAA, +BBBB*/)                                   \
        vsrc1 = INST_AA(inst);                                              \
        if ((s4) GET_REGISTER(vsrc1) _cmp 0) {                              \
            int branchOffset = (s2)FETCH(1);    /* sign-extended */         \
            ILOGV("|if-%s v%d,+0x%04x", (_opname), vsrc1, branchOffset);    \
            ILOGV("> branch taken");                                        \
            if (branchOffset < 0)                                           \
                PERIODIC_CHECKS(branchOffset);                              \
            FINISH(branchOffset);                                           \
        } else {                                                            \
            ILOGV("|if-%s v%d,-", (_opname), vsrc1);                        \
            FINISH(2);                                                      \
        }

#define HANDLE_UNOP(_opcode, _opname, _pfx, _sfx, _type)                    \
    HANDLE_OPCODE(_opcode /*vA, vB*/)                                       \
        vdst = INST_A(inst);                                                \
        vsrc1 = INST_B(inst);                                               \
        ILOGV("|%s v%d,v%d", (_opname), vdst, vsrc1);                       \
        SET_REGISTER##_type(vdst, _pfx GET_REGISTER##_type(vsrc1) _sfx);    \
        FINISH(1);

#define HANDLE_OP_X_INT(_opcode, _opname, _op, _chkdiv)                     \
    HANDLE_OPCODE(_opcode /*vAA, vBB, vCC*/)                                \
    {                                                                       \
        u2 srcRegs;                                                         \
        vdst = INST_AA(inst);                                               \
        srcRegs = FETCH(1);                                                 \
        vsrc1 = srcRegs & 0xff;                                             \
        vsrc2 = srcRegs >> 8;                                               \
        ILOGV("|%s-int v%d,v%d", (_opname), vdst, vsrc1);                   \
        if (_chkdiv != 0) {                                                 \
            s4 firstVal, secondVal, result;                                 \
            firstVal = GET_REGISTER(vsrc1);                                 \
            secondVal = GET_REGISTER(vsrc2);                                \
            if (secondVal == 0) {                                           \
                dvmThrowArithmeticException(env,"divide by zero");          \
                GOTO_exceptionThrown();                                     \
            }                                                               \
            if ((u4)firstVal == 0x80000000 && secondVal == -1) {            \
                if (_chkdiv == 1)                                           \
                    result = firstVal;  /* division */                      \
                else                                                        \
                    result = 0;         /* remainder */                     \
            } else {                                                        \
                result = firstVal _op secondVal;                            \
            }                                                               \
            SET_REGISTER(vdst, result);                                     \
        } else {                                                            \
            /* non-div/rem case */                                          \
            SET_REGISTER(vdst,                                              \
                (s4) GET_REGISTER(vsrc1) _op (s4) GET_REGISTER(vsrc2));     \
        }                                                                   \
    }                                                                       \
    FINISH(2);

#define HANDLE_OP_SHX_INT(_opcode, _opname, _cast, _op)                     \
    HANDLE_OPCODE(_opcode /*vAA, vBB, vCC*/)                                \
    {                                                                       \
        u2 srcRegs;                                                         \
        vdst = INST_AA(inst);                                               \
        srcRegs = FETCH(1);                                                 \
        vsrc1 = srcRegs & 0xff;                                             \
        vsrc2 = srcRegs >> 8;                                               \
        ILOGV("|%s-int v%d,v%d", (_opname), vdst, vsrc1);                   \
        SET_REGISTER(vdst,                                                  \
            _cast GET_REGISTER(vsrc1) _op (GET_REGISTER(vsrc2) & 0x1f));    \
    }                                                                       \
    FINISH(2);

#define HANDLE_OP_X_INT_LIT16(_opcode, _opname, _op, _chkdiv)               \
    HANDLE_OPCODE(_opcode /*vA, vB, #+CCCC*/)                               \
        vdst = INST_A(inst);                                                \
        vsrc1 = INST_B(inst);                                               \
        vsrc2 = FETCH(1);                                                   \
        ILOGV("|%s-int/lit16 v%d,v%d,#+0x%04x",                             \
            (_opname), vdst, vsrc1, vsrc2);                                 \
        if (_chkdiv != 0) {                                                 \
            s4 firstVal, result;                                            \
            firstVal = GET_REGISTER(vsrc1);                                 \
            if ((s2) vsrc2 == 0) {                                          \
                dvmThrowArithmeticException(env,"divide by zero");          \
                GOTO_exceptionThrown();                                     \
            }                                                               \
            if ((u4)firstVal == 0x80000000 && ((s2) vsrc2) == -1) {         \
                /* won't generate /lit16 instr for this; check anyway */    \
                if (_chkdiv == 1)                                           \
                    result = firstVal;  /* division */                      \
                else                                                        \
                    result = 0;         /* remainder */                     \
            } else {                                                        \
                result = firstVal _op (s2) vsrc2;                           \
            }                                                               \
            SET_REGISTER(vdst, result);                                     \
        } else {                                                            \
            /* non-div/rem case */                                          \
            SET_REGISTER(vdst, GET_REGISTER(vsrc1) _op (s2) vsrc2);         \
        }                                                                   \
        FINISH(2);

#define HANDLE_OP_X_INT_LIT8(_opcode, _opname, _op, _chkdiv)                \
    HANDLE_OPCODE(_opcode /*vAA, vBB, #+CC*/)                               \
    {                                                                       \
        u2 litInfo;                                                         \
        vdst = INST_AA(inst);                                               \
        litInfo = FETCH(1);                                                 \
        vsrc1 = litInfo & 0xff;                                             \
        vsrc2 = litInfo >> 8;       /* constant */                          \
        ILOGV("|%s-int/lit8 v%d,v%d,#+0x%02x",                              \
            (_opname), vdst, vsrc1, vsrc2);                                 \
        if (_chkdiv != 0) {                                                 \
            s4 firstVal, result;                                            \
            firstVal = GET_REGISTER(vsrc1);                                 \
            if ((s1) vsrc2 == 0) {                                          \
                dvmThrowArithmeticException(env,"divide by zero");          \
                GOTO_exceptionThrown();                                     \
            }                                                               \
            if ((u4)firstVal == 0x80000000 && ((s1) vsrc2) == -1) {         \
                if (_chkdiv == 1)                                           \
                    result = firstVal;  /* division */                      \
                else                                                        \
                    result = 0;         /* remainder */                     \
            } else {                                                        \
                result = firstVal _op ((s1) vsrc2);                         \
            }                                                               \
            SET_REGISTER(vdst, result);                                     \
        } else {                                                            \
            SET_REGISTER(vdst,                                              \
                (s4) GET_REGISTER(vsrc1) _op (s1) vsrc2);                   \
        }                                                                   \
    }                                                                       \
    FINISH(2);

#define HANDLE_OP_SHX_INT_LIT8(_opcode, _opname, _cast, _op)                \
    HANDLE_OPCODE(_opcode /*vAA, vBB, #+CC*/)                               \
    {                                                                       \
        u2 litInfo;                                                         \
        vdst = INST_AA(inst);                                               \
        litInfo = FETCH(1);                                                 \
        vsrc1 = litInfo & 0xff;                                             \
        vsrc2 = litInfo >> 8;       /* constant */                          \
        ILOGV("|%s-int/lit8 v%d,v%d,#+0x%02x",                              \
            (_opname), vdst, vsrc1, vsrc2);                                 \
        SET_REGISTER(vdst,                                                  \
            _cast GET_REGISTER(vsrc1) _op (vsrc2 & 0x1f));                  \
    }                                                                       \
    FINISH(2);

#define HANDLE_OP_X_INT_2ADDR(_opcode, _opname, _op, _chkdiv)               \
    HANDLE_OPCODE(_opcode /*vA, vB*/)                                       \
        vdst = INST_A(inst);                                                \
        vsrc1 = INST_B(inst);                                               \
        ILOGV("|%s-int-2addr v%d,v%d", (_opname), vdst, vsrc1);             \
        if (_chkdiv != 0) {                                                 \
            s4 firstVal, secondVal, result;                                 \
            firstVal = GET_REGISTER(vdst);                                  \
            secondVal = GET_REGISTER(vsrc1);                                \
            if (secondVal == 0) {                                           \
                dvmThrowArithmeticException(env,"divide by zero");          \
                GOTO_exceptionThrown();                                     \
            }                                                               \
            if ((u4)firstVal == 0x80000000 && secondVal == -1) {            \
                if (_chkdiv == 1)                                           \
                    result = firstVal;  /* division */                      \
                else                                                        \
                    result = 0;         /* remainder */                     \
            } else {                                                        \
                result = firstVal _op secondVal;                            \
            }                                                               \
            SET_REGISTER(vdst, result);                                     \
        } else {                                                            \
            SET_REGISTER(vdst,                                              \
                (s4) GET_REGISTER(vdst) _op (s4) GET_REGISTER(vsrc1));      \
        }                                                                   \
        FINISH(1);

#define HANDLE_OP_SHX_INT_2ADDR(_opcode, _opname, _cast, _op)               \
    HANDLE_OPCODE(_opcode /*vA, vB*/)                                       \
        vdst = INST_A(inst);                                                \
        vsrc1 = INST_B(inst);                                               \
        ILOGV("|%s-int-2addr v%d,v%d", (_opname), vdst, vsrc1);             \
        SET_REGISTER(vdst,                                                  \
            _cast GET_REGISTER(vdst) _op (GET_REGISTER(vsrc1) & 0x1f));     \
        FINISH(1);

#define HANDLE_OP_X_LONG(_opcode, _opname, _op, _chkdiv)                    \
    HANDLE_OPCODE(_opcode /*vAA, vBB, vCC*/)                                \
    {                                                                       \
        u2 srcRegs;                                                         \
        vdst = INST_AA(inst);                                               \
        srcRegs = FETCH(1);                                                 \
        vsrc1 = srcRegs & 0xff;                                             \
        vsrc2 = srcRegs >> 8;                                               \
        ILOGV("|%s-long v%d,v%d,v%d", (_opname), vdst, vsrc1, vsrc2);       \
        if (_chkdiv != 0) {                                                 \
            s8 firstVal, secondVal, result;                                 \
            firstVal = GET_REGISTER_WIDE(vsrc1);                            \
            secondVal = GET_REGISTER_WIDE(vsrc2);                           \
            if (secondVal == 0LL) {                                         \
                dvmThrowArithmeticException(env,"divide by zero");          \
                GOTO_exceptionThrown();                                     \
            }                                                               \
            if ((u8)firstVal == 0x8000000000000000ULL &&                    \
                secondVal == -1LL)                                          \
            {                                                               \
                if (_chkdiv == 1)                                           \
                    result = firstVal;  /* division */                      \
                else                                                        \
                    result = 0;         /* remainder */                     \
            } else {                                                        \
                result = firstVal _op secondVal;                            \
            }                                                               \
            SET_REGISTER_WIDE(vdst, result);                                \
        } else {                                                            \
            SET_REGISTER_WIDE(vdst,                                         \
                (s8) GET_REGISTER_WIDE(vsrc1) _op (s8) GET_REGISTER_WIDE(vsrc2)); \
        }                                                                   \
    }                                                                       \
    FINISH(2);

#define HANDLE_OP_SHX_LONG(_opcode, _opname, _cast, _op)                    \
    HANDLE_OPCODE(_opcode /*vAA, vBB, vCC*/)                                \
    {                                                                       \
        u2 srcRegs;                                                         \
        vdst = INST_AA(inst);                                               \
        srcRegs = FETCH(1);                                                 \
        vsrc1 = srcRegs & 0xff;                                             \
        vsrc2 = srcRegs >> 8;                                               \
        ILOGV("|%s-long v%d,v%d,v%d", (_opname), vdst, vsrc1, vsrc2);       \
        SET_REGISTER_WIDE(vdst,                                             \
            _cast GET_REGISTER_WIDE(vsrc1) _op (GET_REGISTER(vsrc2) & 0x3f)); \
    }                                                                       \
    FINISH(2);

#define HANDLE_OP_X_LONG_2ADDR(_opcode, _opname, _op, _chkdiv)              \
    HANDLE_OPCODE(_opcode /*vA, vB*/)                                       \
        vdst = INST_A(inst);                                                \
        vsrc1 = INST_B(inst);                                               \
        ILOGV("|%s-long-2addr v%d,v%d", (_opname), vdst, vsrc1);            \
        if (_chkdiv != 0) {                                                 \
            s8 firstVal, secondVal, result;                                 \
            firstVal = GET_REGISTER_WIDE(vdst);                             \
            secondVal = GET_REGISTER_WIDE(vsrc1);                           \
            if (secondVal == 0LL) {                                         \
                dvmThrowArithmeticException(env,"divide by zero");          \
                GOTO_exceptionThrown();                                     \
            }                                                               \
            if ((u8)firstVal == 0x8000000000000000ULL &&                    \
                secondVal == -1LL)                                          \
            {                                                               \
                if (_chkdiv == 1)                                           \
                    result = firstVal;  /* division */                      \
                else                                                        \
                    result = 0;         /* remainder */                     \
            } else {                                                        \
                result = firstVal _op secondVal;                            \
            }                                                               \
            SET_REGISTER_WIDE(vdst, result);                                \
        } else {                                                            \
            SET_REGISTER_WIDE(vdst,                                         \
                (s8) GET_REGISTER_WIDE(vdst) _op (s8)GET_REGISTER_WIDE(vsrc1));\
        }                                                                   \
        FINISH(1);

#define HANDLE_OP_SHX_LONG_2ADDR(_opcode, _opname, _cast, _op)              \
    HANDLE_OPCODE(_opcode /*vA, vB*/)                                       \
        vdst = INST_A(inst);                                                \
        vsrc1 = INST_B(inst);                                               \
        ILOGV("|%s-long-2addr v%d,v%d", (_opname), vdst, vsrc1);            \
        SET_REGISTER_WIDE(vdst,                                             \
            _cast GET_REGISTER_WIDE(vdst) _op (GET_REGISTER(vsrc1) & 0x3f)); \
        FINISH(1);

#define HANDLE_OP_X_FLOAT(_opcode, _opname, _op)                            \
    HANDLE_OPCODE(_opcode /*vAA, vBB, vCC*/)                                \
    {                                                                       \
        u2 srcRegs;                                                         \
        vdst = INST_AA(inst);                                               \
        srcRegs = FETCH(1);                                                 \
        vsrc1 = srcRegs & 0xff;                                             \
        vsrc2 = srcRegs >> 8;                                               \
        ILOGV("|%s-float v%d,v%d,v%d", (_opname), vdst, vsrc1, vsrc2);      \
        SET_REGISTER_FLOAT(vdst,                                            \
            GET_REGISTER_FLOAT(vsrc1) _op GET_REGISTER_FLOAT(vsrc2));       \
    }                                                                       \
    FINISH(2);

#define HANDLE_OP_X_DOUBLE(_opcode, _opname, _op)                           \
    HANDLE_OPCODE(_opcode /*vAA, vBB, vCC*/)                                \
    {                                                                       \
        u2 srcRegs;                                                         \
        vdst = INST_AA(inst);                                               \
        srcRegs = FETCH(1);                                                 \
        vsrc1 = srcRegs & 0xff;                                             \
        vsrc2 = srcRegs >> 8;                                               \
        ILOGV("|%s-double v%d,v%d,v%d", (_opname), vdst, vsrc1, vsrc2);     \
        SET_REGISTER_DOUBLE(vdst,                                           \
            GET_REGISTER_DOUBLE(vsrc1) _op GET_REGISTER_DOUBLE(vsrc2));     \
    }                                                                       \
    FINISH(2);

#define HANDLE_OP_X_FLOAT_2ADDR(_opcode, _opname, _op)                      \
    HANDLE_OPCODE(_opcode /*vA, vB*/)                                       \
        vdst = INST_A(inst);                                                \
        vsrc1 = INST_B(inst);                                               \
        ILOGV("|%s-float-2addr v%d,v%d", (_opname), vdst, vsrc1);           \
        SET_REGISTER_FLOAT(vdst,                                            \
            GET_REGISTER_FLOAT(vdst) _op GET_REGISTER_FLOAT(vsrc1));        \
        FINISH(1);

#define HANDLE_OP_X_DOUBLE_2ADDR(_opcode, _opname, _op)                     \
    HANDLE_OPCODE(_opcode /*vA, vB*/)                                       \
        vdst = INST_A(inst);                                                \
        vsrc1 = INST_B(inst);                                               \
        ILOGV("|%s-double-2addr v%d,v%d", (_opname), vdst, vsrc1);          \
        SET_REGISTER_DOUBLE(vdst,                                           \
            GET_REGISTER_DOUBLE(vdst) _op GET_REGISTER_DOUBLE(vsrc1));      \
        FINISH(1);

#define HANDLE_OP_AGET(_opcode, _opname, _setreg)                           \
    HANDLE_OPCODE(_opcode /*vAA, vBB, vCC*/)                                \
    {                                                                       \
        jarray arrayObj;                                                    \
        u2 arrayInfo;                                                       \
        vdst = INST_AA(inst);                                               \
        arrayInfo = FETCH(1);                                               \
        vsrc1 = arrayInfo & 0xff;    /* array ptr */                        \
        vsrc2 = arrayInfo >> 8;      /* index */                            \
        ILOGV("|aget%s v%d,v%d,v%d", (_opname), vdst, vsrc1, vsrc2);        \
        arrayObj = (jarray) GET_REGISTER_AS_OBJECT(vsrc1);                  \
        if (arrayObj == NULL) {                                             \
            dvmThrowNullPointerException(env,NULL);                         \
            GOTO_exceptionThrown();                                         \
        }                                                                   \
        u4 idx = GET_REGISTER(vsrc2);                                       \
        _setreg;                                                            \
        if (env->ExceptionCheck()) {                                        \
            GOTO_exceptionThrown();                                         \
        }                                                                   \
        ILOGV("+ AGET[%d]=%#x", GET_REGISTER(vsrc2), GET_REGISTER(vdst));   \
    }                                                                       \
    FINISH(2);

#define HANDLE_OP_APUT(_opcode, _opname, _setarray)                         \
    HANDLE_OPCODE(_opcode /*vAA, vBB, vCC*/)                                \
    {                                                                       \
        jarray arrayObj;                                                    \
        u2 arrayInfo;                                                       \
        vdst = INST_AA(inst);                                               \
        arrayInfo = FETCH(1);                                               \
        vsrc1 = arrayInfo & 0xff;    /* array ptr */                        \
        vsrc2 = arrayInfo >> 8;      /* index */                            \
        ILOGV("|aput%s v%d,v%d,v%d", (_opname), vdst, vsrc1, vsrc2);        \
        arrayObj = (jarray) GET_REGISTER_AS_OBJECT(vsrc1);                  \
        if (arrayObj == NULL) {                                             \
            dvmThrowNullPointerException(env, NULL);                        \
            GOTO_exceptionThrown();                                         \
        }                                                                   \
        u4 idx = GET_REGISTER(vsrc2);                                       \
        _setarray;                                                          \
        if (env->ExceptionCheck()) {                                        \
            GOTO_exceptionThrown();                                         \
        }                                                                   \
        ILOGV("+ APUT[%d]=0x%08x", GET_REGISTER(vsrc2), GET_REGISTER(vdst));\
    }                                                                       \
FINISH(2);

/*
 * It's possible to get a bad value out of a field with sub-32-bit stores
 * because the -quick versions always operate on 32 bits.  Consider:
 *   short foo = -1  (sets a 32-bit register to 0xffffffff)
 *   iput-quick foo  (writes all 32 bits to the field)
 *   short bar = 1   (sets a 32-bit register to 0x00000001)
 *   iput-short      (writes the low 16 bits to the field)
 *   iget-quick foo  (reads all 32 bits from the field, yielding 0xffff0001)
 * This can only happen when optimized and non-optimized code has interleaved
 * access to the same field.  This is unlikely but possible.
 *
 * The easiest way to fix this is to always read/write 32 bits at a time.  On
 * a device with a 16-bit data bus this is sub-optimal.  (The alternative
 * approach is to have sub-int versions of iget-quick, but now we're wasting
 * Dalvik instruction space and making it less likely that handler code will
 * already be in the CPU i-cache.)
 */
#define HANDLE_IGET_X(_opcode, _opname, _setreg, _regsize)                  \
    HANDLE_OPCODE(_opcode /*vA, vB, field@CCCC*/)                           \
    {                                                                       \
        const vmField *ifield;                                              \
        jobject obj;                                                        \
        vdst = INST_A(inst);                                                \
        vsrc1 = INST_B(inst);   /* object ptr */                            \
        ref = FETCH(1);         /* field ref */                             \
        ILOGV("|iget%s v%d,v%d,field@0x%04x", (_opname), vdst, vsrc1, ref); \
        obj = GET_REGISTER_AS_OBJECT(vsrc1);                                \
        if (obj == NULL) {                                                  \
            dvmThrowNullPointerException(env, NULL);                        \
            GOTO_exceptionThrown();                                         \
        }                                                                   \
        ifield = dvmResolver->dvmResolveField(env, ref, false);             \
        if (ifield == NULL) {                                               \
            GOTO_exceptionThrown();                                         \
        }                                                                   \
        _setreg;                                                            \
        ILOGV("+ IGET '%d'=0x%08llx", ifield->classIdx,                     \
              (u8) GET_REGISTER##_regsize(vdst));                           \
    }                                                                       \
    FINISH(2);


#define HANDLE_IPUT_X(_opcode, _opname, _setfield, _regsize)                \
    HANDLE_OPCODE(_opcode /*vA, vB, field@CCCC*/)                           \
    {                                                                       \
        const vmField *ifield;                                              \
        jobject obj;                                                        \
        vdst = INST_A(inst);                                                \
        vsrc1 = INST_B(inst);   /* object ptr */                            \
        ref = FETCH(1);         /* field ref */                             \
        ILOGV("|iput%s v%d,v%d,field@0x%04x", (_opname), vdst, vsrc1, ref); \
        obj = GET_REGISTER_AS_OBJECT(vsrc1);                                \
        if (obj == NULL) {                                                  \
            dvmThrowNullPointerException(env, NULL);                        \
            GOTO_exceptionThrown();                                         \
        }                                                                   \
        ifield = dvmResolver->dvmResolveField(env, ref, false);             \
        if (ifield == NULL) {                                               \
            GOTO_exceptionThrown();                                         \
        }                                                                   \
        _setfield;                                                          \
        ILOGV("+ IPUT '%d'=0x%08llx", ifield->classIdx,                     \
              (u8) GET_REGISTER##_regsize(vdst));                           \
    }                                                                       \
    FINISH(2);


#define HANDLE_SGET_X(_opcode, _opname, _setreg, _regsize)                  \
    HANDLE_OPCODE(_opcode /*vAA, field@BBBB*/)                              \
    {                                                                       \
        const vmField *sfield;                                              \
        vdst = INST_AA(inst);                                               \
        ref = FETCH(1);         /* field ref */                             \
        ILOGV("|sget%s v%d,sfield@0x%04x", (_opname), vdst, ref);           \
        sfield = dvmResolver->dvmResolveField(env, ref, true);              \
        if (sfield == NULL) {                                               \
            GOTO_exceptionThrown();                                         \
        }                                                                   \
        ScopedLocalRef<jclass> clazz(env,                                   \
                dvmResolver->dvmResolveClass(env, sfield->classIdx));       \
        if (clazz.get() == NULL) {                                          \
            GOTO_exceptionThrown();                                         \
        }                                                                   \
        _setreg;                                                            \
        ILOGV("+ SGET '%d'=0x%08llx",                                       \
              sfield->classIdx, (u8) GET_REGISTER##_regsize(vdst));         \
    }                                                                       \
    FINISH(2);


#define HANDLE_SPUT_X(_opcode, _opname, _setfield, _regsize)                \
    HANDLE_OPCODE(_opcode /*vAA, field@BBBB*/)                              \
    {                                                                       \
        const vmField *sfield;                                              \
        vdst = INST_AA(inst);                                               \
        ref = FETCH(1);         /* field ref */                             \
        ILOGV("|sput%s v%d,sfield@0x%04x", (_opname), vdst, ref);           \
        sfield = dvmResolver->dvmResolveField(env, ref, true);              \
        if (sfield == NULL) {                                               \
            GOTO_exceptionThrown();                                         \
        }                                                                   \
        ScopedLocalRef<jclass> clazz(env,                                   \
                dvmResolver->dvmResolveClass(env, sfield->classIdx));       \
        if (clazz.get() == NULL) {                                          \
            GOTO_exceptionThrown();                                         \
        }                                                                   \
        _setfield;                                                          \
        ILOGV("+ SPUT '%d'=0x%08llx",                                       \
              sfield->classIdx, (u8) GET_REGISTER##_regsize(vdst));         \
    }                                                                       \
    FINISH(2);

#define NEW_ARRAY(_typeCh, _type, _len)                                     \
    switch (_typeCh) {                                                      \
    case 'Z':                                                               \
        newArray = env->NewBooleanArray(_len);                              \
        break;                                                              \
    case 'B':                                                               \
        newArray = env->NewByteArray(_len);                                 \
        break;                                                              \
    case 'C':                                                               \
        newArray = env->NewCharArray(_len);                                 \
        break;                                                              \
    case 'S':                                                               \
        newArray = env->NewShortArray(_len);                                \
        break;                                                              \
    case 'I':                                                               \
        newArray = env->NewIntArray(_len);                                  \
        break;                                                              \
    case 'F':                                                               \
        newArray = env->NewFloatArray(_len);                                \
        break;                                                              \
    case 'J':                                                               \
        newArray = env->NewLongArray(_len);                                 \
        break;                                                              \
    case 'D':                                                               \
        newArray = env->NewDoubleArray(_len);                               \
        break;                                                              \
    case 'L':                                                               \
    case '[': {                                                             \
        ScopedLocalRef<jclass> arrayClass(env,                              \
                dvmResolver->dvmFindClass(env, _type));                     \
        if (arrayClass.get() == NULL) {                                     \
            GOTO_exceptionThrown();                                         \
        }                                                                   \
        newArray = env->NewObjectArray((_len), arrayClass.get(), NULL);     \
        break;                                                              \
    }                                                                       \
    default:                                                                \
        dvmThrowRuntimeException(env,"Can't new array");                    \
        newArray = NULL;                                                    \
}
//设置数组元素
#define SET_ARRAY_ELEMENT(_typeCh, _array, _idx, _vsrc)                     \
    switch(_typeCh){                                                        \
    case '[':                                                               \
    case 'L':                                                               \
        env->SetObjectArrayElement((jobjectArray) (_array), (_idx),         \
                                   GET_REGISTER_AS_OBJECT(_vsrc));          \
        break;                                                              \
    case 'Z':{                                                              \
        val = GET_REGISTER(_vsrc);                                          \
        env->SetBooleanArrayRegion((jbooleanArray) (_array), _idx, 1,       \
                               (const jboolean *) (&val));                  \
        break;                                                              \
    }                                                                       \
    case 'B': {                                                             \
        val = GET_REGISTER(_vsrc);                                          \
        env->SetByteArrayRegion((jbyteArray) (_array), _idx, 1,             \
                               (const jbyte *) (&val));                     \
        break;                                                              \
    }                                                                       \
    case 'S': {                                                             \
        val = GET_REGISTER(_vsrc);                                          \
        env->SetShortArrayRegion((jshortArray) (_array), _idx, 1,           \
                               (const jshort *) (&val));                    \
        break;                                                              \
    }                                                                       \
    case 'C': {                                                             \
        val = GET_REGISTER(_vsrc);                                          \
        env->SetCharArrayRegion((jcharArray) (_array), _idx, 1,             \
                               (const jchar *) (&val));                     \
        break;                                                              \
    }                                                                       \
    case 'I': {                                                             \
        val = GET_REGISTER(_vsrc);                                          \
        env->SetIntArrayRegion((jintArray) (_array), _idx, 1,               \
                               (const jint *) (&val));                      \
        break;                                                              \
    }                                                                       \
    case 'F':{                                                              \
        val = GET_REGISTER(_vsrc);                                          \
        env->SetFloatArrayRegion((jfloatArray) (_array), _idx, 1,           \
                               (const jfloat *) (&val));                    \
        break;                                                              \
    }                                                                       \
}


#define SET_ARGUMENT(_args, _typeCh, _idx, _vsrc)                           \
    ILOGV("set argument args[%d]=%08llx", _idx, GET_REGISTER_WIDE(_vsrc));  \
    switch(_typeCh){                                                        \
        case 'Z':                                                           \
        case 'B':                                                           \
        case 'S':                                                           \
        case 'C':                                                           \
        case 'I':                                                           \
            (_args)[_idx].i = GET_REGISTER(_vsrc);                          \
            regwidth = 1;                                                   \
            break;                                                          \
        case 'F':                                                           \
            (_args)[_idx].f = GET_REGISTER_FLOAT(_vsrc);                    \
            regwidth = 1;                                                   \
            break;                                                          \
        case 'J':                                                           \
            (_args)[_idx].j = GET_REGISTER_WIDE(_vsrc);                     \
            regwidth = 2;                                                   \
            break;                                                          \
        case 'D':                                                           \
            (_args)[_idx].d = GET_REGISTER_DOUBLE(_vsrc);                   \
            regwidth = 2;                                                   \
            break;                                                          \
        case 'L':                                                           \
            (_args)[_idx].l = GET_REGISTER_AS_OBJECT(_vsrc);                \
            regwidth = 1;                                                   \
            break;                                                          \
        default:                                                            \
            regwidth=1;                                                     \
    }


#define INVOKE_STATIC_METHOD(_x)                                               \
{                                                                              \
    jvalue *args;                                                              \
    u1 regwidth;                                                               \
    int i, idx;                                                                \
                                                                               \
                                                                               \
    char returnCh = methodToCall->shorty[0];                                   \
                                                                               \
    const char *paramTypes = methodToCall->shorty + 1;                         \
                                                                               \
    /*                                                                         \
     * Copy args.  This may corrupt vsrc1/vdst.                                \
     */                                                                        \
    if (methodCallRange) {                                                     \
        args = (jvalue *) malloc(sizeof(jvalue) * (vsrc1));                    \
        for (i = 0, idx = 0; idx < vsrc1; i++) {                               \
            SET_ARGUMENT(args, paramTypes[i], i, vdst + idx);                  \
            idx += regwidth;                                                   \
        }                                                                      \
    } else {                                                                   \
        args = args_tmp;                                                       \
        u4 count = vsrc1 >> 4;                                                 \
        assert(count <= 5);                                                    \
        for (i = 0, idx = 0; idx < count; i++) {                               \
            switch (idx) {                                                     \
            case 4:                                                            \
                SET_ARGUMENT(args, paramTypes[i], i, vsrc1 & 0x0f);            \
                break;                                                         \
            case 3:                                                            \
                SET_ARGUMENT(args, paramTypes[i], i, vdst >> 12);              \
                break;                                                         \
            case 2:                                                            \
                SET_ARGUMENT(args, paramTypes[i], i, (vdst & 0x0f00) >> 8);    \
                break;                                                         \
            case 1:                                                            \
                SET_ARGUMENT(args, paramTypes[i], i, (vdst & 0x00f0) >> 4);    \
                break;                                                         \
            case 0:                                                            \
                SET_ARGUMENT(args, paramTypes[i], i, vdst & 0x0f);             \
                break;                                                         \
            default:                                                           \
                regwidth = 1;                                                  \
            }                                                                  \
            idx += regwidth;                                                   \
        }                                                                      \
    }                                                                          \
    _x;                                                                        \
    /*                                                                         \
     * 如果申请的内存则需要释放                                                   \
     */                                                                        \
    if (args != args_tmp) {                                                    \
        free(args);                                                            \
    }                                                                          \
                                                                               \
    if(env->ExceptionCheck()){                                                 \
        GOTO_exceptionThrown();                                                \
    }                                                                          \
    FINISH(3);                                                                 \
}


#define INVOKE_METHOD(_x)                                                      \
{                                                                              \
    jvalue *args;                                                              \
    u1 regwidth;                                                               \
    int i, idx;                                                                \
                                                                               \
    char returnCh = methodToCall->shorty[0];                                   \
                                                                               \
    const char *paramTypes = methodToCall->shorty + 1;                         \
                                                                               \
    /*                                                                         \
     * Copy args.  This may corrupt vsrc1/vdst.                                \
     */                                                                        \
    if (methodCallRange) {                                                     \
        args = (jvalue *) malloc(sizeof(jvalue) * (vsrc1));                    \
        for (i = 0, idx = 0; idx < vsrc1; i++) {                               \
            if (i == 0) {                                                      \
                regwidth = 1;                                                  \
            } else {                                                           \
                SET_ARGUMENT(args, paramTypes[i - 1], i, vdst + idx);          \
            }                                                                  \
            idx += regwidth;                                                   \
        }                                                                      \
    } else {                                                                   \
        args = args_tmp;                                                       \
        u4 count = vsrc1 >> 4;                                                 \
        assert(count <= 5);                                                    \
        for (i = 0, idx = 0; idx < count; i++) {                               \
            switch (idx) {                                                     \
            case 4:                                                            \
                SET_ARGUMENT(args, paramTypes[i - 1], i, vsrc1 & 0x0f);        \
                break;                                                         \
            case 3:                                                            \
                SET_ARGUMENT(args, paramTypes[i - 1], i, vdst >> 12);          \
                break;                                                         \
            case 2:                                                            \
                SET_ARGUMENT(args, paramTypes[i - 1], i, (vdst & 0x0f00) >> 8);\
                break;                                                         \
            case 1:                                                            \
                SET_ARGUMENT(args, paramTypes[i - 1], i, (vdst & 0x00f0) >> 4);\
                break;                                                         \
            case 0:                                                            \
            /*                                                                 \
             * thisPtr已提前解析这里可以不用给args[0]赋值thisPtr                   \
             * args[0].l = GET_REGISTER_AS_OBJECT(vdst & 0x0f);                \
            */                                                                 \
                regwidth = 1;                                                  \
                break;                                                         \
            default:                                                           \
                regwidth = 1;                                                  \
            }                                                                  \
            idx += regwidth;                                                   \
        }                                                                      \
    }                                                                          \
    _x;                                                                        \
    /*                                                                         \
     * 如果申请的内存则需要释放                                                   \
     */                                                                        \
    if (args != args_tmp) {                                                    \
        free(args);                                                            \
    }                                                                          \
                                                                               \
    if(env->ExceptionCheck()){                                                 \
        GOTO_exceptionThrown();                                                \
    }                                                                          \
    FINISH(3);                                                                 \
}

#ifdef __cplusplus
extern "C" {
#endif


__attribute__((visibility("default")))
jvalue vmInterpret(
        JNIEnv *env,
        const vmCode *code,
        const vmResolver *dvmResolver
) {
    jvalue args_tmp[5];//方法调用时参数传递(参数数量小于等于5)
    jvalue retval;
    regptr_t *fp = code->regs;//寄存器
    u1 *fp_flags = code->reg_flags;//寄存器类型标识
    const u2 *pc = code->insns;
    u2 inst;                    // current instruction


    /* instruction decoding */
    u4 ref;                     // 16 or 32-bit quantity fetched directly
    u2 vsrc1, vsrc2, vdst;      // usually used for register indexes


    bool methodCallRange;
    const vmMethod *methodToCall;


    /* static computed goto table */
    DEFINE_GOTO_TABLE(handlerTable);


    FINISH(0);                  /* fetch and execute first instruction */


    /* File: c/OP_NOP.cpp */
    HANDLE_OPCODE(OP_NOP)
    FINISH(1);
    OP_END

/* File: c/OP_MOVE.cpp */
    HANDLE_OPCODE(OP_MOVE /*vA, vB*/)
    vdst = INST_A(inst);
    vsrc1 = INST_B(inst);
    ILOGV("|move%s v%d,v%d %s(v%d=0x%08x)",
          (INST_INST(inst) == OP_MOVE) ? "" : "-object", vdst, vsrc1,
          kSpacing, vdst, GET_REGISTER(vsrc1));
    SET_REGISTER(vdst, GET_REGISTER(vsrc1));
    FINISH(1);
    OP_END

/* File: c/OP_MOVE_FROM16.cpp */
    HANDLE_OPCODE(OP_MOVE_FROM16 /*vAA, vBBBB*/)
    vdst = INST_AA(inst);
    vsrc1 = FETCH(1);
    ILOGV("|move%s/from16 v%d,v%d %s(v%d=0x%08x)",
          (INST_INST(inst) == OP_MOVE_FROM16) ? "" : "-object", vdst, vsrc1,
          kSpacing, vdst, GET_REGISTER(vsrc1));
    SET_REGISTER(vdst, GET_REGISTER(vsrc1));
    FINISH(2);
    OP_END

/* File: c/OP_MOVE_16.cpp */
    HANDLE_OPCODE(OP_MOVE_16 /*vAAAA, vBBBB*/)
    vdst = FETCH(1);
    vsrc1 = FETCH(2);
    ILOGV("|move%s/16 v%d,v%d %s(v%d=0x%08x)",
          (INST_INST(inst) == OP_MOVE_16) ? "" : "-object", vdst, vsrc1,
          kSpacing, vdst, GET_REGISTER(vsrc1));
    SET_REGISTER(vdst, GET_REGISTER(vsrc1));
    FINISH(3);
    OP_END

/* File: c/OP_MOVE_WIDE.cpp */
    HANDLE_OPCODE(OP_MOVE_WIDE /*vA, vB*/)
    /* IMPORTANT: must correctly handle overlapping registers, e.g. both
     * "move-wide v6, v7" and "move-wide v7, v6" */
    vdst = INST_A(inst);
    vsrc1 = INST_B(inst);
    ILOGV("|move-wide v%d,v%d %s(v%d=0x%08llx)", vdst, vsrc1,
          kSpacing + 5, vdst, GET_REGISTER_WIDE(vsrc1));
    SET_REGISTER_WIDE(vdst, GET_REGISTER_WIDE(vsrc1));
    FINISH(1);
    OP_END

/* File: c/OP_MOVE_WIDE_FROM16.cpp */
    HANDLE_OPCODE(OP_MOVE_WIDE_FROM16 /*vAA, vBBBB*/)
    vdst = INST_AA(inst);
    vsrc1 = FETCH(1);
    ILOGV("|move-wide/from16 v%d,v%d  (v%d=0x%08llx)", vdst, vsrc1,
          vdst, GET_REGISTER_WIDE(vsrc1));
    SET_REGISTER_WIDE(vdst, GET_REGISTER_WIDE(vsrc1));
    FINISH(2);
    OP_END

/* File: c/OP_MOVE_WIDE_16.cpp */
    HANDLE_OPCODE(OP_MOVE_WIDE_16 /*vAAAA, vBBBB*/)
    vdst = FETCH(1);
    vsrc1 = FETCH(2);
    ILOGV("|move-wide/16 v%d,v%d %s(v%d=0x%08llx)", vdst, vsrc1,
          kSpacing + 8, vdst, GET_REGISTER_WIDE(vsrc1));
    SET_REGISTER_WIDE(vdst, GET_REGISTER_WIDE(vsrc1));
    FINISH(3);
    OP_END

/* File: c/OP_MOVE_OBJECT.cpp */
    HANDLE_OPCODE(OP_MOVE_OBJECT /*vA, vB*/)
    {
        jobject newRef;
        vdst = INST_A(inst);
        vsrc1 = INST_B(inst);
        ILOGV("|move%s v%d,v%d %s(v%d=0x%08x)",
              (INST_INST(inst) == OP_MOVE) ? "" : "-object", vdst, vsrc1,
              kSpacing, vdst, GET_REGISTER(vsrc1));
        newRef = env->NewLocalRef(GET_REGISTER_AS_OBJECT(vsrc1));
        SET_REGISTER_AS_OBJECT(vdst, newRef);
    }
    FINISH(1);
    OP_END


/* File: c/OP_MOVE_OBJECT_FROM16.cpp */
    HANDLE_OPCODE(OP_MOVE_OBJECT_FROM16 /*vAA, vBBBB*/)
    {
        jobject newRef;
        vdst = INST_AA(inst);
        vsrc1 = FETCH(1);
        ILOGV("|move%s/from16 v%d,v%d %s(v%d=0x%08x)",
              (INST_INST(inst) == OP_MOVE_FROM16) ? "" : "-object", vdst, vsrc1,
              kSpacing, vdst, GET_REGISTER(vsrc1));
        newRef = env->NewLocalRef(GET_REGISTER_AS_OBJECT(vsrc1));
        SET_REGISTER_AS_OBJECT(vdst, newRef);
    }
    FINISH(2);
    OP_END


/* File: c/OP_MOVE_OBJECT_16.cpp */
    HANDLE_OPCODE(OP_MOVE_OBJECT_16 /*vAAAA, vBBBB*/)
    {
        jobject newRef;
        vdst = FETCH(1);
        vsrc1 = FETCH(2);
        ILOGV("|move%s/16 v%d,v%d %s(v%d=0x%08x)",
              (INST_INST(inst) == OP_MOVE_16) ? "" : "-object", vdst, vsrc1,
              kSpacing, vdst, GET_REGISTER(vsrc1));
        newRef = env->NewLocalRef(GET_REGISTER_AS_OBJECT(vsrc1));
        SET_REGISTER_AS_OBJECT(vdst, newRef);
    }
    FINISH(3);
    OP_END


/* File: c/OP_MOVE_RESULT.cpp */
    HANDLE_OPCODE(OP_MOVE_RESULT /*vAA*/)
    vdst = INST_AA(inst);
    ILOGV("|move-result%s v%d %s(v%d=0x%08x)",
          (INST_INST(inst) == OP_MOVE_RESULT) ? "" : "-object",
          vdst, kSpacing + 4, vdst, retval.i);
    SET_REGISTER(vdst, retval.i);
    FINISH(1);
    OP_END

/* File: c/OP_MOVE_RESULT_WIDE.cpp */
    HANDLE_OPCODE(OP_MOVE_RESULT_WIDE /*vAA*/)
    vdst = INST_AA(inst);
    ILOGV("|move-result-wide v%d %s(0x%08llx)", vdst, kSpacing, retval.j);
    SET_REGISTER_WIDE(vdst, retval.j);
    FINISH(1);
    OP_END

/* File: c/OP_MOVE_RESULT_OBJECT.cpp */
    HANDLE_OPCODE(OP_MOVE_RESULT_OBJECT /*vAA*/)
    vdst = INST_AA(inst);
    ILOGV("|move-result%s v%d %s(v%d=0x%08x)",
          (INST_INST(inst) == OP_MOVE_RESULT) ? "" : "-object",
          vdst, kSpacing + 4, vdst, retval.i);
    SET_REGISTER_AS_OBJECT(vdst, retval.l);
    FINISH(1);
    OP_END


/* File: c/OP_MOVE_EXCEPTION.cpp */
    HANDLE_OPCODE(OP_MOVE_EXCEPTION /*vAA*/)
    vdst = INST_AA(inst);
    ILOGV("|move-exception v%d", vdst);
    SET_REGISTER_AS_OBJECT(vdst, env->ExceptionOccurred());
    env->ExceptionClear();
    FINISH(1);
    OP_END

/* File: c/OP_RETURN_VOID.cpp */
    HANDLE_OPCODE(OP_RETURN_VOID /**/)
    ILOGV("|return-void");

    GOTO_returnFromMethod();
    OP_END

/* File: c/OP_RETURN.cpp */
    HANDLE_OPCODE(OP_RETURN /*vAA*/)
    vsrc1 = INST_AA(inst);
    ILOGV("|return%s v%d",
          (INST_INST(inst) == OP_RETURN) ? "" : "-object", vsrc1);
    retval.i = GET_REGISTER(vsrc1);
    GOTO_returnFromMethod();
    OP_END

/* File: c/OP_RETURN_WIDE.cpp */
    HANDLE_OPCODE(OP_RETURN_WIDE /*vAA*/)
    vsrc1 = INST_AA(inst);
    ILOGV("|return-wide v%d", vsrc1);
    retval.j = GET_REGISTER_WIDE(vsrc1);
    GOTO_returnFromMethod();
    OP_END

/* File: c/OP_RETURN_OBJECT.cpp */
    HANDLE_OPCODE(OP_RETURN_OBJECT /*vAA*/)
    vsrc1 = INST_AA(inst);
    ILOGV("|return%s v%d",
          (INST_INST(inst) == OP_RETURN) ? "" : "-object", vsrc1);
    retval.l = GET_REGISTER_AS_OBJECT(vsrc1);
    GOTO_returnFromMethod();
    OP_END


/* File: c/OP_CONST_4.cpp */
    HANDLE_OPCODE(OP_CONST_4 /*vA, #+B*/)
    {
        s4 tmp;

        vdst = INST_A(inst);
        tmp = (s4) (INST_B(inst) << 28) >> 28;  // sign extend 4-bit value
        ILOGV("|const/4 v%d,#0x%02x", vdst, (s4) tmp);
        SET_REGISTER(vdst, tmp);
    }
    FINISH(1);
    OP_END

/* File: c/OP_CONST_16.cpp */
    HANDLE_OPCODE(OP_CONST_16 /*vAA, #+BBBB*/)
    vdst = INST_AA(inst);
    vsrc1 = FETCH(1);
    ILOGV("|const/16 v%d,#0x%04x", vdst, (s2) vsrc1);
    SET_REGISTER(vdst, (s2) vsrc1);
    FINISH(2);
    OP_END

/* File: c/OP_CONST.cpp */
    HANDLE_OPCODE(OP_CONST /*vAA, #+BBBBBBBB*/)
    {
        u4 tmp;

        vdst = INST_AA(inst);
        tmp = FETCH(1);
        tmp |= (u4) FETCH(2) << 16;
        ILOGV("|const v%d,#0x%08x", vdst, tmp);
        SET_REGISTER(vdst, tmp);
    }
    FINISH(3);
    OP_END

/* File: c/OP_CONST_HIGH16.cpp */
    HANDLE_OPCODE(OP_CONST_HIGH16 /*vAA, #+BBBB0000*/)
    vdst = INST_AA(inst);
    vsrc1 = FETCH(1);
    ILOGV("|const/high16 v%d,#0x%04x0000", vdst, vsrc1);
    SET_REGISTER(vdst, vsrc1 << 16);
    FINISH(2);
    OP_END

/* File: c/OP_CONST_WIDE_16.cpp */
    HANDLE_OPCODE(OP_CONST_WIDE_16 /*vAA, #+BBBB*/)
    vdst = INST_AA(inst);
    vsrc1 = FETCH(1);
    ILOGV("|const-wide/16 v%d,#0x%04x", vdst, (s2) vsrc1);
    SET_REGISTER_WIDE(vdst, (s2) vsrc1);
    FINISH(2);
    OP_END

/* File: c/OP_CONST_WIDE_32.cpp */
    HANDLE_OPCODE(OP_CONST_WIDE_32 /*vAA, #+BBBBBBBB*/)
    {
        u4 tmp;

        vdst = INST_AA(inst);
        tmp = FETCH(1);
        tmp |= (u4) FETCH(2) << 16;
        ILOGV("|const-wide/32 v%d,#0x%08x", vdst, tmp);
        SET_REGISTER_WIDE(vdst, (s4) tmp);
    }
    FINISH(3);
    OP_END

/* File: c/OP_CONST_WIDE.cpp */
    HANDLE_OPCODE(OP_CONST_WIDE /*vAA, #+BBBBBBBBBBBBBBBB*/)
    {
        u8 tmp;

        vdst = INST_AA(inst);
        tmp = FETCH(1);
        tmp |= (u8) FETCH(2) << 16;
        tmp |= (u8) FETCH(3) << 32;
        tmp |= (u8) FETCH(4) << 48;
        ILOGV("|const-wide v%d,#0x%08llx", vdst, tmp);
        SET_REGISTER_WIDE(vdst, tmp);
    }
    FINISH(5);
    OP_END

/* File: c/OP_CONST_WIDE_HIGH16.cpp */
    HANDLE_OPCODE(OP_CONST_WIDE_HIGH16 /*vAA, #+BBBB000000000000*/)
    vdst = INST_AA(inst);
    vsrc1 = FETCH(1);
    ILOGV("|const-wide/high16 v%d,#0x%04x000000000000", vdst, vsrc1);
    SET_REGISTER_WIDE(vdst, ((u8) vsrc1) << 48);
    FINISH(2);
    OP_END

/* File: c/OP_CONST_STRING.cpp */
    HANDLE_OPCODE(OP_CONST_STRING /*vAA, string@BBBB*/)
    {
        jstring strObj;

        vdst = INST_AA(inst);
        ref = FETCH(1);
        ILOGV("|const-string v%d string@0x%04x", vdst, ref);
        strObj = dvmResolver->dvmConstantString(env, ref);
        if (strObj == NULL)
            GOTO_exceptionThrown();
        SET_REGISTER_AS_OBJECT(vdst, strObj);
    }
    FINISH(2);
    OP_END

/* File: c/OP_CONST_STRING_JUMBO.cpp */
    HANDLE_OPCODE(OP_CONST_STRING_JUMBO /*vAA, string@BBBBBBBB*/)
    {
        jstring strObj;
        u4 tmp;

        vdst = INST_AA(inst);
        tmp = FETCH(1);
        tmp |= (u4) FETCH(2) << 16;
        ILOGV("|const-string/jumbo v%d string@0x%08x", vdst, tmp);
        strObj = dvmResolver->dvmConstantString(env, tmp);
        if (strObj == NULL)
            GOTO_exceptionThrown();

        SET_REGISTER_AS_OBJECT(vdst, strObj);
    }
    FINISH(3);
    OP_END

/* File: c/OP_CONST_CLASS.cpp */
    HANDLE_OPCODE(OP_CONST_CLASS /*vAA, class@BBBB*/)
    {
        jclass clazz;

        vdst = INST_AA(inst);
        ref = FETCH(1);
        ILOGV("|const-class v%d class@0x%04x", vdst, ref);
        clazz = dvmResolver->dvmResolveClass(env, ref);
        if (clazz == NULL) {
            GOTO_exceptionThrown();
        }

        SET_REGISTER_AS_OBJECT(vdst, clazz);
    }
    FINISH(2);
    OP_END

/* File: c/OP_MONITOR_ENTER.cpp */
    HANDLE_OPCODE(OP_MONITOR_ENTER /*vAA*/)
    {
        jobject obj;

        vsrc1 = INST_AA(inst);
        ILOGV("|monitor-enter v%d %s(0x%08x)",
              vsrc1, kSpacing + 6, GET_REGISTER(vsrc1));
        obj = GET_REGISTER_AS_OBJECT(vsrc1);
        if (obj == NULL) {
            dvmThrowNullPointerException(env, NULL);
            GOTO_exceptionThrown();
        }
        if (env->MonitorEnter(obj) != JNI_OK) {
            GOTO_exceptionThrown();
        }
        ILOGV("+ locking %p ", obj);
    }
    FINISH(1);
    OP_END

/* File: c/OP_MONITOR_EXIT.cpp */
    HANDLE_OPCODE(OP_MONITOR_EXIT /*vAA*/)
    {
        jobject obj;

        vsrc1 = INST_AA(inst);
        ILOGV("|monitor-exit v%d %s(0x%08x)",
              vsrc1, kSpacing + 5, GET_REGISTER(vsrc1));
        obj = GET_REGISTER_AS_OBJECT(vsrc1);
        if (obj == NULL) {
            dvmThrowNullPointerException(env, NULL);
            GOTO_exceptionThrown();
        }
        if (env->MonitorExit(obj) != JNI_OK) {
            GOTO_exceptionThrown();
        }
        ILOGV("+ unlocking %p", obj);
    }
    FINISH(1);
    OP_END

/* File: c/OP_CHECK_CAST.cpp */
    HANDLE_OPCODE(OP_CHECK_CAST /*vAA, class@BBBB*/)
    {
        jobject obj;


        vsrc1 = INST_AA(inst);
        ref = FETCH(1);         /* class to check against */
        ILOGV("|check-cast v%d,class@0x%04x", vsrc1, ref);

        obj = GET_REGISTER_AS_OBJECT(vsrc1);
        if (obj != NULL) {
            ScopedLocalRef<jclass> clazz(env, dvmResolver->dvmResolveClass(env, ref));
            if (clazz.get() == NULL) {
                GOTO_exceptionThrown();
            }

            if (!env->IsInstanceOf(obj, clazz.get())) {
                dvmThrowClassCastException(env, obj, clazz.get());
                GOTO_exceptionThrown();
            }
        }
    }
    FINISH(2);
    OP_END

/* File: c/OP_INSTANCE_OF.cpp */
    HANDLE_OPCODE(OP_INSTANCE_OF /*vA, vB, class@CCCC*/)
    {
        jobject obj;
        jboolean isInstanceOf;

        vdst = INST_A(inst);
        vsrc1 = INST_B(inst);   /* object to check */
        ref = FETCH(1);         /* class to check against */
        ILOGV("|instance-of v%d,v%d,class@0x%04x", vdst, vsrc1, ref);

        obj = GET_REGISTER_AS_OBJECT(vsrc1);
        if (obj == NULL) {
            SET_REGISTER(vdst, 0);
        } else {
            ScopedLocalRef<jclass> clazz(env, dvmResolver->dvmResolveClass(env, ref));
            if (clazz.get() == NULL) {
                GOTO_exceptionThrown();
            }
            isInstanceOf = env->IsInstanceOf(obj, clazz.get());
            SET_REGISTER(vdst, isInstanceOf);
        }
    }
    FINISH(2);
    OP_END

/* File: c/OP_ARRAY_LENGTH.cpp */
    HANDLE_OPCODE(OP_ARRAY_LENGTH /*vA, vB*/)
    {
        jarray arrayObj;
        jsize arrayLength;

        vdst = INST_A(inst);
        vsrc1 = INST_B(inst);
        arrayObj = (jarray) GET_REGISTER_AS_OBJECT(vsrc1);
        ILOGV("|array-length v%d,v%d  (%p)", vdst, vsrc1, arrayObj);
        if (arrayObj == NULL)
            GOTO_exceptionThrown();

        arrayLength = env->GetArrayLength(arrayObj);
        SET_REGISTER(vdst, arrayLength);
    }
    FINISH(1);
    OP_END

/* File: c/OP_NEW_INSTANCE.cpp */
    HANDLE_OPCODE(OP_NEW_INSTANCE /*vAA, class@BBBB*/)
    {
        jobject newObj;

        vdst = INST_AA(inst);
        ref = FETCH(1);
        ILOGV("|new-instance v%d,class@0x%04x", vdst, ref);
        ScopedLocalRef<jclass> clazz(env,
                                     dvmResolver->dvmResolveClass(env, ref));
        if (clazz.get() == NULL) {
            GOTO_exceptionThrown();
        }
        newObj = env->AllocObject(clazz.get());
        if (newObj == NULL) {
            GOTO_exceptionThrown();
        }
        SET_REGISTER_AS_OBJECT(vdst, newObj);
    }
    FINISH(2);
    OP_END

/* File: c/OP_NEW_ARRAY.cpp */
    HANDLE_OPCODE(OP_NEW_ARRAY /*vA, vB, class@CCCC*/)
    {
        jarray newArray;
        s4 length;

        const char *type;
//
        vdst = INST_A(inst);
        vsrc1 = INST_B(inst);       /* length reg */
        ref = FETCH(1);
        ILOGV("|new-array v%d,v%d,class@0x%04x  (%d elements)",
              vdst, vsrc1, ref, (s4) GET_REGISTER(vsrc1));
        length = (s4) GET_REGISTER(vsrc1);
        if (length < 0) {
            dvmThrowNegativeArraySizeException(env, length);
            GOTO_exceptionThrown();
        }

        type = dvmResolver->dvmResolveTypeUtf(env, ref);
        if (type == NULL) {
            GOTO_exceptionThrown();
        }

        NEW_ARRAY(type[1], type + 1, length);

        if (newArray == NULL) {
            GOTO_exceptionThrown();
        }
        SET_REGISTER_AS_OBJECT(vdst, newArray);
    }
    FINISH(2);
    OP_END

/* File: c/OP_FILLED_NEW_ARRAY.cpp */
    HANDLE_OPCODE(OP_FILLED_NEW_ARRAY /*vB, {vD, vE, vF, vG, vA}, class@CCCC*/)
    GOTO_invoke(filledNewArray, false);
    OP_END

/* File: c/OP_FILLED_NEW_ARRAY_RANGE.cpp */
    HANDLE_OPCODE(OP_FILLED_NEW_ARRAY_RANGE /*{vCCCC..v(CCCC+AA-1)}, class@BBBB*/)
    GOTO_invoke(filledNewArray, true);
    OP_END

/* File: c/OP_FILL_ARRAY_DATA.cpp */
    HANDLE_OPCODE(OP_FILL_ARRAY_DATA)   /*vAA, +BBBBBBBB*/
    {
        const u2 *arrayData;
        s4 offset;
        jarray arrayObj;
//
        vsrc1 = INST_AA(inst);
        offset = FETCH(1) | (((s4) FETCH(2)) << 16);
        ILOGV("|fill-array-data v%d +0x%04x", vsrc1, offset);
        arrayData = pc + offset;       // offset in 16-bit units

        arrayObj = (jarray) GET_REGISTER_AS_OBJECT(vsrc1);
        if (!dvmInterpHandleFillArrayData(env, arrayObj, arrayData)) {
            GOTO_exceptionThrown();
        }
        FINISH(3);
    }
    OP_END

/* File: c/OP_THROW.cpp */
    HANDLE_OPCODE(OP_THROW /*vAA*/)
    {
        jthrowable obj;

        /*
         * We don't create an exception here, but the process of searching
         * for a catch block can do class lookups and throw exceptions.
         * We need to update the saved PC.
         */

        vsrc1 = INST_AA(inst);
        ILOGV("|throw v%d  (%p)", vsrc1, (void *) GET_REGISTER_AS_OBJECT(vsrc1));
        obj = (jthrowable) GET_REGISTER_AS_OBJECT(vsrc1);
        if (obj == NULL) {
            /* will throw a null pointer exception */
            LOGVV("Bad exception");
        } else {
            /* use the requested exception */
            env->Throw(obj);
        }
        GOTO_exceptionThrown();
    }
    OP_END

/* File: c/OP_GOTO.cpp */
    HANDLE_OPCODE(OP_GOTO /*+AA*/)
    vdst = INST_AA(inst);
    if ((s1) vdst < 0)
        ILOGV("|goto -0x%02x", -((s1) vdst));
    else
        ILOGV("|goto +0x%02x", ((s1) vdst));
    ILOGV("> branch taken");
    if ((s1) vdst < 0) PERIODIC_CHECKS((s1) vdst);
    FINISH((s1) vdst);
    OP_END

/* File: c/OP_GOTO_16.cpp */
    HANDLE_OPCODE(OP_GOTO_16 /*+AAAA*/)
    {
        s4 offset = (s2) FETCH(1);          /* sign-extend next code unit */

        if (offset < 0)
            ILOGV("|goto/16 -0x%04x", -offset);
        else
            ILOGV("|goto/16 +0x%04x", offset);
        ILOGV("> branch taken");
        if (offset < 0) PERIODIC_CHECKS(offset);
        FINISH(offset);
    }
    OP_END

/* File: c/OP_GOTO_32.cpp */
    HANDLE_OPCODE(OP_GOTO_32 /*+AAAAAAAA*/)
    {
        s4 offset = FETCH(1);               /* low-order 16 bits */
        offset |= ((s4) FETCH(2)) << 16;    /* high-order 16 bits */

        if (offset < 0)
            ILOGV("|goto/32 -0x%08x", -offset);
        else
            ILOGV("|goto/32 +0x%08x", offset);
        ILOGV("> branch taken");
        if (offset <= 0)    /* allowed to branch to self */
            PERIODIC_CHECKS(offset);
        FINISH(offset);
    }
    OP_END

/* File: c/OP_PACKED_SWITCH.cpp */
    HANDLE_OPCODE(OP_PACKED_SWITCH /*vAA, +BBBB*/)
    {
        const u2 *switchData;
        u4 testVal;
        s4 offset;

        vsrc1 = INST_AA(inst);
        offset = FETCH(1) | (((s4) FETCH(2)) << 16);
        ILOGV("|packed-switch v%d +0x%04x", vsrc1, offset);
        switchData = pc + offset;       // offset in 16-bit units

        testVal = GET_REGISTER(vsrc1);

        offset = dvmInterpHandlePackedSwitch(env, switchData, testVal);
        ILOGV("> branch taken (0x%04x)", offset);
        if (offset <= 0)  /* uncommon */
            PERIODIC_CHECKS(offset);
        FINISH(offset);
    }
    OP_END

/* File: c/OP_SPARSE_SWITCH.cpp */
    HANDLE_OPCODE(OP_SPARSE_SWITCH /*vAA, +BBBB*/)
    {
        const u2 *switchData;
        u4 testVal;
        s4 offset;

        vsrc1 = INST_AA(inst);
        offset = FETCH(1) | (((s4) FETCH(2)) << 16);
        ILOGV("|sparse-switch v%d +0x%04x", vsrc1, offset);
        switchData = pc + offset;       // offset in 16-bit units

        testVal = GET_REGISTER(vsrc1);

        offset = dvmInterpHandleSparseSwitch(env, switchData, testVal);
        ILOGV("> branch taken (0x%04x)", offset);
        if (offset <= 0)  /* uncommon */
            PERIODIC_CHECKS(offset);
        FINISH(offset);
    }
    OP_END

/* File: c/OP_CMPL_FLOAT.cpp */
HANDLE_OP_CMPX(OP_CMPL_FLOAT, "l-float", float, _FLOAT, -1)
    OP_END

/* File: c/OP_CMPG_FLOAT.cpp */
HANDLE_OP_CMPX(OP_CMPG_FLOAT, "g-float", float, _FLOAT, 1)
    OP_END

/* File: c/OP_CMPL_DOUBLE.cpp */
HANDLE_OP_CMPX(OP_CMPL_DOUBLE, "l-double", double, _DOUBLE, -1)
    OP_END

/* File: c/OP_CMPG_DOUBLE.cpp */
HANDLE_OP_CMPX(OP_CMPG_DOUBLE, "g-double", double, _DOUBLE, 1)
    OP_END

/* File: c/OP_CMP_LONG.cpp */
HANDLE_OP_CMPX(OP_CMP_LONG, "-long", s8, _WIDE, 0)
    OP_END

/* File: c/OP_IF_EQ.cpp */
    HANDLE_OPCODE(OP_IF_EQ /*vA, vB, +CCCC*/)
    vsrc1 = INST_A(inst);
    vsrc2 = INST_B(inst);
    if (GET_REGISTER_FLAGS(vsrc1)) {
        //对象寄存器 需要特殊比较
        if (env->IsSameObject(GET_REGISTER_AS_OBJECT(vsrc1), GET_REGISTER_AS_OBJECT(vsrc2))) {
            int branchOffset = (s2) FETCH(1);    /* sign-extended */
            ILOGV("|if-eq v%d,v%d,+0x%04x", vsrc1, vsrc2,
                  branchOffset);
            ILOGV("> branch taken");
            if (branchOffset < 0)
                PERIODIC_CHECKS(branchOffset);
            FINISH(branchOffset);
        } else {
            ILOGV("|if-eq v%d,v%d,-", vsrc1, vsrc2);
            FINISH(2);
        }
    } else {
        if ((s4) GET_REGISTER(vsrc1) == (s4) GET_REGISTER(vsrc2)) {
            int branchOffset = (s2) FETCH(1);    /* sign-extended */
            ILOGV("|if-eq v%d,v%d,+0x%04x", vsrc1, vsrc2,
                  branchOffset);
            ILOGV("> branch taken");
            if (branchOffset < 0)
                PERIODIC_CHECKS(branchOffset);
            FINISH(branchOffset);
        } else {
            ILOGV("|if-eq v%d,v%d,-", vsrc1, vsrc2);
            FINISH(2);
        }
    }

/* File: c/OP_IF_NE.cpp */
    HANDLE_OPCODE(OP_IF_NE /*vA, vB, +CCCC*/)
    vsrc1 = INST_A(inst);
    vsrc2 = INST_B(inst);
    if (GET_REGISTER_FLAGS(vsrc1)) {
        //对象寄存器 需要特殊比较
        if (!env->IsSameObject(GET_REGISTER_AS_OBJECT(vsrc1), GET_REGISTER_AS_OBJECT(vsrc2))) {
            int branchOffset = (s2) FETCH(1);    /* sign-extended */
            ILOGV("|if-eq v%d,v%d,+0x%04x", vsrc1, vsrc2,
                  branchOffset);
            ILOGV("> branch taken");
            if (branchOffset < 0)
                PERIODIC_CHECKS(branchOffset);
            FINISH(branchOffset);
        } else {
            ILOGV("|if-eq v%d,v%d,-", vsrc1, vsrc2);
            FINISH(2);
        }
    } else {
        if ((s4) GET_REGISTER(vsrc1) != (s4) GET_REGISTER(vsrc2)) {
            int branchOffset = (s2) FETCH(1);    /* sign-extended */
            ILOGV("|if-ne v%d,v%d,+0x%04x", vsrc1, vsrc2,
                  branchOffset);
            ILOGV("> branch taken");
            if (branchOffset < 0)
                PERIODIC_CHECKS(branchOffset);
            FINISH(branchOffset);
        } else {
            ILOGV("|if-ne v%d,v%d,-", vsrc1, vsrc2);
            FINISH(2);
        }
    }

/* File: c/OP_IF_LT.cpp */
HANDLE_OP_IF_XX(OP_IF_LT, "lt", <)
    OP_END

/* File: c/OP_IF_GE.cpp */
HANDLE_OP_IF_XX(OP_IF_GE, "ge", >=)
    OP_END

/* File: c/OP_IF_GT.cpp */
HANDLE_OP_IF_XX(OP_IF_GT, "gt", >)
    OP_END

/* File: c/OP_IF_LE.cpp */
HANDLE_OP_IF_XX(OP_IF_LE, "le", <=)
    OP_END

/* File: c/OP_IF_EQZ.cpp */
HANDLE_OP_IF_XXZ(OP_IF_EQZ, "eqz", ==)
    OP_END

/* File: c/OP_IF_NEZ.cpp */
HANDLE_OP_IF_XXZ(OP_IF_NEZ, "nez", !=)
    OP_END

/* File: c/OP_IF_LTZ.cpp */
HANDLE_OP_IF_XXZ(OP_IF_LTZ, "ltz", <)
    OP_END

/* File: c/OP_IF_GEZ.cpp */
HANDLE_OP_IF_XXZ(OP_IF_GEZ, "gez", >=)
    OP_END

/* File: c/OP_IF_GTZ.cpp */
HANDLE_OP_IF_XXZ(OP_IF_GTZ, "gtz", >)
    OP_END

/* File: c/OP_IF_LEZ.cpp */
HANDLE_OP_IF_XXZ(OP_IF_LEZ, "lez", <=)
    OP_END

/* File: c/OP_UNUSED_3E.cpp */
    HANDLE_OPCODE(OP_UNUSED_3E)
    OP_END

/* File: c/OP_UNUSED_3F.cpp */
    HANDLE_OPCODE(OP_UNUSED_3F)
    OP_END

/* File: c/OP_UNUSED_40.cpp */
    HANDLE_OPCODE(OP_UNUSED_40)
    OP_END

/* File: c/OP_UNUSED_41.cpp */
    HANDLE_OPCODE(OP_UNUSED_41)
    OP_END

/* File: c/OP_UNUSED_42.cpp */
    HANDLE_OPCODE(OP_UNUSED_42)
    OP_END

/* File: c/OP_UNUSED_43.cpp */
    HANDLE_OPCODE(OP_UNUSED_43)
    OP_END

/* File: c/OP_AGET.cpp */
HANDLE_OP_AGET(OP_AGET, "", {
    u4 *arrData = (u4 *) env->GetPrimitiveArrayCritical(arrayObj, NULL);
    u4 val = arrData[idx];
    env->ReleasePrimitiveArrayCritical(arrayObj, arrData, JNI_ABORT);

    SET_REGISTER(vdst, val);

})
    OP_END

/* File: c/OP_AGET_WIDE.cpp */
HANDLE_OP_AGET(OP_AGET_WIDE, "-wide", {
    u8 *arrData = (u8 *) env->GetPrimitiveArrayCritical(arrayObj, NULL);
    u8 val = arrData[idx];
    env->ReleasePrimitiveArrayCritical(arrayObj, arrData, JNI_ABORT);

    SET_REGISTER_WIDE(vdst, val);
})
    OP_END

/* File: c/OP_AGET_OBJECT.cpp */
    HANDLE_OPCODE(OP_AGET_OBJECT /*vAA, vBB, vCC*/)
    {
        jobjectArray arrayObj;
        u2 arrayInfo;
        vdst = INST_AA(inst);
        arrayInfo = FETCH(1);
        vsrc1 = arrayInfo & 0xff;    /* array ptr */
        vsrc2 = arrayInfo >> 8;      /* index */
        ILOGV("|aget%s v%d,v%d,v%d", "-object", vdst, vsrc1, vsrc2);
        arrayObj = (jobjectArray) GET_REGISTER_AS_OBJECT(vsrc1);
        if (arrayObj == NULL) {
            GOTO_exceptionThrown();
        }
        u4 idx = GET_REGISTER(vsrc2);

        ILOGV("+ AGET[%d]=%#x", GET_REGISTER(vsrc2), GET_REGISTER(vdst));

        jobject eleObj = env->GetObjectArrayElement(arrayObj, idx);
        SET_REGISTER_AS_OBJECT(vdst, eleObj);

        if (env->ExceptionCheck()) {
            GOTO_exceptionThrown();
        }
    }
    FINISH(2);
    OP_END

/* File: c/OP_AGET_BOOLEAN.cpp */
HANDLE_OP_AGET(OP_AGET_BOOLEAN, "-boolean", {
    jboolean val;
    env->GetBooleanArrayRegion((jbooleanArray) arrayObj, idx, 1, &val);
    SET_REGISTER(vdst, val);
})
    OP_END

/* File: c/OP_AGET_BYTE.cpp */
HANDLE_OP_AGET(OP_AGET_BYTE, "-byte", {
    jbyte val;
    env->GetByteArrayRegion((jbyteArray) arrayObj, idx, 1, &val);
    SET_REGISTER(vdst, val);
})
    OP_END

/* File: c/OP_AGET_CHAR.cpp */
HANDLE_OP_AGET(OP_AGET_CHAR, "-char", {
    jchar val;
    env->GetCharArrayRegion((jcharArray) arrayObj, idx, 1, &val);
    SET_REGISTER(vdst, val);
})
    OP_END

/* File: c/OP_AGET_SHORT.cpp */
HANDLE_OP_AGET(OP_AGET_SHORT, "-short", {
    jshort val;
    env->GetShortArrayRegion((jshortArray) arrayObj, idx, 1, &val);
    SET_REGISTER(vdst, val);
})
    OP_END

/* File: c/OP_APUT.cpp */
HANDLE_OP_APUT(OP_APUT, "", {
    u4 *arrData = (u4 *) env->GetPrimitiveArrayCritical(arrayObj, NULL);
    arrData[idx] = GET_REGISTER(vdst);
    env->ReleasePrimitiveArrayCritical(arrayObj, arrData, 0);

})
    OP_END

/* File: c/OP_APUT_WIDE.cpp */
HANDLE_OP_APUT(OP_APUT_WIDE, "-wide", {
    u8 *arrData = (u8 *) env->GetPrimitiveArrayCritical(arrayObj, NULL);
    arrData[idx] = GET_REGISTER_WIDE(vdst);
    env->ReleasePrimitiveArrayCritical(arrayObj, arrData, 0);
})
    OP_END

/* File: c/OP_APUT_OBJECT.cpp */
    HANDLE_OPCODE(OP_APUT_OBJECT /*vAA, vBB, vCC*/)
    {
        jobjectArray arrayObj;
        u2 arrayInfo;
        vdst = INST_AA(inst);
        arrayInfo = FETCH(1);
        vsrc1 = arrayInfo & 0xff;    /* array ptr */
        vsrc2 = arrayInfo >> 8;      /* index */
        ILOGV("|aput%s v%d,v%d,v%d", "-object", vdst, vsrc1, vsrc2);
        arrayObj = (jobjectArray) GET_REGISTER_AS_OBJECT(vsrc1);
        if (arrayObj == NULL) {
            dvmThrowNullPointerException(env, NULL);
            GOTO_exceptionThrown();
        }
        u4 idx = GET_REGISTER(vsrc2);
        ILOGV("+ APUT[%d]=0x%08x", GET_REGISTER(vsrc2), GET_REGISTER_AS_OBJECT(vdst));
        env->SetObjectArrayElement(arrayObj, idx, GET_REGISTER_AS_OBJECT(vdst));

        if (env->ExceptionCheck()) {
            GOTO_exceptionThrown();
        }
    }
    FINISH(2);
    OP_END

/* File: c/OP_APUT_BOOLEAN.cpp */
HANDLE_OP_APUT(OP_APUT_BOOLEAN, "-boolean", {
    jboolean val = GET_REGISTER(vdst);
    env->SetBooleanArrayRegion((jbooleanArray) arrayObj, idx, 1, &val);
})
    OP_END

/* File: c/OP_APUT_BYTE.cpp */
HANDLE_OP_APUT(OP_APUT_BYTE, "-byte", {
    jbyte val = GET_REGISTER(vdst);
    env->SetByteArrayRegion((jbyteArray) arrayObj, idx, 1, &val);
})
    OP_END

/* File: c/OP_APUT_CHAR.cpp */
HANDLE_OP_APUT(OP_APUT_CHAR, "-char", {
    jchar val = GET_REGISTER(vdst);
    env->SetCharArrayRegion((jcharArray) arrayObj, idx, 1, &val);
})
    OP_END

/* File: c/OP_APUT_SHORT.cpp */
HANDLE_OP_APUT(OP_APUT_SHORT, "-short", {
    jshort val = GET_REGISTER(vdst);
    env->SetShortArrayRegion((jshortArray) arrayObj, idx, 1, &val);
})
    OP_END

/* File: c/OP_IGET.cpp */
HANDLE_IGET_X(OP_IGET, "", {
    if (ifield->type == 'I') {
        jint i = env->GetIntField(obj, ifield->fieldId);
        SET_REGISTER(vdst, i);
    } else {
        jfloat f = env->GetFloatField(obj, ifield->fieldId);
        SET_REGISTER_FLOAT(vdst, f);
    }
},)
    OP_END

/* File: c/OP_IGET_WIDE.cpp */
HANDLE_IGET_X(OP_IGET_WIDE, "-wide", {
    if (ifield->type == 'J') {
        jlong j = env->GetLongField(obj, ifield->fieldId);
        SET_REGISTER_WIDE(vdst, j);
    } else {
        jdouble d = env->GetDoubleField(obj, ifield->fieldId);
        SET_REGISTER_DOUBLE(vdst, d);
    }
}, _WIDE)
    OP_END

/* File: c/OP_IGET_OBJECT.cpp */
HANDLE_IGET_X(OP_IGET_OBJECT, "-object", {
    //必须保证先获得field,不然寄存器vdst和vsrc1相同时,宏替换导致obj先失效,然后又使用了失效的obj从而导致错误
    jobject objectField = env->GetObjectField(obj, ifield->fieldId);
    SET_REGISTER_AS_OBJECT(vdst, objectField);
}, _AS_OBJECT)
    OP_END

/* File: c/OP_IGET_BOOLEAN.cpp */
HANDLE_IGET_X(OP_IGET_BOOLEAN, "", {
    jboolean b = env->GetBooleanField(obj, ifield->fieldId);
    SET_REGISTER(vdst, b);
},)
    OP_END

/* File: c/OP_IGET_BYTE.cpp */
HANDLE_IGET_X(OP_IGET_BYTE, "", {
    jbyte b = env->GetByteField(obj, ifield->fieldId);
    SET_REGISTER(vdst, b);
},)
    OP_END

/* File: c/OP_IGET_CHAR.cpp */
HANDLE_IGET_X(OP_IGET_CHAR, "", {
    jchar c = env->GetCharField(obj, ifield->fieldId);
    SET_REGISTER(vdst, c);
},)
    OP_END

/* File: c/OP_IGET_SHORT.cpp */
HANDLE_IGET_X(OP_IGET_SHORT, "", {
    jshort s = env->GetShortField(obj, ifield->fieldId);
    SET_REGISTER(vdst, s);
},)
    OP_END

/* File: c/OP_IPUT.cpp */
HANDLE_IPUT_X(OP_IPUT, "", {
    switch (ifield->type) {
        case 'I':
            env->SetIntField(obj, ifield->fieldId, GET_REGISTER(vdst));
            break;
        case 'F':
            env->SetFloatField(obj, ifield->fieldId, GET_REGISTER_FLOAT(vdst));
            break;
    }
},)
    OP_END

/* File: c/OP_IPUT_WIDE.cpp */
HANDLE_IPUT_X(OP_IPUT_WIDE, "-wide", {
    switch (ifield->type) {
        case 'J':
            env->SetLongField(obj, ifield->fieldId, (jlong) GET_REGISTER_WIDE(vdst));
            break;
        case 'D':
            env->SetDoubleField(obj, ifield->fieldId, GET_REGISTER_DOUBLE(vdst));
            break;
    }
}, _WIDE)
    OP_END

/* File: c/OP_IPUT_OBJECT.cpp */
HANDLE_IPUT_X(OP_IPUT_OBJECT, "-object", {
    env->SetObjectField(obj, ifield->fieldId, GET_REGISTER_AS_OBJECT(vdst));
}, _WIDE)
    OP_END

/* File: c/OP_IPUT_BOOLEAN.cpp */
HANDLE_IPUT_X(OP_IPUT_BOOLEAN, "", {
    env->SetBooleanField(obj, ifield->fieldId, GET_REGISTER(vdst));
},)
    OP_END

/* File: c/OP_IPUT_BYTE.cpp */
HANDLE_IPUT_X(OP_IPUT_BYTE, "", {
    env->SetByteField(obj, ifield->fieldId, GET_REGISTER(vdst));
},)
    OP_END

/* File: c/OP_IPUT_CHAR.cpp */
HANDLE_IPUT_X(OP_IPUT_CHAR, "", {
    env->SetCharField(obj, ifield->fieldId, GET_REGISTER(vdst));
},)
    OP_END

/* File: c/OP_IPUT_SHORT.cpp */
HANDLE_IPUT_X(OP_IPUT_SHORT, "", {
    env->SetShortField(obj, ifield->fieldId, GET_REGISTER(vdst));
},)
    OP_END

/* File: c/OP_SGET.cpp */
HANDLE_SGET_X(OP_SGET, "", {
    if (sfield->type == 'I') {
        SET_REGISTER(vdst, env->GetStaticIntField(clazz.get(), sfield->fieldId));
    } else {
        SET_REGISTER_FLOAT(vdst, env->GetStaticFloatField(clazz.get(), sfield->fieldId));
    }
},)
    OP_END

/* File: c/OP_SGET_WIDE.cpp */
HANDLE_SGET_X(OP_SGET_WIDE, "-wide", {
    if (sfield->type == 'J') {
        SET_REGISTER_WIDE(vdst, env->GetStaticLongField(clazz.get(), sfield->fieldId));
    } else {
        SET_REGISTER_DOUBLE(vdst, env->GetStaticDoubleField(clazz.get(), sfield->fieldId));
    }
}, _WIDE)
    OP_END

/* File: c/OP_SGET_OBJECT.cpp */
HANDLE_SGET_X(OP_SGET_OBJECT, "-object", {
    SET_REGISTER_AS_OBJECT(vdst, env->GetStaticObjectField(clazz.get(), sfield->fieldId));
}, _AS_OBJECT)
    OP_END

/* File: c/OP_SGET_BOOLEAN.cpp */
HANDLE_SGET_X(OP_SGET_BOOLEAN, "", {
    SET_REGISTER(vdst, env->GetStaticBooleanField(clazz.get(), sfield->fieldId));
},)
    OP_END

/* File: c/OP_SGET_BYTE.cpp */
HANDLE_SGET_X(OP_SGET_BYTE, "", {
    SET_REGISTER(vdst, env->GetStaticByteField(clazz.get(), sfield->fieldId));
},)
    OP_END

/* File: c/OP_SGET_CHAR.cpp */
HANDLE_SGET_X(OP_SGET_CHAR, "", {
    SET_REGISTER(vdst, env->GetStaticCharField(clazz.get(), sfield->fieldId));
},)
    OP_END

/* File: c/OP_SGET_SHORT.cpp */
HANDLE_SGET_X(OP_SGET_SHORT, "", {
    SET_REGISTER(vdst, env->GetStaticShortField(clazz.get(), sfield->fieldId));
},)
    OP_END

/* File: c/OP_SPUT.cpp */
HANDLE_SPUT_X(OP_SPUT, "", {
    switch (sfield->type) {
        case 'I':
            env->SetStaticIntField(clazz.get(), sfield->fieldId, GET_REGISTER(vdst));
            break;
        case 'F':
            env->SetStaticFloatField(clazz.get(), sfield->fieldId, GET_REGISTER_FLOAT(vdst));
            break;
    }
},)
    OP_END


/* File: c/OP_SPUT_WIDE.cpp */
HANDLE_SPUT_X(OP_SPUT_WIDE, "-wide", {
    switch (sfield->type) {
        case 'J':
            env->SetStaticLongField(clazz.get(), sfield->fieldId, GET_REGISTER_WIDE(vdst));
            break;
        case 'D':
            env->SetStaticDoubleField(clazz.get(), sfield->fieldId, GET_REGISTER_DOUBLE(vdst));
            break;
    }
}, _WIDE)
    OP_END

/* File: c/OP_SPUT_OBJECT.cpp */
HANDLE_SPUT_X(OP_SPUT_OBJECT, "-object", {
    env->SetStaticObjectField(clazz.get(), sfield->fieldId, GET_REGISTER_AS_OBJECT(vdst));
}, _AS_OBJECT)
    OP_END

/* File: c/OP_SPUT_BOOLEAN.cpp */
HANDLE_SPUT_X(OP_SPUT_BOOLEAN, "", {
    env->SetStaticBooleanField(clazz.get(), sfield->fieldId, GET_REGISTER(vdst));
},)
    OP_END

/* File: c/OP_SPUT_BYTE.cpp */
HANDLE_SPUT_X(OP_SPUT_BYTE, "", {
    env->SetStaticByteField(clazz.get(), sfield->fieldId, GET_REGISTER(vdst));
},)
    OP_END

/* File: c/OP_SPUT_CHAR.cpp */
HANDLE_SPUT_X(OP_SPUT_CHAR, "", {
    env->SetStaticCharField(clazz.get(), sfield->fieldId, GET_REGISTER(vdst));
},)
    OP_END

/* File: c/OP_SPUT_SHORT.cpp */
HANDLE_SPUT_X(OP_SPUT_SHORT, "", {
    env->SetStaticShortField(clazz.get(), sfield->fieldId, GET_REGISTER(vdst));

},)
    OP_END

/* File: c/OP_INVOKE_VIRTUAL.cpp */
    HANDLE_OPCODE(
            OP_INVOKE_VIRTUAL /*vB, {vD, vE, vF, vG, vA}, meth@CCCC*/)
    GOTO_invoke(invokeVirtual, false);
    OP_END

/* File: c/OP_INVOKE_SUPER.cpp */
    HANDLE_OPCODE(
            OP_INVOKE_SUPER /*vB, {vD, vE, vF, vG, vA}, meth@CCCC*/)
    GOTO_invoke(invokeSuper, false);
    OP_END

/* File: c/OP_INVOKE_DIRECT.cpp */
    HANDLE_OPCODE(
            OP_INVOKE_DIRECT /*vB, {vD, vE, vF, vG, vA}, meth@CCCC*/)
    GOTO_invoke(invokeDirect, false);
    OP_END

/* File: c/OP_INVOKE_STATIC.cpp */
    HANDLE_OPCODE(
            OP_INVOKE_STATIC /*vB, {vD, vE, vF, vG, vA}, meth@CCCC*/)
    GOTO_invoke(invokeStatic, false);
    OP_END

/* File: c/OP_INVOKE_INTERFACE.cpp */
    HANDLE_OPCODE(
            OP_INVOKE_INTERFACE /*vB, {vD, vE, vF, vG, vA}, meth@CCCC*/)
    GOTO_invoke(invokeInterface, false);
    OP_END

/* File: c/OP_UNUSED_73.cpp */
    HANDLE_OPCODE(OP_UNUSED_73)
    OP_END

/* File: c/OP_INVOKE_VIRTUAL_RANGE.cpp */
    HANDLE_OPCODE(
            OP_INVOKE_VIRTUAL_RANGE /*{vCCCC..v(CCCC+AA-1)}, meth@BBBB*/)
    GOTO_invoke(invokeVirtual, true);
    OP_END

/* File: c/OP_INVOKE_SUPER_RANGE.cpp */
    HANDLE_OPCODE(
            OP_INVOKE_SUPER_RANGE /*{vCCCC..v(CCCC+AA-1)}, meth@BBBB*/)
    GOTO_invoke(invokeSuper, true);
    OP_END

/* File: c/OP_INVOKE_DIRECT_RANGE.cpp */
    HANDLE_OPCODE(
            OP_INVOKE_DIRECT_RANGE /*{vCCCC..v(CCCC+AA-1)}, meth@BBBB*/)
    GOTO_invoke(invokeDirect, true);
    OP_END

/* File: c/OP_INVOKE_STATIC_RANGE.cpp */
    HANDLE_OPCODE(
            OP_INVOKE_STATIC_RANGE /*{vCCCC..v(CCCC+AA-1)}, meth@BBBB*/)
    GOTO_invoke(invokeStatic, true);
    OP_END

/* File: c/OP_INVOKE_INTERFACE_RANGE.cpp */
    HANDLE_OPCODE(
            OP_INVOKE_INTERFACE_RANGE /*{vCCCC..v(CCCC+AA-1)}, meth@BBBB*/)
    GOTO_invoke(invokeInterface, true);
    OP_END

/* File: c/OP_UNUSED_79.cpp */
    HANDLE_OPCODE(OP_UNUSED_79)
    OP_END

/* File: c/OP_UNUSED_7A.cpp */
    HANDLE_OPCODE(OP_UNUSED_7A)
    OP_END

/* File: c/OP_NEG_INT.cpp */
HANDLE_UNOP(OP_NEG_INT, "neg-int", -, ,)
    OP_END

/* File: c/OP_NOT_INT.cpp */
HANDLE_UNOP(OP_NOT_INT, "not-int", , ^ 0xffffffff,)
    OP_END

/* File: c/OP_NEG_LONG.cpp */
HANDLE_UNOP(OP_NEG_LONG, "neg-long", -, , _WIDE)
    OP_END

/* File: c/OP_NOT_LONG.cpp */
HANDLE_UNOP(OP_NOT_LONG, "not-long", , ^ 0xffffffffffffffffULL, _WIDE)
    OP_END

/* File: c/OP_NEG_FLOAT.cpp */
HANDLE_UNOP(OP_NEG_FLOAT, "neg-float", -, , _FLOAT)
    OP_END

/* File: c/OP_NEG_DOUBLE.cpp */
HANDLE_UNOP(OP_NEG_DOUBLE, "neg-double", -, , _DOUBLE)
    OP_END

/* File: c/OP_INT_TO_LONG.cpp */
HANDLE_NUMCONV(OP_INT_TO_LONG, "int-to-long", _INT, _WIDE)
    OP_END

/* File: c/OP_INT_TO_FLOAT.cpp */
HANDLE_NUMCONV(OP_INT_TO_FLOAT, "int-to-float", _INT, _FLOAT)
    OP_END

/* File: c/OP_INT_TO_DOUBLE.cpp */
HANDLE_NUMCONV(OP_INT_TO_DOUBLE, "int-to-double", _INT, _DOUBLE)
    OP_END

/* File: c/OP_LONG_TO_INT.cpp */
HANDLE_NUMCONV(OP_LONG_TO_INT, "long-to-int", _WIDE, _INT)
    OP_END

/* File: c/OP_LONG_TO_FLOAT.cpp */
HANDLE_NUMCONV(OP_LONG_TO_FLOAT, "long-to-float", _WIDE, _FLOAT)
    OP_END

/* File: c/OP_LONG_TO_DOUBLE.cpp */
HANDLE_NUMCONV(OP_LONG_TO_DOUBLE, "long-to-double", _WIDE, _DOUBLE)
    OP_END

/* File: c/OP_FLOAT_TO_INT.cpp */
HANDLE_FLOAT_TO_INT(OP_FLOAT_TO_INT, "float-to-int",
                    float, _FLOAT, s4, _INT)
    OP_END

/* File: c/OP_FLOAT_TO_LONG.cpp */
HANDLE_FLOAT_TO_INT(OP_FLOAT_TO_LONG, "float-to-long",
                    float, _FLOAT, s8, _WIDE)
    OP_END

/* File: c/OP_FLOAT_TO_DOUBLE.cpp */
HANDLE_NUMCONV(OP_FLOAT_TO_DOUBLE, "float-to-double", _FLOAT, _DOUBLE)
    OP_END

/* File: c/OP_DOUBLE_TO_INT.cpp */
HANDLE_FLOAT_TO_INT(OP_DOUBLE_TO_INT, "double-to-int",
                    double, _DOUBLE, s4, _INT)
    OP_END

/* File: c/OP_DOUBLE_TO_LONG.cpp */
HANDLE_FLOAT_TO_INT(OP_DOUBLE_TO_LONG, "double-to-long",
                    double, _DOUBLE, s8, _WIDE)
    OP_END

/* File: c/OP_DOUBLE_TO_FLOAT.cpp */
HANDLE_NUMCONV(OP_DOUBLE_TO_FLOAT, "double-to-float", _DOUBLE, _FLOAT)
    OP_END

/* File: c/OP_INT_TO_BYTE.cpp */
HANDLE_INT_TO_SMALL(OP_INT_TO_BYTE, "byte", s1)
    OP_END

/* File: c/OP_INT_TO_CHAR.cpp */
HANDLE_INT_TO_SMALL(OP_INT_TO_CHAR, "char", u2)
    OP_END

/* File: c/OP_INT_TO_SHORT.cpp */
HANDLE_INT_TO_SMALL(OP_INT_TO_SHORT, "short", s2)    /* want sign bit */
    OP_END

/* File: c/OP_ADD_INT.cpp */
HANDLE_OP_X_INT(OP_ADD_INT, "add", +, 0)
    OP_END

/* File: c/OP_SUB_INT.cpp */
HANDLE_OP_X_INT(OP_SUB_INT, "sub", -, 0)
    OP_END

/* File: c/OP_MUL_INT.cpp */
HANDLE_OP_X_INT(OP_MUL_INT, "mul", *, 0)
    OP_END

/* File: c/OP_DIV_INT.cpp */
HANDLE_OP_X_INT(OP_DIV_INT, "div", /, 1)
    OP_END

/* File: c/OP_REM_INT.cpp */
HANDLE_OP_X_INT(OP_REM_INT, "rem", %, 2)
    OP_END

/* File: c/OP_AND_INT.cpp */
HANDLE_OP_X_INT(OP_AND_INT, "and", &, 0)
    OP_END

/* File: c/OP_OR_INT.cpp */
HANDLE_OP_X_INT(OP_OR_INT, "or", |, 0)
    OP_END

/* File: c/OP_XOR_INT.cpp */
HANDLE_OP_X_INT(OP_XOR_INT, "xor", ^, 0)
    OP_END

/* File: c/OP_SHL_INT.cpp */
HANDLE_OP_SHX_INT(OP_SHL_INT, "shl", (s4), <<)
    OP_END

/* File: c/OP_SHR_INT.cpp */
HANDLE_OP_SHX_INT(OP_SHR_INT, "shr", (s4), >>)
    OP_END

/* File: c/OP_USHR_INT.cpp */
HANDLE_OP_SHX_INT(OP_USHR_INT, "ushr", (u4), >>)
    OP_END

/* File: c/OP_ADD_LONG.cpp */
HANDLE_OP_X_LONG(OP_ADD_LONG, "add", +, 0)
    OP_END

/* File: c/OP_SUB_LONG.cpp */
HANDLE_OP_X_LONG(OP_SUB_LONG, "sub", -, 0)
    OP_END

/* File: c/OP_MUL_LONG.cpp */
HANDLE_OP_X_LONG(OP_MUL_LONG, "mul", *, 0)
    OP_END

/* File: c/OP_DIV_LONG.cpp */
HANDLE_OP_X_LONG(OP_DIV_LONG, "div", /, 1)
    OP_END

/* File: c/OP_REM_LONG.cpp */
HANDLE_OP_X_LONG(OP_REM_LONG, "rem", %, 2)
    OP_END

/* File: c/OP_AND_LONG.cpp */
HANDLE_OP_X_LONG(OP_AND_LONG, "and", &, 0)
    OP_END

/* File: c/OP_OR_LONG.cpp */
HANDLE_OP_X_LONG(OP_OR_LONG, "or", |, 0)
    OP_END

/* File: c/OP_XOR_LONG.cpp */
HANDLE_OP_X_LONG(OP_XOR_LONG, "xor", ^, 0)
    OP_END

/* File: c/OP_SHL_LONG.cpp */
HANDLE_OP_SHX_LONG(OP_SHL_LONG, "shl", (s8), <<)
    OP_END

/* File: c/OP_SHR_LONG.cpp */
HANDLE_OP_SHX_LONG(OP_SHR_LONG, "shr", (s8), >>)
    OP_END

/* File: c/OP_USHR_LONG.cpp */
HANDLE_OP_SHX_LONG(OP_USHR_LONG, "ushr", (u8), >>)
    OP_END

/* File: c/OP_ADD_FLOAT.cpp */
HANDLE_OP_X_FLOAT(OP_ADD_FLOAT, "add", +)
    OP_END

/* File: c/OP_SUB_FLOAT.cpp */
HANDLE_OP_X_FLOAT(OP_SUB_FLOAT, "sub", -)
    OP_END

/* File: c/OP_MUL_FLOAT.cpp */
HANDLE_OP_X_FLOAT(OP_MUL_FLOAT, "mul", *)
    OP_END

/* File: c/OP_DIV_FLOAT.cpp */
HANDLE_OP_X_FLOAT(OP_DIV_FLOAT, "div", /)
    OP_END

/* File: c/OP_REM_FLOAT.cpp */
    HANDLE_OPCODE(OP_REM_FLOAT /*vAA, vBB, vCC*/)
    {
        u2 srcRegs;
        vdst = INST_AA(inst);
        srcRegs = FETCH(1);
        vsrc1 = srcRegs & 0xff;
        vsrc2 = srcRegs >> 8;
        ILOGV("|%s-float v%d,v%d,v%d", "mod", vdst, vsrc1, vsrc2);
        SET_REGISTER_FLOAT(vdst,
                           fmodf(GET_REGISTER_FLOAT(vsrc1),
                                 GET_REGISTER_FLOAT(vsrc2)));
    }
    FINISH(2);
    OP_END

/* File: c/OP_ADD_DOUBLE.cpp */
HANDLE_OP_X_DOUBLE(OP_ADD_DOUBLE, "add", +)
    OP_END

/* File: c/OP_SUB_DOUBLE.cpp */
HANDLE_OP_X_DOUBLE(OP_SUB_DOUBLE, "sub", -)
    OP_END

/* File: c/OP_MUL_DOUBLE.cpp */
HANDLE_OP_X_DOUBLE(OP_MUL_DOUBLE, "mul", *)
    OP_END

/* File: c/OP_DIV_DOUBLE.cpp */
HANDLE_OP_X_DOUBLE(OP_DIV_DOUBLE, "div", /)
    OP_END

/* File: c/OP_REM_DOUBLE.cpp */
    HANDLE_OPCODE(OP_REM_DOUBLE /*vAA, vBB, vCC*/)
    {
        u2 srcRegs;
        vdst = INST_AA(inst);
        srcRegs = FETCH(1);
        vsrc1 = srcRegs & 0xff;
        vsrc2 = srcRegs >> 8;
        ILOGV("|%s-double v%d,v%d,v%d", "mod", vdst, vsrc1, vsrc2);
        SET_REGISTER_DOUBLE(vdst,
                            fmod(GET_REGISTER_DOUBLE(vsrc1),
                                 GET_REGISTER_DOUBLE(vsrc2)));
    }
    FINISH(2);
    OP_END

/* File: c/OP_ADD_INT_2ADDR.cpp */
HANDLE_OP_X_INT_2ADDR(OP_ADD_INT_2ADDR, "add", +, 0)
    OP_END

/* File: c/OP_SUB_INT_2ADDR.cpp */
HANDLE_OP_X_INT_2ADDR(OP_SUB_INT_2ADDR, "sub", -, 0)
    OP_END

/* File: c/OP_MUL_INT_2ADDR.cpp */
HANDLE_OP_X_INT_2ADDR(OP_MUL_INT_2ADDR, "mul", *, 0)
    OP_END

/* File: c/OP_DIV_INT_2ADDR.cpp */
HANDLE_OP_X_INT_2ADDR(OP_DIV_INT_2ADDR, "div", /, 1)
    OP_END

/* File: c/OP_REM_INT_2ADDR.cpp */
HANDLE_OP_X_INT_2ADDR(OP_REM_INT_2ADDR, "rem", %, 2)
    OP_END

/* File: c/OP_AND_INT_2ADDR.cpp */
HANDLE_OP_X_INT_2ADDR(OP_AND_INT_2ADDR, "and", &, 0)
    OP_END

/* File: c/OP_OR_INT_2ADDR.cpp */
HANDLE_OP_X_INT_2ADDR(OP_OR_INT_2ADDR, "or", |, 0)
    OP_END

/* File: c/OP_XOR_INT_2ADDR.cpp */
HANDLE_OP_X_INT_2ADDR(OP_XOR_INT_2ADDR, "xor", ^, 0)
    OP_END

/* File: c/OP_SHL_INT_2ADDR.cpp */
HANDLE_OP_SHX_INT_2ADDR(OP_SHL_INT_2ADDR, "shl", (s4), <<)
    OP_END

/* File: c/OP_SHR_INT_2ADDR.cpp */
HANDLE_OP_SHX_INT_2ADDR(OP_SHR_INT_2ADDR, "shr", (s4), >>)
    OP_END

/* File: c/OP_USHR_INT_2ADDR.cpp */
HANDLE_OP_SHX_INT_2ADDR(OP_USHR_INT_2ADDR, "ushr", (u4), >>)
    OP_END

/* File: c/OP_ADD_LONG_2ADDR.cpp */
HANDLE_OP_X_LONG_2ADDR(OP_ADD_LONG_2ADDR, "add", +, 0)
    OP_END

/* File: c/OP_SUB_LONG_2ADDR.cpp */
HANDLE_OP_X_LONG_2ADDR(OP_SUB_LONG_2ADDR, "sub", -, 0)
    OP_END

/* File: c/OP_MUL_LONG_2ADDR.cpp */
HANDLE_OP_X_LONG_2ADDR(OP_MUL_LONG_2ADDR, "mul", *, 0)
    OP_END

/* File: c/OP_DIV_LONG_2ADDR.cpp */
HANDLE_OP_X_LONG_2ADDR(OP_DIV_LONG_2ADDR, "div", /, 1)
    OP_END

/* File: c/OP_REM_LONG_2ADDR.cpp */
HANDLE_OP_X_LONG_2ADDR(OP_REM_LONG_2ADDR, "rem", %, 2)
    OP_END

/* File: c/OP_AND_LONG_2ADDR.cpp */
HANDLE_OP_X_LONG_2ADDR(OP_AND_LONG_2ADDR, "and", &, 0)
    OP_END

/* File: c/OP_OR_LONG_2ADDR.cpp */
HANDLE_OP_X_LONG_2ADDR(OP_OR_LONG_2ADDR, "or", |, 0)
    OP_END

/* File: c/OP_XOR_LONG_2ADDR.cpp */
HANDLE_OP_X_LONG_2ADDR(OP_XOR_LONG_2ADDR, "xor", ^, 0)
    OP_END

/* File: c/OP_SHL_LONG_2ADDR.cpp */
HANDLE_OP_SHX_LONG_2ADDR(OP_SHL_LONG_2ADDR, "shl", (s8), <<)
    OP_END

/* File: c/OP_SHR_LONG_2ADDR.cpp */
HANDLE_OP_SHX_LONG_2ADDR(OP_SHR_LONG_2ADDR, "shr", (s8), >>)
    OP_END

/* File: c/OP_USHR_LONG_2ADDR.cpp */
HANDLE_OP_SHX_LONG_2ADDR(OP_USHR_LONG_2ADDR, "ushr", (u8), >>)
    OP_END

/* File: c/OP_ADD_FLOAT_2ADDR.cpp */
HANDLE_OP_X_FLOAT_2ADDR(OP_ADD_FLOAT_2ADDR, "add", +)
    OP_END

/* File: c/OP_SUB_FLOAT_2ADDR.cpp */
HANDLE_OP_X_FLOAT_2ADDR(OP_SUB_FLOAT_2ADDR, "sub", -)
    OP_END

/* File: c/OP_MUL_FLOAT_2ADDR.cpp */
HANDLE_OP_X_FLOAT_2ADDR(OP_MUL_FLOAT_2ADDR, "mul", *)
    OP_END

/* File: c/OP_DIV_FLOAT_2ADDR.cpp */
HANDLE_OP_X_FLOAT_2ADDR(OP_DIV_FLOAT_2ADDR, "div", /)
    OP_END

/* File: c/OP_REM_FLOAT_2ADDR.cpp */
    HANDLE_OPCODE(OP_REM_FLOAT_2ADDR /*vA, vB*/)
    vdst = INST_A(inst);
    vsrc1 = INST_B(inst);
    ILOGV("|%s-float-2addr v%d,v%d", "mod", vdst, vsrc1);
    SET_REGISTER_FLOAT(vdst,
                       fmodf(GET_REGISTER_FLOAT(vdst),
                             GET_REGISTER_FLOAT(vsrc1)));
    FINISH(1);
    OP_END

/* File: c/OP_ADD_DOUBLE_2ADDR.cpp */
HANDLE_OP_X_DOUBLE_2ADDR(OP_ADD_DOUBLE_2ADDR, "add", +)
    OP_END

/* File: c/OP_SUB_DOUBLE_2ADDR.cpp */
HANDLE_OP_X_DOUBLE_2ADDR(OP_SUB_DOUBLE_2ADDR, "sub", -)
    OP_END

/* File: c/OP_MUL_DOUBLE_2ADDR.cpp */
HANDLE_OP_X_DOUBLE_2ADDR(OP_MUL_DOUBLE_2ADDR, "mul", *)
    OP_END

/* File: c/OP_DIV_DOUBLE_2ADDR.cpp */
HANDLE_OP_X_DOUBLE_2ADDR(OP_DIV_DOUBLE_2ADDR, "div", /)
    OP_END

/* File: c/OP_REM_DOUBLE_2ADDR.cpp */
    HANDLE_OPCODE(OP_REM_DOUBLE_2ADDR /*vA, vB*/)
    vdst = INST_A(inst);
    vsrc1 = INST_B(inst);
    ILOGV("|%s-double-2addr v%d,v%d", "mod", vdst, vsrc1);
    SET_REGISTER_DOUBLE(vdst,
                        fmod(GET_REGISTER_DOUBLE(vdst),
                             GET_REGISTER_DOUBLE(vsrc1)));
    FINISH(1);
    OP_END

/* File: c/OP_ADD_INT_LIT16.cpp */
HANDLE_OP_X_INT_LIT16(OP_ADD_INT_LIT16, "add", +, 0)
    OP_END

/* File: c/OP_RSUB_INT.cpp */
    HANDLE_OPCODE(OP_RSUB_INT /*vA, vB, #+CCCC*/)
    {
        vdst = INST_A(inst);
        vsrc1 = INST_B(inst);
        vsrc2 = FETCH(1);
        ILOGV("|rsub-int v%d,v%d,#+0x%04x", vdst, vsrc1, vsrc2);
        SET_REGISTER(vdst, (s2) vsrc2 - (s4) GET_REGISTER(vsrc1));
    }
    FINISH(2);
    OP_END

/* File: c/OP_MUL_INT_LIT16.cpp */
HANDLE_OP_X_INT_LIT16(OP_MUL_INT_LIT16, "mul", *, 0)
    OP_END

/* File: c/OP_DIV_INT_LIT16.cpp */
HANDLE_OP_X_INT_LIT16(OP_DIV_INT_LIT16, "div", /, 1)
    OP_END

/* File: c/OP_REM_INT_LIT16.cpp */
HANDLE_OP_X_INT_LIT16(OP_REM_INT_LIT16, "rem", %, 2)
    OP_END

/* File: c/OP_AND_INT_LIT16.cpp */
HANDLE_OP_X_INT_LIT16(OP_AND_INT_LIT16, "and", &, 0)
    OP_END

/* File: c/OP_OR_INT_LIT16.cpp */
HANDLE_OP_X_INT_LIT16(OP_OR_INT_LIT16, "or", |, 0)
    OP_END

/* File: c/OP_XOR_INT_LIT16.cpp */
HANDLE_OP_X_INT_LIT16(OP_XOR_INT_LIT16, "xor", ^, 0)
    OP_END

/* File: c/OP_ADD_INT_LIT8.cpp */
HANDLE_OP_X_INT_LIT8(OP_ADD_INT_LIT8, "add", +, 0)
    OP_END

/* File: c/OP_RSUB_INT_LIT8.cpp */
    HANDLE_OPCODE(OP_RSUB_INT_LIT8 /*vAA, vBB, #+CC*/)
    {
        u2 litInfo;
        vdst = INST_AA(inst);
        litInfo = FETCH(1);
        vsrc1 = litInfo & 0xff;
        vsrc2 = litInfo >> 8;
        ILOGV("|%s-int/lit8 v%d,v%d,#+0x%02x", "rsub", vdst, vsrc1,
              vsrc2);
        SET_REGISTER(vdst, (s1) vsrc2 - (s4) GET_REGISTER(vsrc1));
    }
    FINISH(2);
    OP_END

/* File: c/OP_MUL_INT_LIT8.cpp */
HANDLE_OP_X_INT_LIT8(OP_MUL_INT_LIT8, "mul", *, 0)
    OP_END

/* File: c/OP_DIV_INT_LIT8.cpp */
HANDLE_OP_X_INT_LIT8(OP_DIV_INT_LIT8, "div", /, 1)
    OP_END

/* File: c/OP_REM_INT_LIT8.cpp */
HANDLE_OP_X_INT_LIT8(OP_REM_INT_LIT8, "rem", %, 2)
    OP_END

/* File: c/OP_AND_INT_LIT8.cpp */
HANDLE_OP_X_INT_LIT8(OP_AND_INT_LIT8, "and", &, 0)
    OP_END

/* File: c/OP_OR_INT_LIT8.cpp */
HANDLE_OP_X_INT_LIT8(OP_OR_INT_LIT8, "or", |, 0)
    OP_END

/* File: c/OP_XOR_INT_LIT8.cpp */
HANDLE_OP_X_INT_LIT8(OP_XOR_INT_LIT8, "xor", ^, 0)
    OP_END

/* File: c/OP_SHL_INT_LIT8.cpp */
HANDLE_OP_SHX_INT_LIT8(OP_SHL_INT_LIT8, "shl", (s4), <<)
    OP_END

/* File: c/OP_SHR_INT_LIT8.cpp */
HANDLE_OP_SHX_INT_LIT8(OP_SHR_INT_LIT8, "shr", (s4), >>)
    OP_END

/* File: c/OP_USHR_INT_LIT8.cpp */
HANDLE_OP_SHX_INT_LIT8(OP_USHR_INT_LIT8, "ushr", (u4), >>)
    OP_END

    //不用实现odex指令
    HANDLE_OPCODE(OP_IGET_BOOLEAN_QUICK)
    OP_END
    HANDLE_OPCODE(OP_IGET_BYTE_QUICK)
    OP_END
    HANDLE_OPCODE(OP_IGET_CHAR_QUICK)
    OP_END
    HANDLE_OPCODE(OP_IGET_SHORT_QUICK)
    OP_END
    HANDLE_OPCODE(OP_IGET_OBJECT_QUICK)
    OP_END
    HANDLE_OPCODE(OP_IGET_QUICK)
    OP_END
    HANDLE_OPCODE(OP_IGET_WIDE_QUICK)
    OP_END

    HANDLE_OPCODE(OP_IPUT_BOOLEAN_QUICK)
    OP_END
    HANDLE_OPCODE(OP_IPUT_BYTE_QUICK)
    OP_END
    HANDLE_OPCODE(OP_IPUT_CHAR_QUICK)
    OP_END
    HANDLE_OPCODE(OP_IPUT_SHORT_QUICK)
    OP_END
    HANDLE_OPCODE(OP_IPUT_QUICK)
    OP_END
    HANDLE_OPCODE(OP_IPUT_OBJECT_QUICK)
    OP_END
    HANDLE_OPCODE(OP_IPUT_WIDE_QUICK)
    OP_END

    HANDLE_OPCODE(OP_RETURN_VOID_NO_BARRIER)
    OP_END
    HANDLE_OPCODE(OP_INVOKE_VIRTUAL_QUICK)
    OP_END
    HANDLE_OPCODE(OP_INVOKE_VIRTUAL_QUICK_RANGE)
    OP_END

    HANDLE_OPCODE(OP_UNUSED_F3)
    OP_END
    HANDLE_OPCODE(OP_UNUSED_F4)
    OP_END
    HANDLE_OPCODE(OP_UNUSED_F5)
    OP_END
    HANDLE_OPCODE(OP_UNUSED_F6)
    OP_END
    HANDLE_OPCODE(OP_UNUSED_F7)
    OP_END
    HANDLE_OPCODE(OP_UNUSED_F8)
    OP_END
    HANDLE_OPCODE(OP_UNUSED_F9)
    OP_END

    //todo 实现高版本指令
    HANDLE_OPCODE(OP_INVOKE_POLYMORPHIC)
    OP_END
    HANDLE_OPCODE(OP_INVOKE_POLYMORPHIC_RANGE)
    OP_END

    HANDLE_OPCODE(OP_INVOKE_CUSTOM)
    OP_END
    HANDLE_OPCODE(OP_INVOKE_CUSTOM_RANGE)
    OP_END
    HANDLE_OPCODE(OP_CONST_METHOD_HANDLE)
    OP_END
    HANDLE_OPCODE(OP_CONST_METHOD_TYPE)
    OP_END
/*
 * In portable interp, most unused opcodes will fall through to here.
 */

/* File: c/gotoTargets.cpp */
/*
 * C footer.  This has some common code shared by the various targets.
 */

/*
 * Everything from here on is a "goto target".  In the basic interpreter
 * we jump into these targets and then jump directly to the handler for
 * next instruction.  Here, these are subroutines that return to the caller.
 */

    GOTO_TARGET(filledNewArray, bool methodCallRange, bool)
    {
        jarray newArray;
        char typeCh;
        const char *type;
        int i;
        u4 val;
        u4 arg5;

        ref = FETCH(1);             /* class ref */
        vdst = FETCH(2);            /* first 4 regs -or- range base */

        if (methodCallRange) {
            vsrc1 = INST_AA(inst);  /* #of elements */
            arg5 = -1;              /* silence compiler warning */
            ILOGV("|filled-new-array-range args=%d @0x%04x {regs=v%d-v%d}",
                  vsrc1, ref, vdst, vdst + vsrc1 - 1);
        } else {
            arg5 = INST_A(inst);
            vsrc1 = INST_B(inst);   /* #of elements */
            ILOGV("|filled-new-array args=%d @0x%04x {regs=0x%04x %x}",
                  vsrc1, ref, vdst, arg5);
        }
        type = dvmResolver->dvmResolveTypeUtf(env, ref);
        if (type == NULL) {
            dvmThrowNullPointerException(env,
                                         NULL);
            GOTO_exceptionThrown();
        }

        /*
         * Create an array of the specified type.
         */
        LOGVV("+++ filled-new-array type is '%s'", type);
        typeCh = type[1];
        if (typeCh == 'D' || typeCh == 'J') {
            /* category 2 primitives not allowed */
            dvmThrowRuntimeException(env,
                                     "bad filled array req");
            GOTO_exceptionThrown();
        } else if (typeCh != 'L' && typeCh != '[' && typeCh != 'I') {
            ALOGV("non-int primitives not implemented");
            dvmThrowInternalError(env,
                                  "filled-new-array not implemented for anything but 'int'");
            GOTO_exceptionThrown();
        }
        NEW_ARRAY(typeCh, type + 1, vsrc1);


        if (newArray == NULL)
            GOTO_exceptionThrown();

        /*
         * Fill in the elements.  It's legal for vsrc1 to be zero.
         */


        if (methodCallRange) {
            for (i = 0; i < vsrc1; i++) {
                SET_ARRAY_ELEMENT(typeCh, newArray, i, vdst + i);
            }
        } else {
            assert(vsrc1 <= 5);
            if (vsrc1 == 5) {
                SET_ARRAY_ELEMENT(typeCh, newArray, 4, arg5);
                vsrc1--;
            }
            for (i = 0; i < vsrc1; i++) {
                SET_ARRAY_ELEMENT(typeCh, newArray, i, vdst & 0x0f);
                vdst >>= 4;
            }
        }

        retval.l = newArray;
    }
    FINISH(3);
    GOTO_TARGET_END


    GOTO_TARGET(invokeVirtual, bool methodCallRange, bool)
    {
        jobject thisPtr;


        vsrc1 = INST_AA(inst);      /* AA (count) or BA (count + arg 5) */
        ref = FETCH(1);             /* method ref */
        vdst = FETCH(2);            /* 4 regs -or- first reg */

/*
 * The object against which we are executing a method is always
 * in the first argument.
 */
        if (methodCallRange) {
            assert(vsrc1 > 0);
            ILOGV("|invoke-virtual-range args=%d @0x%04x {regs=v%d-v%d}",
                  vsrc1, ref, vdst, vdst + vsrc1 - 1);
            thisPtr = GET_REGISTER_AS_OBJECT(vdst);
        } else {
            assert((vsrc1 >> 4) > 0);
            ILOGV("|invoke-virtual args=%d @0x%04x {regs=0x%04x %x}",
                  vsrc1 >> 4, ref, vdst, vsrc1 & 0x0f);
            thisPtr = GET_REGISTER_AS_OBJECT(vdst & 0x0f);
        }

        if (thisPtr == NULL) {
            dvmThrowNullPointerException(env, NULL);
            GOTO_exceptionThrown();
        }

        methodToCall = dvmResolver->dvmResolveMethod(env, ref, false);

        if (methodToCall == NULL) {
            GOTO_exceptionThrown();
        }

        INVOKE_METHOD(
                {
                    switch (returnCh) {
                        case 'Z':
                            retval.i = env->CallBooleanMethodA(
                                    thisPtr,
                                    methodToCall->methodId,
                                    args + 1);
                            break;
                        case 'B':
                            retval.i = env->CallByteMethodA(
                                    thisPtr,
                                    methodToCall->methodId,
                                    args + 1);
                            break;
                        case 'C':
                            retval.i = env->CallCharMethodA(
                                    thisPtr,
                                    methodToCall->methodId,
                                    args + 1);
                            break;
                        case 'S':
                            retval.i = env->CallShortMethodA(
                                    thisPtr,
                                    methodToCall->methodId,
                                    args + 1);
                            break;
                        case 'I':
                            retval.i = env->CallIntMethodA(
                                    thisPtr,
                                    methodToCall->methodId,
                                    args + 1);
                            break;
                        case 'F':
                            retval.f = env->CallFloatMethodA(
                                    thisPtr,
                                    methodToCall->methodId,
                                    args + 1);
                            break;
                        case 'J':
                            retval.j = env->CallLongMethodA(
                                    thisPtr,
                                    methodToCall->methodId,
                                    args + 1);
                            break;
                        case 'D':
                            retval.d = env->CallDoubleMethodA(
                                    thisPtr,
                                    methodToCall->methodId,
                                    args + 1);
                            break;
                        case 'L':
                            retval.l = env->CallObjectMethodA(
                                    thisPtr,
                                    methodToCall->methodId,
                                    args + 1);
                            if (INST_INST(FETCH(3)) != OP_MOVE_RESULT_OBJECT) {
                                if (retval.l != NULL) env->DeleteLocalRef(retval.l);
                            }
                            break;
                        case 'V':
                            env->CallVoidMethodA(
                                    thisPtr,
                                    methodToCall->methodId,
                                    args + 1);
                            break;
                        default:;
                    }

                });
    }
    GOTO_TARGET_END

    GOTO_TARGET(invokeSuper, bool methodCallRange)
    {
        jobject thisPtr;

        vsrc1 = INST_AA(inst);      /* AA (count) or BA (count + arg 5) */
        ref = FETCH(1);             /* method ref */
        vdst = FETCH(2);            /* 4 regs -or- first reg */

        if (methodCallRange) {
            ILOGV("|invoke-super-range args=%d @0x%04x {regs=v%d-v%d}",
                  vsrc1, ref, vdst, vdst + vsrc1 - 1);
            thisPtr = GET_REGISTER_AS_OBJECT(vdst);
        } else {
            ILOGV("|invoke-super args=%d @0x%04x {regs=0x%04x %x}",
                  vsrc1 >> 4, ref, vdst, vsrc1 & 0x0f);
            thisPtr = GET_REGISTER_AS_OBJECT(vdst & 0x0f);
        }

        if (thisPtr == NULL) {
            dvmThrowNullPointerException(env, NULL);
            GOTO_exceptionThrown();
        }

        methodToCall = dvmResolver->dvmResolveMethod(env, ref, false);

        if (methodToCall == NULL) {
            GOTO_exceptionThrown();
        }

        INVOKE_METHOD(
                {
                    jclass clazz = dvmResolver->dvmResolveClass(env, methodToCall->classIdx);
                    switch (returnCh) {
                        case 'Z':
                            retval.i = env->CallNonvirtualBooleanMethodA(
                                    thisPtr,
                                    clazz,
                                    methodToCall->methodId,
                                    args + 1);
                            break;
                        case 'B':
                            retval.i = env->CallNonvirtualByteMethodA(
                                    thisPtr,
                                    clazz,
                                    methodToCall->methodId,
                                    args + 1);
                            break;
                        case 'C':
                            retval.i = env->CallNonvirtualCharMethodA(
                                    thisPtr,
                                    clazz,
                                    methodToCall->methodId,
                                    args + 1);
                            break;
                        case 'S':
                            retval.i = env->CallNonvirtualShortMethodA(
                                    thisPtr,
                                    clazz,
                                    methodToCall->methodId,
                                    args + 1);
                            break;
                        case 'I':
                            retval.i = env->CallNonvirtualIntMethodA(
                                    thisPtr,
                                    clazz,
                                    methodToCall->methodId,
                                    args + 1);
                            break;
                        case 'F':
                            retval.f = env->CallNonvirtualFloatMethodA(
                                    thisPtr,
                                    clazz,
                                    methodToCall->methodId,
                                    args + 1);
                            break;
                        case 'J':
                            retval.j = env->CallNonvirtualLongMethodA(
                                    thisPtr,
                                    clazz,
                                    methodToCall->methodId,
                                    args + 1);
                            break;
                        case 'D':
                            retval.d = env->CallNonvirtualDoubleMethodA(
                                    thisPtr,
                                    clazz,
                                    methodToCall->methodId,
                                    args + 1);
                            break;
                        case 'L':
                            retval.l = env->CallNonvirtualObjectMethodA(
                                    thisPtr,
                                    clazz,
                                    methodToCall->methodId,
                                    args + 1);
                            if (INST_INST(FETCH(3)) != OP_MOVE_RESULT_OBJECT) {
                                if (retval.l != NULL) env->DeleteLocalRef(retval.l);
                            }
                            break;
                        case 'V':
                            env->CallNonvirtualVoidMethodA(
                                    thisPtr,
                                    clazz,
                                    methodToCall->methodId,
                                    args + 1);
                            break;
                        default:;
                    }
                    env->DeleteLocalRef(clazz);

                });

    }
    GOTO_TARGET_END

    GOTO_TARGET(invokeInterface, bool methodCallRange)
    {
        jobject thisPtr;


        vsrc1 = INST_AA(inst);      /* AA (count) or BA (count + arg 5) */
        ref = FETCH(1);             /* method ref */
        vdst = FETCH(2);            /* 4 regs -or- first reg */

/*
 * The object against which we are executing a method is always
 * in the first argument.
 */
        if (methodCallRange) {
            assert(vsrc1 > 0);
            ILOGV("|invoke-interface-range args=%d @0x%04x {regs=v%d-v%d}",
                  vsrc1, ref, vdst, vdst + vsrc1 - 1);
            thisPtr = GET_REGISTER_AS_OBJECT(vdst);
        } else {
            assert((vsrc1 >> 4) > 0);
            ILOGV("|invoke-interface args=%d @0x%04x {regs=0x%04x %x}",
                  vsrc1 >> 4, ref, vdst, vsrc1 & 0x0f);
            thisPtr = GET_REGISTER_AS_OBJECT(vdst & 0x0f);
        }
        if (thisPtr == NULL) {
            dvmThrowNullPointerException(env,
                                         NULL);
            GOTO_exceptionThrown();
        }
        methodToCall = dvmResolver->dvmResolveMethod(env, ref, false);

        if (methodToCall == NULL) {
            GOTO_exceptionThrown();
        }
        INVOKE_METHOD(
                {
                    switch (returnCh) {
                        case 'Z':
                            retval.i = env->CallBooleanMethodA(thisPtr, methodToCall->methodId,
                                                               args + 1);
                            break;
                        case 'B':
                            retval.i = env->CallByteMethodA(thisPtr, methodToCall->methodId,
                                                            args + 1);
                            break;
                        case 'C':
                            retval.i = env->CallCharMethodA(thisPtr, methodToCall->methodId,
                                                            args + 1);
                            break;
                        case 'S':
                            retval.i = env->CallShortMethodA(thisPtr, methodToCall->methodId,
                                                             args + 1);
                            break;
                        case 'I':
                            retval.i = env->CallIntMethodA(thisPtr, methodToCall->methodId,
                                                           args + 1);
                            break;
                        case 'F':
                            retval.f = env->CallFloatMethodA(thisPtr, methodToCall->methodId,
                                                             args + 1);
                            break;
                        case 'J':
                            retval.j = env->CallLongMethodA(thisPtr, methodToCall->methodId,
                                                            args + 1);
                            break;
                        case 'D':
                            retval.d = env->CallDoubleMethodA(thisPtr, methodToCall->methodId,
                                                              args + 1);
                            break;
                        case 'L':
                            retval.l = env->CallObjectMethodA(thisPtr, methodToCall->methodId,
                                                              args + 1);
                            if (INST_INST(FETCH(3)) != OP_MOVE_RESULT_OBJECT) {
                                if (retval.l != NULL) env->DeleteLocalRef(retval.l);
                            }
                            break;
                        case 'V':
                            env->CallVoidMethodA(thisPtr, methodToCall->methodId, args + 1);
                            break;
                        default:;
                    }

                });
    }
    GOTO_TARGET_END

    GOTO_TARGET(invokeDirect, bool methodCallRange)
    {
        jobject thisPtr;


        vsrc1 = INST_AA(inst);      /* AA (count) or BA (count + arg 5) */
        ref = FETCH(1);             /* method ref */
        vdst = FETCH(2);            /* 4 regs -or- first reg */

        if (methodCallRange) {
            ILOGV("|invoke-direct-range args=%d @0x%04x {regs=v%d-v%d}",
                  vsrc1, ref, vdst, vdst + vsrc1 - 1);
            thisPtr = GET_REGISTER_AS_OBJECT(vdst);
        } else {
            ILOGV("|invoke-direct args=%d @0x%04x {regs=0x%04x %x}",
                  vsrc1 >> 4, ref, vdst, vsrc1 & 0x0f);
            thisPtr = GET_REGISTER_AS_OBJECT(vdst & 0x0f);
        }
        if (thisPtr == NULL) {
            dvmThrowNullPointerException(env, NULL);
            GOTO_exceptionThrown();
        }

        methodToCall = dvmResolver->dvmResolveMethod(env, ref, false);

        if (methodToCall == NULL) {
            GOTO_exceptionThrown();
        }

        INVOKE_METHOD(
                {
                    jclass clazz = dvmResolver->dvmResolveClass(env, methodToCall->classIdx);
                    switch (returnCh) {
                        case 'Z':
                            retval.i = env->CallNonvirtualBooleanMethodA(
                                    thisPtr,
                                    clazz,
                                    methodToCall->methodId,
                                    args + 1);
                            break;
                        case 'B':
                            retval.i = env->CallNonvirtualByteMethodA(
                                    thisPtr,
                                    clazz,
                                    methodToCall->methodId,
                                    args + 1);
                            break;
                        case 'C':
                            retval.i = env->CallNonvirtualCharMethodA(
                                    thisPtr,
                                    clazz,
                                    methodToCall->methodId,
                                    args + 1);
                            break;
                        case 'S':
                            retval.i = env->CallNonvirtualShortMethodA(
                                    thisPtr,
                                    clazz,
                                    methodToCall->methodId,
                                    args + 1);
                            break;
                        case 'I':
                            retval.i = env->CallNonvirtualIntMethodA(
                                    thisPtr,
                                    clazz,
                                    methodToCall->methodId,
                                    args + 1);
                            break;
                        case 'F':
                            retval.f = env->CallNonvirtualFloatMethodA(
                                    thisPtr,
                                    clazz,
                                    methodToCall->methodId,
                                    args + 1);
                            break;
                        case 'J':
                            retval.j = env->CallNonvirtualLongMethodA(
                                    thisPtr,
                                    clazz,
                                    methodToCall->methodId,
                                    args + 1);
                            break;
                        case 'D':
                            retval.d = env->CallNonvirtualDoubleMethodA(
                                    thisPtr,
                                    clazz,
                                    methodToCall->methodId,
                                    args + 1);
                            break;
                        case 'L':
                            retval.l = env->CallNonvirtualObjectMethodA(
                                    thisPtr,
                                    clazz,
                                    methodToCall->methodId,
                                    args + 1);

                            if (INST_INST(FETCH(3)) != OP_MOVE_RESULT_OBJECT) {
                                if (retval.l != NULL) env->DeleteLocalRef(retval.l);
                            }
                            break;
                        case 'V':
                            env->CallNonvirtualVoidMethodA(
                                    thisPtr,
                                    clazz,
                                    methodToCall->methodId,
                                    args + 1);
                            break;
                        default:;
                    }
                    env->DeleteLocalRef(clazz);

                });

    }
    GOTO_TARGET_END

    GOTO_TARGET(invokeStatic, bool methodCallRange)
    {

        vsrc1 = INST_AA(inst);      /* AA (count) or BA (count + arg 5) */
        ref = FETCH(1);             /* method ref */
        vdst = FETCH(2);            /* 4 regs -or- first reg */

        if (methodCallRange)
            ILOGV("|invoke-static-range args=%d @0x%04x {regs=v%d-v%d}",
                  vsrc1, ref, vdst, vdst + vsrc1 - 1);
        else
            ILOGV("|invoke-static args=%d @0x%04x {regs=0x%04x %x}",
                  vsrc1 >> 4, ref, vdst, vsrc1 & 0x0f);

        methodToCall = dvmResolver->dvmResolveMethod(env, ref, true);

        if (methodToCall == NULL) {
            GOTO_exceptionThrown();
        }

        INVOKE_STATIC_METHOD(
                {
                    jclass clazz = dvmResolver->dvmResolveClass(env, methodToCall->classIdx);
                    switch (returnCh) {
                        case 'Z':
                            retval.i = env->CallStaticBooleanMethodA(clazz,
                                                                     methodToCall->methodId,
                                                                     args);
                            break;
                        case 'B':
                            retval.i = env->CallStaticByteMethodA(clazz,
                                                                  methodToCall->methodId,
                                                                  args);
                            break;
                        case 'C':
                            retval.i = env->CallStaticCharMethodA(clazz,
                                                                  methodToCall->methodId,
                                                                  args);
                            break;
                        case 'S':
                            retval.i = env->CallStaticShortMethodA(clazz,
                                                                   methodToCall->methodId,
                                                                   args);
                            break;
                        case 'I':
                            retval.i = env->CallStaticIntMethodA(clazz,
                                                                 methodToCall->methodId,
                                                                 args);
                            break;
                        case 'F':
                            retval.f = env->CallStaticFloatMethodA(clazz,
                                                                   methodToCall->methodId,
                                                                   args);
                            break;
                        case 'J':
                            retval.j = env->CallStaticLongMethodA(clazz,
                                                                  methodToCall->methodId,
                                                                  args);
                            break;
                        case 'D':
                            retval.d = env->CallStaticDoubleMethodA(clazz,
                                                                    methodToCall->methodId,
                                                                    args);
                            break;
                        case 'L':
                            retval.l = env->CallStaticObjectMethodA(clazz,
                                                                    methodToCall->methodId,
                                                                    args);
                            if (INST_INST(FETCH(3)) != OP_MOVE_RESULT_OBJECT) {
                                if (retval.l != NULL) env->DeleteLocalRef(retval.l);
                            }
                            break;
                        case 'V':
                            env->CallStaticVoidMethodA(clazz, methodToCall->methodId, args);
                            break;
                        default:;
                    }
                    env->DeleteLocalRef(clazz);

                });
    }
    GOTO_TARGET_END



/*
 * General handling for return-void, return, and return-wide.  Put the
 * return value in "retval" before jumping here.
 */
    GOTO_TARGET(returnFromMethod)
    {
        return retval;
    }
    GOTO_TARGET_END


/*
 * Jump here when the code throws an exception.
 *
 * By the time we get here, the Throwable has been created and the stack
 * trace has been saved off.
 */
    GOTO_TARGET(exceptionThrown)
    {
        int catchRelPc;

        PERIODIC_CHECKS(0);

        ScopedLocalRef<jthrowable> exception(env, env->ExceptionOccurred());
        env->ExceptionClear();
        ILOGV("Handling exception %p at :%d",
              exception.get(), (pc - code->insns));


/*
 * We need to unroll to the catch block or the nearest "break"
 * frame.
 *
 * A break frame could indicate that we have reached an intermediate
 * native call, or have gone off the top of the stack and the thread
 * needs to exit.  Either way, we return from here, leaving the
 * exception raised.
 *
 * If we do find a catch block, we want to transfer execution to
 * that point.
 *
 * Note this can cause an exception while resolving classes in
 * the "catch" blocks.
 */
        catchRelPc = dvmFindCatchBlock(env, dvmResolver, pc - code->insns,
                                       exception.get(), (TryCatchHandler *) code->triesHandlers);

        if (catchRelPc < 0) {
/* falling through to JNI code or off the bottom of the stack */
            env->Throw(exception.get());
            GOTO_bail();
        }
        pc = code->insns + catchRelPc;

/*
 * Restore the exception if the handler wants it.
 *
 * The Dalvik spec mandates that, if an exception handler wants to
 * do something with the exception, the first instruction executed
 * must be "move-exception".  We can pass the exception along
 * through the thread struct, and let the move-exception instruction
 * clear it for us.
 *
 * If the handler doesn't call move-exception, we don't want to
 * finish here with an exception still pending.
 */
        if (INST_INST(FETCH(0)) == OP_MOVE_EXCEPTION) {
            env->Throw(exception.get());
        }

    }
    FINISH(0);
    GOTO_TARGET_END


/* File: portable/enddefs.cpp */
/*--- end of opcodes ---*/

    bail:
    ILOGD("|-- Leaving interpreter loop");      // note "curMethod" may be NULL
    return retval;

}


#ifdef __cplusplus
}
#endif
