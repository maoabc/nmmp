

/*
 * Functions for dealing with try-catch info.
 */

#ifndef VM_DEXCATCH_H_
#define VM_DEXCATCH_H_

#include "Common.h"
#include "vm.h"
#include "DexOpcodes.h"
#include "Leb128.h"

/*
 * Catch handler entry, used while iterating over catch_handler_items.
 */
typedef struct {
    u4 typeIdx;    /* type index of the caught exception type */
    u4 address;    /* handler address */
} VmCatchHandler;


/*
 * Direct-mapped "try_item".
 */
typedef struct {
    u4 startAddr;          /* start address, in 16-bit code units */
    u2 insnCount;          /* instruction count, in 16-bit code units */
    u2 handlerOff;         /* offset in encoded handler data to handlers */
} TryItem;

typedef struct {
    u2 triesSize;
    u2 unused;
    /* followed by try_item[triesSize] */
    TryItem tryItems[1];

    /* followed by uleb128 handlersSize */
    /* followed by catch_handler_item[handlersSize] */
} TryCatchHandler;

/*
 * Iterator over catch handler data. This structure should be treated as
 * opaque.
 */
struct VmCatchIterator {
    const u1 *pEncodedData;
    bool catchesAll;
    u4 countRemaining;
    VmCatchHandler handler;
};


/* Get the list of "tries" for the given DexCode. */
DEX_INLINE const TryItem *dexGetTries(const TryCatchHandler *pHandler) {
    return pHandler->tryItems;
}
/* Get the base of the encoded data for the given DexCode. */
DEX_INLINE const u1 *vmGetCatchHandlerData(const TryCatchHandler *pHandler) {
    const TryItem *pTries = dexGetTries(pHandler);
    return (const u1 *) &pTries[pHandler->triesSize];
}




/* Initialize a VmCatchIterator to emptiness. This mostly exists to
 * squelch innocuous warnings. */
DEX_INLINE void vmCatchIteratorClear(VmCatchIterator *pIterator) {
    pIterator->pEncodedData = NULL;
    pIterator->catchesAll = false;
    pIterator->countRemaining = 0;
    pIterator->handler.typeIdx = 0;
    pIterator->handler.address = 0;
}

/* Initialize a DexCatchIterator with a direct pointer to encoded handlers. */
DEX_INLINE void vmCatchIteratorInitToPointer(VmCatchIterator *pIterator,
                                             const u1 *pEncodedData) {
    s4 count = readSignedLeb128(&pEncodedData);

    if (count <= 0) {
        pIterator->catchesAll = true;
        count = -count;
    } else {
        pIterator->catchesAll = false;
    }

    pIterator->pEncodedData = pEncodedData;
    pIterator->countRemaining = count;
}

/* Initialize a DexCatchIterator to a particular handler offset. */
DEX_INLINE void vmCatchIteratorInit(VmCatchIterator *pIterator,
                                    const TryCatchHandler *handler, u4 offset) {
    vmCatchIteratorInitToPointer(pIterator,
                                 vmGetCatchHandlerData(handler) + offset);
}

/* Get the next item from a DexCatchIterator. Returns NULL if at end. */
DEX_INLINE VmCatchHandler *vmCatchIteratorNext(VmCatchIterator *pIterator) {
    if (pIterator->countRemaining == 0) {
        if (!pIterator->catchesAll) {
            return NULL;
        }

        pIterator->catchesAll = false;
        pIterator->handler.typeIdx = kNoIndex;
    } else {
        u4 typeIdx = readUnsignedLeb128(&pIterator->pEncodedData);
        pIterator->handler.typeIdx = typeIdx;
        pIterator->countRemaining--;
    }

    pIterator->handler.address = readUnsignedLeb128(&pIterator->pEncodedData);
    return &pIterator->handler;
}

/* Get the handler offset just past the end of the one just iterated over.
 * This ends the iteration if it wasn't already. */
u4 vmCatchIteratorGetEndOffset(VmCatchIterator *pIterator,
                               const TryCatchHandler *pHandler);

/* Helper for vmFindCatchHandler(). Do not call directly. */
int vmFindCatchHandlerOffset0(u2 triesSize, const TryItem *pTries,
                              u4 address);

/* Find the handler associated with a given address, if any.
 * Initializes the given iterator and returns true if a match is
 * found. Returns false if there is no applicable handler. */
DEX_INLINE bool vmFindCatchHandler(VmCatchIterator *pIterator,
                                   TryCatchHandler *pHandler, u4 address) {
    u2 triesSize = pHandler->triesSize;
    int offset = -1;
    TryItem *tries = pHandler->tryItems;

    // Short-circuit the overwhelmingly common cases.
    switch (triesSize) {
        case 0: {
            break;
        }
        case 1: {
            u4 start = tries[0].startAddr;

            if (address < start) {
                break;
            }

            u4 end = start + tries[0].insnCount;

            if (address >= end) {
                break;
            }

            offset = tries[0].handlerOff;
            break;
        }
        default: {
            offset = vmFindCatchHandlerOffset0(triesSize, tries,
                                               address);
        }
    }

    if (offset < 0) {
        vmCatchIteratorClear(pIterator); // This squelches warnings.
        return false;
    } else {
        vmCatchIteratorInit(pIterator, pHandler, offset);
        return true;
    }
}

#endif  // VM_DEXCATCH_H_
