package com.linuer.nan.registry;

import com.linuer.nan.model.InvokerService;
import com.linuer.nan.model.ProviderService;

import java.util.List;
import java.util.Map;

/**
 * 描述:
 *  消费端注册中心接口
 * @author linuer
 * @create 2017-11-04 20:04
 */
public interface IRegisterCenter4Invoker {

    /**
     * 消费端初始化服务提供者信息本地缓存
     *
     * @param remoteAppKey
     * @param groupName
     */
    public void initProviderMap(String remoteAppKey, String groupName);


    /**
     * 消费端获取服务提供者信息
     *
     * @return
     */
    public Map<String, List<ProviderService>> getServiceMetaDataMap4Consume();


    /**
     * 消费端将消费者信息注册到注册中心对应的节点下
     *
     * @param invoker
     */
    public void registerInvoker(final InvokerService invoker);
}
