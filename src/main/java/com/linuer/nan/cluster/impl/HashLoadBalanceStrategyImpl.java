package com.linuer.nan.cluster.impl;

import com.linuer.nan.cluster.LoadBalanceStrategy;
import com.linuer.nan.helper.IPHelper;
import com.linuer.nan.model.ProviderService;

import java.util.List;

/**
 * 描述:
 * 软负载哈希算法实现
 * @author linuer
 * @create 2017-11-05 10:46
 */
public class HashLoadBalanceStrategyImpl implements LoadBalanceStrategy {

    @Override
    public ProviderService select(List<ProviderService> providerServices) {
        //获取调用方ip
        String localIP = IPHelper.localIp();
        //获取源地址对应的hashcode
        int hashCode = localIP.hashCode();
        //获取服务列表大小
        int size = providerServices.size();

        return providerServices.get(hashCode % size);
    }

}
