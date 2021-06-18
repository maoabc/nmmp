package com.nmmedit.apkprotect.data.config;

import com.google.gson.annotations.SerializedName;

public class Config {
    @SerializedName("abi")
    public AbiConfig abi;
    @SerializedName("path")
    public PathConfig path;
    @SerializedName("ndk")
    public NdkConfig ndk;

    public Config(AbiConfig abi) {
        this.abi = abi;
    }
}