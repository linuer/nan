package com.linuer.nan.cluster.impl;

import org.apache.commons.lang3.StringUtils;

/**
 * 描述:
 *
 * @author linuer
 * @create 2017-11-05 10:46
 */
public enum LoadBalanceEnum {

    //随机算法
    Random("Random"),
    //权重随机算法
    WeightRandom("WeightRandom"),
    //轮询算法
    Polling("Polling"),
    //权重轮询算法
    WeightPolling("WeightPolling"),
    //源地址hash算法
    Hash("Hash");

    private LoadBalanceEnum(String code) {
        this.code = code;
    }


    public static LoadBalanceEnum queryByCode(String code) {
        if (StringUtils.isBlank(code)) {
            return null;
        }
        for (LoadBalanceEnum strategy : values()) {
            if (StringUtils.equals(code, strategy.getCode())) {
                return strategy;
            }
        }
        return null;
    }

    private String code;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}
