package com.linuer.nan.cluster.impl;

import com.linuer.nan.cluster.LoadBalanceStrategy;
import com.linuer.nan.model.ProviderService;
import org.apache.commons.lang3.RandomUtils;

import java.util.List;

/**
 * 描述:
 * 软负载随机算法实现
 * @author linuer
 * @create 2017-11-05 10:50
 */
public class RandomLoadBalanceStrategyImpl implements LoadBalanceStrategy{
    @Override
    public ProviderService select(List<ProviderService> providerServices) {
        int MAX_LEN = providerServices.size();
        int index = RandomUtils.nextInt(0, MAX_LEN - 1);
        return providerServices.get(index);
    }
}
