package com.nmmedit.apkprotect.data.config;

import com.google.gson.annotations.SerializedName;

public class PathConfig {
    @SerializedName("sdk_path")
    public String sdk_path;
    @SerializedName("cmake_path")
    public String cmake_path;
    @SerializedName("ndk_path")
    public String ndk_path;

    @SerializedName("ndk_toolchains")
    public String ndk_toolchains;
    @SerializedName("ndk_abi")
    public String ndk_abi;
    @SerializedName("ndk_strip")
    public String ndk_strip;
}
