package com.linuer.nan.cluster.impl;

import com.google.common.collect.Lists;
import com.linuer.nan.cluster.LoadBalanceStrategy;
import com.linuer.nan.model.ProviderService;
import org.apache.commons.lang3.RandomUtils;

import java.util.List;

/**
 * 描述:
 *
 * @author linuer
 * @create 2017-11-05 10:53
 */
public class WeightRandomLoadBalanceStrategyImpl implements LoadBalanceStrategy {
    @Override
    public ProviderService select(List<ProviderService> providerServices) {
        //存放加权后的服务提供者列表
        List<ProviderService> providerList = Lists.newArrayList();
        for (ProviderService provider : providerServices) {
            int weight = provider.getWeight();
            for (int i = 0; i < weight; i++) {
                providerList.add(provider.copy());
            }
        }

        int MAX_LEN = providerList.size();
        int index = RandomUtils.nextInt(0, MAX_LEN - 1);
        return providerList.get(index);
    }
}