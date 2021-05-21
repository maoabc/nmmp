/*
 * Copyright 2016 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package apk.arsc;

import java.nio.ByteBuffer;
import java.util.Locale;

import javax.annotation.Nullable;

/**
 * The common superclass for the various types of XML nodes.
 */
public abstract class XmlNodeChunk extends Chunk {

    private XmlChunk xmlChunk;

    /**
     * The line number in the original source at which this node appeared.
     */
    private final int lineNumber;

    /**
     * A string reference of this node's comment. If this is -1, then there is no comment.
     */
    private final int comment;

    protected XmlNodeChunk(ByteBuffer buffer, @Nullable Chunk parent) {
        super(buffer, parent);
        lineNumber = buffer.getInt();
        comment = buffer.getInt();
    }

    /**
     * Returns true if this XML node contains a comment. Else, returns false.
     */
    public boolean hasComment() {
        return comment != -1;
    }

    /**
     * Returns the line number in the original source at which this node appeared.
     */
    public int getLineNumber() {
        return lineNumber;
    }

    /**
     * Returns the comment associated with this node, if any. Else, returns the empty string.
     */
    public String getComment() {
        return getString(comment);
    }

    /**
     * An {@link XmlNodeChunk} does not know by itself what strings its indices reference. In order
     * to get the actual string, the first {@link XmlChunk} ancestor is found. The
     * {@link XmlChunk} ancestor should have a string pool which {@code index} references.
     *
     * @param index The index of the string.
     * @return String that the given {@code index} references, or empty string if {@code index} is -1.
     */
    protected String getString(int index) {
        if (index == -1) {  // Special case. Packed XML files use -1 for "no string entry"
            return "";
        }
        if (xmlChunk == null) {
            Chunk parent = getParent();
            while (parent != null) {
                if (parent instanceof XmlChunk) {
                    xmlChunk = (XmlChunk) parent;
                }
                parent = parent.getParent();
            }
        }
        if (xmlChunk != null) {
            return xmlChunk.getString(index);
        }
        throw new IllegalStateException("XmlNodeChunk did not have an XmlChunk parent.");
    }

    /**
     * An {@link XmlNodeChunk} and anything that is itself an {@link XmlNodeChunk} has a header size
     * of 16. Anything else is, interestingly, considered to be a payload. For that reason, this
     * method is final.
     */
    @Override
    protected final void writeHeader(ByteBuffer output) {
        super.writeHeader(output);
        output.putInt(lineNumber);
        output.putInt(comment);
    }

    /**
     * Returns a brief description of this XML node. The representation of this information is
     * subject to change, but below is a typical example:
     *
     * <pre>"XmlNodeChunk{line=1234, comment=My awesome comment.}"</pre>
     */
    @Override
    public String toString() {
        return String.format(
                Locale.US, "XmlNodeChunk{line=%d, comment=%s}", getLineNumber(), getComment());
    }
}
