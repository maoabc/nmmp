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
    public static int  getMinSdk(@Nonnull byte[] manifestBytes) {
        ResourceFile file = new ResourceFile(manifestBytes);
        for (Chunk chunk : file.getChunks()) {
            if (chunk instanceof XmlChunk) {
                XmlChunk xmlChunk = (XmlChunk) chunk;
                for (Chunk subChunk : xmlChunk.getChunks().values()) {
                    if (subChunk instanceof XmlStartElementChunk) {
                        XmlStartElementChunk startElementChunk = (XmlStartElementChunk) subChunk;
                        if (startElementChunk.getName().equals("uses-sdk")) {
                            List<XmlAttribute> attributes = startElementChunk.getAttributes();
                            for (XmlAttribute attribute : attributes) {
                                ResourceValue typedValue = attribute.typedValue();
                                if (attribute.name().equals("minSdkVersion") &&
                                        typedValue.type() == ResourceValue.Type.INT_DEC) {
                                    return attribute.typedValue().data();
                                }
                            }
                        }
                    }
                }
            }
        }
        //最低按安卓5
        return 21;
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
                                if (attribute.name().equals("package")) {
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
                        //todo 可能还需要在<application>节点插入 android:extractNativeLibs="true" 不然本地库无法压缩放入apk
                        if (modifyApplicationName(stringPoolChunk, startElementChunk, newName)) {
                            modified = true;
                            break;
                        }
                    }
                }
            }
        }
        if (modified) {
            return file.toByteArray(SerializableResource.SHRINK);
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
            //如果之前application节点存在name属性则修改它
            for (int i = 0; i < attributes.size(); i++) {
                XmlAttribute attribute = attributes.get(i);
                ResourceValue typedValue = attribute.typedValue();
                if (attribute.name().equals("name") &&
                        typedValue.type() == ResourceValue.Type.STRING) {
                    XmlAttribute newAttr = createXmlNameAttribute(stringPoolChunk, startElement, newAppName);

                    startElement.setAttribute(i, newAttr);
                    return true;
                }
            }
            //application节点不存在name属性，创建后添加
            XmlAttribute newAttr = createXmlNameAttribute(stringPoolChunk, startElement, newAppName);
            // 属性有顺序，通用插入属性处理应该要根据 com.android.internal.R 里的定义对属性进行排序，
            // 这里只是插入name属性，所以简单的查找索引位置然后插入
            // android:theme
            // public static final int AndroidManifestApplication_theme=0;
            // android:label
            // public static final int AndroidManifestApplication_label=1;
            // android:icon
            // public static final int AndroidManifestApplication_icon=2;
            // android:name
            // public static final int AndroidManifestApplication_name=3;

            int nameAttrIndex = 0;
            for (XmlAttribute attribute : attributes) {
                //name属性在这些属性之后
                if ("theme".equals(attribute.name())
                        || "label".equals(attribute.name())
                        || "icon".equals(attribute.name())
                ) {
                    nameAttrIndex++;
                }
            }

            startElement.addAttribute(nameAttrIndex, newAttr);

            return true;

        }
        return false;
    }

    private static int getOrAddString(StringPoolChunk stringPool, String str) {
        final int index = stringPool.indexOf(str);
        if (index != -1) {
            return index;
        }
        return stringPool.addString(str);
    }

    //创建xml属性，类似android:name="strvalue"
    @Nonnull
    private static XmlAttribute createXmlNameAttribute(@Nonnull StringPoolChunk stringPoolChunk,
                                                       @Nonnull XmlStartElementChunk startElementChunk,
                                                       @Nonnull String str) {
        int strIdx = getOrAddString(stringPoolChunk, str);
        ResourceValue resourceValue = ResourceValue.builder()
                .data(strIdx)
                .size(ResourceValue.SIZE)
                .type(ResourceValue.Type.STRING).build();

        String attrNameSpace = "";
        for (XmlAttribute attribute : startElementChunk.getAttributes()) {
            attrNameSpace = attribute.namespace();
            if (!"".equals(attrNameSpace)) {
                break;
            }
        }

        if ("".equals(attrNameSpace)) {
            throw new RuntimeException("Modify application name falied");
        }


        final int nameSpaceIdx = getOrAddString(stringPoolChunk, attrNameSpace);
        final int name = getOrAddString(stringPoolChunk, "name");
        return XmlAttribute.create(
                nameSpaceIdx,
                name,
                strIdx,
                resourceValue,
                startElementChunk
        );
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
