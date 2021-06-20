package com.nmmedit.apkprotect.deobfus;

public interface MappingProcessor {
    /**
     * Processes the given class name mapping.
     *
     * @param className    the original class name.
     * @param newClassName the new class name.
     * @return whether the processor is interested in receiving mappings of the
     * class members of this class.
     */
    void processClassMapping(String className,
                             String newClassName);

    /**
     * Processes the given field name mapping.
     *
     * @param className    the original class name.
     * @param fieldType    the original external field type.
     * @param fieldName    the original field name.
     * @param newClassName the new class name.
     * @param newFieldName the new field name.
     */
    void processFieldMapping(String className,
                             String fieldType,
                             String fieldName,
                             String newClassName,
                             String newFieldName);

    /**
     * Processes the given method name mapping.
     *
     * @param className          the original class name.
     * @param firstLineNumber    the first line number of the method, or 0 if
     *                           it is not known.
     * @param lastLineNumber     the last line number of the method, or 0 if
     *                           it is not known.
     * @param methodReturnType   the original external method return type.
     * @param methodName         the original external method name.
     * @param methodArguments    the original external method arguments.
     * @param newClassName       the new class name.
     * @param newFirstLineNumber the new first line number of the method, or 0
     *                           if it is not known.
     * @param newLastLineNumber  the new last line number of the method, or 0
     *                           if it is not known.
     * @param newMethodName      the new method name.
     */
    void processMethodMapping(String className,
                              int firstLineNumber,
                              int lastLineNumber,
                              String methodReturnType,
                              String methodName,
                              String methodArguments,
                              String newClassName,
                              int newFirstLineNumber,
                              int newLastLineNumber,
                              String newMethodName);
}
