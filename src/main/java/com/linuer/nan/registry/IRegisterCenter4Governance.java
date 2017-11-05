package com.linuer.nan.registry;

import com.linuer.nan.model.InvokerService;
import com.linuer.nan.model.ProviderService;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;

/**
 * 描述:
 *服务治理接口
 * @author linuer
 * @create 2017-11-04 19:52
 */
public interface IRegisterCenter4Governance {

    /**
     * 获取服务提供者列表与服务消费者列表
     *
     * @param serviceName
     * @param appKey
     * @return
     */
    public Pair<List<ProviderService>, List<InvokerService>> queryProvidersAndInvokers(String serviceName, String appKey);

}
