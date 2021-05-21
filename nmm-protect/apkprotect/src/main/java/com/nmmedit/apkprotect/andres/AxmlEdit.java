package com.nmmedit.apkprotect.andres;

import apk.arsc.*;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.List;

public class AxmlEdit {

    @Nonnull
    public static String getApplicationName(@Nonnull byte[] manifestBytes) {
        ResourceFile file = new ResourceFile(manifestBytes);
        for (Chunk chunk : file.getChunks()) {
            if (chunk instanceof XmlChunk) {
                XmlChunk xmlChunk = (XmlChunk) chunk;
                for (Chunk subChunk : xmlChunk.getChunks().values()) {
                    if (subChunk instanceof XmlStartElementChunk) {
                        XmlStartElementChunk startElementChunk = (XmlStartElementChunk) subChunk;
                        if (startElementChunk.getName().equals("application")) {
                            return getApplicationName(startElementChunk);
                        }
                    }
                }
            }
        }
        return "";
    }
    @Nonnull
    public static String getPackageName(@Nonnull byte[] manifestBytes) {
        ResourceFile file = new ResourceFile(manifestBytes);
        for (Chunk chunk : file.getChunks()) {
            if (chunk instanceof XmlChunk) {
                XmlChunk xmlChunk = (XmlChunk) chunk;
                for (Chunk subChunk : xmlChunk.getChunks().values()) {
                    if (subChunk instanceof XmlStartElementChunk) {
                        XmlStartElementChunk startElementChunk = (XmlStartElementChunk) subChunk;
                        if (startElementChunk.getName().equals("manifest")) {
                            for (XmlAttribute attribute : startElementChunk.getAttributes()) {
                                if(attribute.name().equals("package")){
                                    return attribute.rawValue();
                                }
                            }

                        }
                    }
                }
            }
        }
        return "";
    }

    /**
     * 修改manifest.xml同时返回修改后的字节数组
     *
     * @param manifestBytes
     * @param newName
     * @return
     * @throws IOException
     */
    public static byte[] renameApplicationName(@Nonnull byte[] manifestBytes, @Nonnull String newName) throws IOException {
        ResourceFile file = new ResourceFile(manifestBytes);

        boolean modified = false;
        for (Chunk chunk : file.getChunks()) {
            if (chunk instanceof XmlChunk) {
                XmlChunk xmlChunk = (XmlChunk) chunk;
                StringPoolChunk stringPoolChunk = getStringPoolChunk(xmlChunk);
                if (stringPoolChunk == null) {
                    return null;
                }
                for (Chunk subChunk : xmlChunk.getChunks().values()) {
                    if (subChunk instanceof XmlStartElementChunk) {
                        XmlStartElementChunk startElementChunk = (XmlStartElementChunk) subChunk;
                        if (modifyApplicationName(stringPoolChunk, startElementChunk, newName)) {
                            modified = true;
                            break;
                        }
                    }
                }
            }
        }
        if (modified) {
            return file.toByteArray();
        }
        return null;
    }


    //得到application 对应的class name
    @Nonnull
    private static String getApplicationName(@Nonnull XmlStartElementChunk startElement) {
        List<XmlAttribute> attributes = startElement.getAttributes();
        for (XmlAttribute attribute : attributes) {
            ResourceValue typedValue = attribute.typedValue();
            if (attribute.name().equals("name") &&
                    typedValue.type() == ResourceValue.Type.STRING) {
                return attribute.rawValue();
            }
        }
        return "";
    }

    private static boolean modifyApplicationName(@Nonnull StringPoolChunk stringPoolChunk,
                                                 @Nonnull XmlStartElementChunk startElement,
                                                 @Nonnull String newAppName) {
        if (startElement.getName().equals("application")) {
            List<XmlAttribute> attributes = startElement.getAttributes();
            for (int i = 0; i < attributes.size(); i++) {
                XmlAttribute attribute = attributes.get(i);
                ResourceValue typedValue = attribute.typedValue();
                if (attribute.name().equals("name") &&
                        typedValue.type() == ResourceValue.Type.STRING) {
                    int strIdx = stringPoolChunk.addString(newAppName);
                    ResourceValue newValue = ResourceValue.builder()
                            .data(strIdx)
                            .size(typedValue.size())
                            .type(typedValue.type()).build();
                    XmlAttribute newAttr = XmlAttribute.create(
                            attribute.namespaceIndex(),
                            attribute.nameIndex(),
                            strIdx,
                            newValue,
                            attribute.parent()
                    );

//                    System.out.println("attr " + typedValue + "   " + attribute.rawValue()
//                            + "  " + typedValue.data());
//                    System.out.println(typedValue.type());
//                    System.out.println("new Attr " + newAttr + "  " + newAttr.rawValue());

                    startElement.setAttribute(i, newAttr);
                    return true;
                }
            }
        }
        return false;
    }

    private static StringPoolChunk getStringPoolChunk(XmlChunk xmlChunk) {
        for (Chunk chunk : xmlChunk.getChunks().values()) {
            if (chunk instanceof StringPoolChunk) {
                return (StringPoolChunk) chunk;
            }
        }
        return null;
    }
}
