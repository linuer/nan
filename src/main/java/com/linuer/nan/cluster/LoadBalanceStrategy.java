package com.linuer.nan.cluster;

import com.linuer.nan.model.ProviderService;

import java.util.List;

/**
 * 描述:
 *  负载均衡策略，消费端策略
 * @author linuer
 * @create 2017-11-05 10:45
 */
public interface LoadBalanceStrategy {
    /**
     * 负载策略算法
     *
     * @param providerServices
     * @return
     */
    public ProviderService select(List<ProviderService> providerServices);
}
