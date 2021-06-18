package com.nmmedit.apkprotect.data.config;

import com.google.gson.annotations.SerializedName;

public class PathConfig {
    @SerializedName("sdk")
    public String sdk;
    @SerializedName("cmake")
    public String cmake;
    @SerializedName("ndk")
    public String ndk;
}
