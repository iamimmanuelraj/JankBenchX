package com.android.benchmark.models;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import androidx.annotation.Keep;

@Keep
public class Result {

    @SerializedName("test_name")
    @Expose
    private String testName;
    @SerializedName("score")
    @Expose
    private Integer score;
    @SerializedName("jank_penalty")
    @Expose
    private Integer jankPenalty;
    @SerializedName("consistency_bonus")
    @Expose
    private Integer consistencyBonus;
    @SerializedName("jank_pct")
    @Expose
    private Double jankPct;
    @SerializedName("bad_frame_pct")
    @Expose
    private Double badFramePct;
    @SerializedName("total_frames")
    @Expose
    private Integer totalFrames;
    @SerializedName("ms_avg")
    @Expose
    private Double msAvg;
    @SerializedName("ms_10th_pctl")
    @Expose
    private Double ms10thPctl;
    @SerializedName("ms_20th_pctl")
    @Expose
    private Double ms20thPctl;
    @SerializedName("ms_30th_pctl")
    @Expose
    private Double ms30thPctl;
    @SerializedName("ms_40th_pctl")
    @Expose
    private Double ms40thPctl;
    @SerializedName("ms_50th_pctl")
    @Expose
    private Double ms50thPctl;
    @SerializedName("ms_60th_pctl")
    @Expose
    private Double ms60thPctl;
    @SerializedName("ms_70th_pctl")
    @Expose
    private Double ms70thPctl;
    @SerializedName("ms_80th_pctl")
    @Expose
    private Double ms80thPctl;
    @SerializedName("ms_90th_pctl")
    @Expose
    private Double ms90thPctl;
    @SerializedName("ms_95th_pctl")
    @Expose
    private Double ms95thPctl;
    @SerializedName("ms_99th_pctl")
    @Expose
    private Double ms99thPctl;

    public String getTestName() {
        return this.testName;
    }

    public void setTestName(final String testName) {
        this.testName = testName;
    }

    public Integer getScore() {
        return this.score;
    }

    public void setScore(final Integer score) {
        this.score = score;
    }

    public Integer getJankPenalty() {
        return this.jankPenalty;
    }

    public void setJankPenalty(final Integer jankPenalty) {
        this.jankPenalty = jankPenalty;
    }

    public Integer getConsistencyBonus() {
        return this.consistencyBonus;
    }

    public void setConsistencyBonus(final Integer consistencyBonus) {
        this.consistencyBonus = consistencyBonus;
    }

    public Double getJankPct() {
        return this.jankPct;
    }

    public void setJankPct(final Double jankPct) {
        this.jankPct = jankPct;
    }

    public Double getBadFramePct() {
        return this.badFramePct;
    }

    public void setBadFramePct(final Double badFramePct) {
        this.badFramePct = badFramePct;
    }

    public Integer getTotalFrames() {
        return this.totalFrames;
    }

    public void setTotalFrames(final Integer totalFrames) {
        this.totalFrames = totalFrames;
    }

    public Double getMsAvg() {
        return this.msAvg;
    }

    public void setMsAvg(final Double msAvg) {
        this.msAvg = msAvg;
    }

    public Double getMs10thPctl() {
        return this.ms10thPctl;
    }

    public void setMs10thPctl(final Double ms10thPctl) {
        this.ms10thPctl = ms10thPctl;
    }

    public Double getMs20thPctl() {
        return this.ms20thPctl;
    }

    public void setMs20thPctl(final Double ms20thPctl) {
        this.ms20thPctl = ms20thPctl;
    }

    public Double getMs30thPctl() {
        return this.ms30thPctl;
    }

    public void setMs30thPctl(final Double ms30thPctl) {
        this.ms30thPctl = ms30thPctl;
    }

    public Double getMs40thPctl() {
        return this.ms40thPctl;
    }

    public void setMs40thPctl(final Double ms40thPctl) {
        this.ms40thPctl = ms40thPctl;
    }

    public Double getMs50thPctl() {
        return this.ms50thPctl;
    }

    public void setMs50thPctl(final Double ms50thPctl) {
        this.ms50thPctl = ms50thPctl;
    }

    public Double getMs60thPctl() {
        return this.ms60thPctl;
    }

    public void setMs60thPctl(final Double ms60thPctl) {
        this.ms60thPctl = ms60thPctl;
    }

    public Double getMs70thPctl() {
        return this.ms70thPctl;
    }

    public void setMs70thPctl(final Double ms70thPctl) {
        this.ms70thPctl = ms70thPctl;
    }

    public Double getMs80thPctl() {
        return this.ms80thPctl;
    }

    public void setMs80thPctl(final Double ms80thPctl) {
        this.ms80thPctl = ms80thPctl;
    }

    public Double getMs90thPctl() {
        return this.ms90thPctl;
    }

    public void setMs90thPctl(final Double ms90thPctl) {
        this.ms90thPctl = ms90thPctl;
    }

    public Double getMs95thPctl() {
        return this.ms95thPctl;
    }

    public void setMs95thPctl(final Double ms95thPctl) {
        this.ms95thPctl = ms95thPctl;
    }

    public Double getMs99thPctl() {
        return this.ms99thPctl;
    }

    public void setMs99thPctl(final Double ms99thPctl) {
        this.ms99thPctl = ms99thPctl;
    }

}