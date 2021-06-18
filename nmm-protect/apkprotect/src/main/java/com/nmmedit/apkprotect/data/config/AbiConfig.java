package com.nmmedit.apkprotect.data.config;

import com.google.gson.annotations.SerializedName;

public class AbiConfig {
    @SerializedName("armeabi-v7a")
    public boolean arm;
    @SerializedName("arm64-v8a")
    public boolean arm64;
    @SerializedName("x86")
    public boolean x86;
    @SerializedName("x86_64")
    public boolean x64;
}
