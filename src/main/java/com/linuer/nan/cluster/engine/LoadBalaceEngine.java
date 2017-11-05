package com.linuer.nan.cluster.engine;

import com.google.common.collect.Maps;
import com.linuer.nan.cluster.LoadBalanceStrategy;
import com.linuer.nan.cluster.impl.*;

import java.util.Map;

/**
 * 描述:
 * 负载均衡引擎
 * @author linuer
 * @create 2017-11-05 10:53
 */
public class LoadBalaceEngine {
    private static final Map<LoadBalanceEnum, LoadBalanceStrategy> clusterStrategyMap = Maps.newConcurrentMap();

    static {
        clusterStrategyMap.put(LoadBalanceEnum.Random, new RandomLoadBalanceStrategyImpl());
        clusterStrategyMap.put(LoadBalanceEnum.WeightRandom, new WeightRandomLoadBalanceStrategyImpl());
        clusterStrategyMap.put(LoadBalanceEnum.Polling, new PollingLoadBalanceStrategyImpl());
        clusterStrategyMap.put(LoadBalanceEnum.WeightPolling, new WeightPollingLoadBalanceStrategyImpl());
        clusterStrategyMap.put(LoadBalanceEnum.Hash, new HashLoadBalanceStrategyImpl());
    }


    public static LoadBalanceStrategy queryClusterStrategy(String clusterStrategy) {
        LoadBalanceEnum clusterStrategyEnum = LoadBalanceEnum.queryByCode(clusterStrategy);
        if (clusterStrategyEnum == null) {
            //默认选择随机算法
            return new RandomLoadBalanceStrategyImpl();
        }

        return clusterStrategyMap.get(clusterStrategyEnum);
    }
}
