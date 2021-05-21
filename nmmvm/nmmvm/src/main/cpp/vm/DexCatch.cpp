
/*
 * Functions for dealing with try-catch info.
 */

#include "DexCatch.h"



/* Helper for dexFindCatchHandlerOffset(), which does an actual search
 * in the tries table. Returns -1 if there is no applicable handler. */
int vmFindCatchHandlerOffset0(u2 triesSize, const TryItem *pTries,
                              u4 address) {
    // Note: Signed type is important for max and min.
    int min = 0;
    int max = triesSize - 1;

    while (max >= min) {
        int guess = (min + max) >> 1;
        const TryItem *pTry = &pTries[guess];
        u4 start = pTry->startAddr;

        if (address < start) {
            max = guess - 1;
            continue;
        }

        u4 end = start + pTry->insnCount;

        if (address >= end) {
            min = guess + 1;
            continue;
        }

        // We have a winner!
        return (int) pTry->handlerOff;
    }

    // No match.
    return -1;
}

/* Get the handler offset just past the end of the one just iterated over.
 * This ends the iteration if it wasn't already. */
u4 vmCatchIteratorGetEndOffset(VmCatchIterator *pIterator,
                               const TryCatchHandler *pHandler
) {
    while (vmCatchIteratorNext(pIterator) != NULL) /* empty */ ;

    return (u4) (pIterator->pEncodedData -
            vmGetCatchHandlerData(pHandler));
}
