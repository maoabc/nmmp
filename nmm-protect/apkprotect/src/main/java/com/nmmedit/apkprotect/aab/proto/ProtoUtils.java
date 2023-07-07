package com.nmmedit.apkprotect.aab.proto;

import com.android.aapt.Resources;
import com.android.bundle.Config;
import com.android.bundle.Files;
import com.android.bundle.Targeting;

import javax.annotation.Nonnull;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

public class ProtoUtils {

    /**
     * BundleConfig.pb文件
     */
    public static class BundleConfig {
        /**
         * 修改BundleConfig.pb一些属性
         *
         * @param configBytes
         * @return
         * @throws IOException
         */
        @Nonnull
        public static byte[] editConfig(byte[] configBytes) throws IOException {
            final Config.BundleConfig.Builder configBuilder = Config.BundleConfig.parseFrom(configBytes).toBuilder();
            final Config.Optimizations.Builder optimizationsBuilder = configBuilder.getOptimizationsBuilder();

            //未压缩本地库设置
            //设置为true则可以在生成对应abi的apk不压缩，从而减小安装后体积
            final Config.UncompressNativeLibraries.Builder uncompressNativeLibBuilder = Config.UncompressNativeLibraries.newBuilder();
            uncompressNativeLibBuilder.setEnabled(false);
            optimizationsBuilder.setUncompressNativeLibraries(uncompressNativeLibBuilder);

            final ByteArrayOutputStream bout = new ByteArrayOutputStream(configBytes.length);
            configBuilder.build().writeTo(bout);
            return bout.toByteArray();
        }
    }

    /**
     * .aab内的AndroidManifest.xml是protobuf生成的二进制文件
     */
    public static class AndroidManifest {

        @Nonnull
        public static String getPackageName(@Nonnull byte[] manifestBytes) throws IOException {
            final Resources.XmlNode xmlNode = Resources.XmlNode.parseFrom(manifestBytes);
            final Resources.XmlElement element = xmlNode.getElement();
            if (!"manifest".equals(element.getName())) {
                throw new IOException("Not is manifest");
            }
            final int count = element.getAttributeCount();
            for (int i = 0; i < count; i++) {
                final Resources.XmlAttribute attr = element.getAttribute(i);
                if ("package".equals(attr.getName())) {
                    return attr.getValue();
                }
            }
            throw new IOException("No package attr");
        }

        /**
         * 修改原Manifest里一些属性
         *
         * @param manifestBytes
         * @return
         * @throws IOException
         */
        @Nonnull
        public static byte[] editAndroidManifest(@Nonnull byte[] manifestBytes) throws IOException {
            final Resources.XmlNode.Builder rootNodeBuilder = Resources.XmlNode.parseFrom(manifestBytes).toBuilder();

            final Resources.XmlElement.Builder elementBuilder = rootNodeBuilder.getElementBuilder();
            if (!"manifest".equals(elementBuilder.getName())) {
                throw new IOException("Not is manifest");
            }

            final int count = elementBuilder.getChildCount();
            for (int i = 0; i < count; i++) {
                final Resources.XmlNode.Builder childBuilder = elementBuilder.getChildBuilder(i);
                final Resources.XmlElement.Builder childElementBuilder;
                if (childBuilder.hasElement() && "application".equals((childElementBuilder = childBuilder.getElementBuilder()).getName())) {
                    final int attrCount = childElementBuilder.getAttributeCount();
                    //查找extractNativeLibs 属性,如果找到则删除它
                    //todo 实际测试后发现这个属性无用会被覆盖，需要修改BundleConfig.pb
                    int attrIdx = -1;
                    for (int j = 0; j < attrCount; j++) {
                        final Resources.XmlAttribute.Builder attribute = childElementBuilder.getAttributeBuilder(j);
                        if ("extractNativeLibs".equals(attribute.getName())) {
                            attrIdx = j;
                        }
                    }
                    if (attrIdx != -1) {
                        childElementBuilder.removeAttribute(attrIdx);
                    }
                }
            }
            final ByteArrayOutputStream bout = new ByteArrayOutputStream(manifestBytes.length);
            rootNodeBuilder.build().writeTo(bout);
            return bout.toByteArray();
        }
    }

    /**
     * base/native.pb文件生成
     */
    public static class NativeLibraries {

        private static Files.NativeLibraries genNativeLibsProtoBuf(@Nonnull List<String> abis) {

            final Files.NativeLibraries.Builder nativeLibsBuilder = Files.NativeLibraries.newBuilder();

            for (String abi : abis) {
                final Targeting.NativeDirectoryTargeting.Builder targetingBuilder = Targeting.NativeDirectoryTargeting.newBuilder();
                final Files.TargetedNativeDirectory.Builder dirBuilder = Files.TargetedNativeDirectory.newBuilder();
                if ("armeabi-v7a".equals(abi)) {
                    targetingBuilder.getAbiBuilder().setAlias(Targeting.Abi.AbiAlias.ARMEABI_V7A);
                } else if ("arm64-v8a".equals(abi)) {
                    targetingBuilder.getAbiBuilder().setAlias(Targeting.Abi.AbiAlias.ARM64_V8A);
                } else if ("x86".equals(abi)) {
                    targetingBuilder.getAbiBuilder().setAlias(Targeting.Abi.AbiAlias.X86);
                } else if ("x86_64".equals(abi)) {
                    targetingBuilder.getAbiBuilder().setAlias(Targeting.Abi.AbiAlias.X86_64);
                } else {
                    throw new RuntimeException("Unknown abi " + abi);
                }
                dirBuilder.setPath("lib/" + abi);
                dirBuilder.setTargeting(targetingBuilder);
                nativeLibsBuilder.addDirectory(dirBuilder);
            }
            return nativeLibsBuilder.build();
        }

        public static void writeNativePB(@Nonnull List<String> abis, @Nonnull OutputStream out) throws IOException {
            final Files.NativeLibraries nativeLibraries = genNativeLibsProtoBuf(abis);
            nativeLibraries.writeTo(out);
        }
    }
}
