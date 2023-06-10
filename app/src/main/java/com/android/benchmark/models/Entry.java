package com.android.benchmark.models;

import java.util.List;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import androidx.annotation.Keep;

@Keep
public class Entry {

    @SerializedName("run_id")
    @Expose
    private Integer runId;
    @SerializedName("benchmark_version")
    @Expose
    private String benchmarkVersion;
    @SerializedName("device_name")
    @Expose
    private String device_name;
    @SerializedName("device_model")
    @Expose
    private String deviceModel;
    @SerializedName("device_product")
    @Expose
    private String deviceProduct;
    @SerializedName("device_board")
    @Expose
    private String deviceBoard;
    @SerializedName("device_manufacturer")
    @Expose
    private String deviceManufacturer;
    @SerializedName("device_brand")
    @Expose
    private String deviceBrand;
    @SerializedName("device_hardware")
    @Expose
    private String deviceHardware;
    @SerializedName("android_version")
    @Expose
    private String androidVersion;
    @SerializedName("build_type")
    @Expose
    private String buildType;
    @SerializedName("build_time")
    @Expose
    private String buildTime;
    @SerializedName("fingerprint")
    @Expose
    private String fingerprint;
    @SerializedName("kernel_version")
    @Expose
    private String kernelVersion;
    @SerializedName("results")
    @Expose
    private List<Result> results;
    @SerializedName("refresh_rate")
    @Expose
    private Integer refreshRate;

    public Integer getRunId() {
        return this.runId;
    }

    public void setRunId(final Integer runId) {
        this.runId = runId;
    }

    public String getBenchmarkVersion() {
        return this.benchmarkVersion;
    }

    public void setBenchmarkVersion(final String benchmarkVersion) {
        this.benchmarkVersion = benchmarkVersion;
    }

    public String getDeviceName() {
        return this.device_name;
    }

    public void setDeviceName(final String device_name) {
        this.device_name = device_name;
    }

    public String getDeviceModel() {
        return this.deviceModel;
    }

    public void setDeviceModel(final String deviceModel) {
        this.deviceModel = deviceModel;
    }

    public String getDeviceProduct() {
        return this.deviceProduct;
    }

    public void setDeviceProduct(final String deviceProduct) {
        this.deviceProduct = deviceProduct;
    }

    public String getDeviceBoard() {
        return this.deviceBoard;
    }

    public void setDeviceBoard(final String deviceBoard) {
        this.deviceBoard = deviceBoard;
    }

    public String getDeviceManufacturer() {
        return this.deviceManufacturer;
    }

    public void setDeviceManufacturer(final String deviceManufacturer) {
        this.deviceManufacturer = deviceManufacturer;
    }

    public String getDeviceBrand() {
        return this.deviceBrand;
    }

    public void setDeviceBrand(final String deviceBrand) {
        this.deviceBrand = deviceBrand;
    }

    public String getDeviceHardware() {
        return this.deviceHardware;
    }

    public void setDeviceHardware(final String deviceHardware) {
        this.deviceHardware = deviceHardware;
    }

    public String getAndroidVersion() {
        return this.androidVersion;
    }

    public void setAndroidVersion(final String androidVersion) {
        this.androidVersion = androidVersion;
    }

    public String getBuildType() {
        return this.buildType;
    }

    public void setBuildType(final String buildType) {
        this.buildType = buildType;
    }

    public String getBuildTime() {
        return this.buildTime;
    }

    public void setBuildTime(final String buildTime) {
        this.buildTime = buildTime;
    }

    public String getFingerprint() {
        return this.fingerprint;
    }

    public void setFingerprint(final String fingerprint) {
        this.fingerprint = fingerprint;
    }

    public String getKernelVersion() {
        return this.kernelVersion;
    }

    public void setKernelVersion(final String kernelVersion) {
        this.kernelVersion = kernelVersion;
    }

    public List<Result> getResults() {
        return this.results;
    }

    public void setResults(final List<Result> results) {
        this.results = results;
    }

    public int getRefreshRate() {
        return this.refreshRate;
    }

    public void setRefreshRate(final int refreshRate) {
        this.refreshRate = refreshRate;
    }
}