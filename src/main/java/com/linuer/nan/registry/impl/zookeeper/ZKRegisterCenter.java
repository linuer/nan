package com.linuer.nan.registry.impl.zookeeper;

import com.alibaba.fastjson.JSON;
import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.linuer.nan.model.InvokerService;
import com.linuer.nan.model.ProviderService;
import com.linuer.nan.helper.IPHelper;
import com.linuer.nan.helper.PropertyConfigeHelper;
import com.linuer.nan.registry.IRegisterCenter4Governance;
import com.linuer.nan.registry.IRegisterCenter4Invoker;
import com.linuer.nan.registry.IRegisterCenter4Provider;
import org.I0Itec.zkclient.IZkChildListener;
import org.I0Itec.zkclient.ZkClient;
import org.I0Itec.zkclient.serialize.SerializableSerializer;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;
import java.util.Map;

/**
 * 描述:
 * Z    K注册中心实现
 * @author linuer
 * @create 2017-11-04 20:09
 */
public class ZKRegisterCenter implements IRegisterCenter4Invoker, IRegisterCenter4Provider, IRegisterCenter4Governance {

    private static ZKRegisterCenter registerCenter = new ZKRegisterCenter();

    //服务提供者列表,Key:服务提供者接口  value:服务提供者服务方法列表,  hello : sayhello,saygoodbye
    private static final Map<String, List<ProviderService>> providerServiceMap = Maps.newConcurrentMap();
    //服务端ZK服务元信息,选择服务(第一次直接从ZK拉取,后续由ZK的监听机制主动更新)
    private static final Map<String, List<ProviderService>> serviceMetaDataMap4Consume = com.google.common.collect.Maps.newConcurrentMap();

    private static String ZK_SERVICE = PropertyConfigeHelper.getZkService();
    private static int ZK_SESSION_TIME_OUT = PropertyConfigeHelper.getZkConnectionTimeout();
    private static int ZK_CONNECTION_TIME_OUT = PropertyConfigeHelper.getZkConnectionTimeout();
    private static String ROOT_PATH = "/nan_register";
    public static String PROVIDER_TYPE = "provider";
    public static String INVOKER_TYPE = "consumer";
    private static volatile ZkClient zkClient = null;


    private ZKRegisterCenter() {
    }

    public static ZKRegisterCenter singleton() {
        return registerCenter;
    }

    @Override  //    /config_register/nan/default/ares.remoting.test.HelloService/
    public void registerProvider(final List<ProviderService> serviceMetaData) {
        if (CollectionUtils.isEmpty(serviceMetaData)) {
            return;
        }

        //连接zk,注册服务 同步锁，防止重复注册
        synchronized (ZKRegisterCenter.class) {
            for (ProviderService provider : serviceMetaData) {
                String serviceItfKey = provider.getServiceItf().getName();

                List<ProviderService> providers = providerServiceMap.get(serviceItfKey);
                if (providers == null) {
                    providers = Lists.newArrayList();
                }
                providers.add(provider);
                providerServiceMap.put(serviceItfKey, providers);
            }

            if (zkClient == null) {
                zkClient = new ZkClient(ZK_SERVICE, ZK_SESSION_TIME_OUT, ZK_CONNECTION_TIME_OUT, new SerializableSerializer());
            }

            //创建 ZK命名空间/当前部署应用APP命名空间/
            String APP_KEY = serviceMetaData.get(0).getAppKey();
            String ZK_PATH = ROOT_PATH + "/" + APP_KEY;
            boolean exist = zkClient.exists(ZK_PATH);
            if (!exist) {
                zkClient.createPersistent(ZK_PATH, true);
            }

            for (Map.Entry<String, List<ProviderService>> entry : providerServiceMap.entrySet()) {
                //服务分组
                String groupName = entry.getValue().get(0).getGroupName();
                //创建服务提供者
                String serviceNode = entry.getKey();
                //     /config_register/ares/default/nan.remoting.test.HelloService/
                String servicePath = ZK_PATH + "/" + groupName + "/" + serviceNode + "/" + PROVIDER_TYPE;
                exist = zkClient.exists(servicePath);
                if (!exist) {
                    zkClient.createPersistent(servicePath, true);
                }

                //创建当前服务器节点
                int serverPort = entry.getValue().get(0).getServerPort();//服务端口
                int weight = entry.getValue().get(0).getWeight();//服务权重
                int workerThreads = entry.getValue().get(0).getWorkerThreads();//服务工作线程
                String localIp = IPHelper.localIp();
                String currentServiceIpNode = servicePath + "/" + localIp + "|" + serverPort + "|" + weight + "|" + workerThreads + "|" + groupName;
                exist = zkClient.exists(currentServiceIpNode);
                if (!exist) {
                    //注意,这里创建的是临时节点  /config_register/ares/default/ares.remoting.test.HelloService/provider/[192.168.43.1|8081|2|100|default]
                    zkClient.createEphemeral(currentServiceIpNode);
                }

                //监听注册服务的变化,同时更新数据到本地缓存
                zkClient.subscribeChildChanges(servicePath, new IZkChildListener() {
                    @Override
                    public void handleChildChange(String parentPath, List<String> currentChilds) throws Exception {
                        if (currentChilds == null) {
                            currentChilds = Lists.newArrayList();
                        }

                        //存活的服务IP列表   Lists.transform: a.对原有的list列表修改会影响Lists.transform已经生成列表 对Lists.transform生成的列表的元素进行修改可能无法生效
                        //在这里用来实时更新
                        List<String> activityServiceIpList = Lists.newArrayList(Lists.transform(currentChilds, new Function<String, String>() {
                            @Override
                            public String apply(String input) {
                                return StringUtils.split(input, "|")[0];
                            }
                        }));
                        refreshActivityService(activityServiceIpList);
                    }
                });

            }
        }
    }

    @Override
    public Map<String, List<ProviderService>> getProviderServiceMap() {
        return providerServiceMap;
    }


    @Override
    public void initProviderMap(String remoteAppKey, String groupName) {
        if (MapUtils.isEmpty(serviceMetaDataMap4Consume)) {
            serviceMetaDataMap4Consume.putAll(fetchOrUpdateServiceMetaData(remoteAppKey, groupName));
        }
    }

    @Override
    public Map<String, List<ProviderService>> getServiceMetaDataMap4Consume() {
        return serviceMetaDataMap4Consume;
    }

    @Override
    public void registerInvoker(InvokerService invoker) {
        if (invoker == null) {
            return;
        }

        //连接zk,注册服务
        synchronized (ZKRegisterCenter.class) {

            if (zkClient == null) {
                zkClient = new ZkClient(ZK_SERVICE, ZK_SESSION_TIME_OUT, ZK_CONNECTION_TIME_OUT, new SerializableSerializer());
            }
            //创建 ZK命名空间/当前部署应用APP命名空间/
            boolean exist = zkClient.exists(ROOT_PATH);
            if (!exist) {
                zkClient.createPersistent(ROOT_PATH, true);
            }


            //创建服务消费者节点
            String remoteAppKey = invoker.getRemoteAppKey();
            String groupName = invoker.getGroupName();
            String serviceNode = invoker.getServiceItf().getName();
            String servicePath = ROOT_PATH + "/" + remoteAppKey + "/" + groupName + "/" + serviceNode + "/" + INVOKER_TYPE;
            exist = zkClient.exists(servicePath);
            if (!exist) {
                zkClient.createPersistent(servicePath, true);
            }

            //创建当前服务器节点
            String localIp = IPHelper.localIp();
            String currentServiceIpNode = servicePath + "/" + localIp;
            exist = zkClient.exists(currentServiceIpNode);
            if (!exist) {
                //注意,这里创建的是临时节点
                zkClient.createEphemeral(currentServiceIpNode);
            }
        }
    }


    //利用ZK自动刷新当前存活的服务提供者列表数据
    private void refreshActivityService(List<String> serviceIpList) {
        if (serviceIpList == null) {
            serviceIpList = Lists.newArrayList();
        }

        Map<String, List<ProviderService>> currentServiceMetaDataMap = Maps.newHashMap();
        for (Map.Entry<String, List<ProviderService>> entry : providerServiceMap.entrySet()) {
            String key = entry.getKey();
            List<ProviderService> providerServices = entry.getValue();

            List<ProviderService> serviceMetaDataModelList = currentServiceMetaDataMap.get(key);
            if (serviceMetaDataModelList == null) {
                serviceMetaDataModelList = Lists.newArrayList();
            }

            for (ProviderService serviceMetaData : providerServices) {
                if (serviceIpList.contains(serviceMetaData.getServerIp())) {
                    serviceMetaDataModelList.add(serviceMetaData);
                }
            }
            currentServiceMetaDataMap.put(key, serviceMetaDataModelList);
        }
        providerServiceMap.clear();
        System.out.println("currentServiceMetaDataMap,"+ JSON.toJSONString(currentServiceMetaDataMap));
        providerServiceMap.putAll(currentServiceMetaDataMap);
    }


    private void refreshServiceMetaDataMap(List<String> serviceIpList) {
        if (serviceIpList == null) {
            serviceIpList = Lists.newArrayList();
        }

        Map<String, List<ProviderService>> currentServiceMetaDataMap = Maps.newHashMap();
        for (Map.Entry<String, List<ProviderService>> entry : serviceMetaDataMap4Consume.entrySet()) {
            String serviceItfKey = entry.getKey();
            List<ProviderService> serviceList = entry.getValue();

            List<ProviderService> providerServiceList = currentServiceMetaDataMap.get(serviceItfKey);
            if (providerServiceList == null) {
                providerServiceList = Lists.newArrayList();
            }

            for (ProviderService serviceMetaData : serviceList) {
                if (serviceIpList.contains(serviceMetaData.getServerIp())) {
                    providerServiceList.add(serviceMetaData);
                }
            }
            currentServiceMetaDataMap.put(serviceItfKey, providerServiceList);
        }

        serviceMetaDataMap4Consume.clear();
        serviceMetaDataMap4Consume.putAll(currentServiceMetaDataMap);
    }


    private Map<String, List<ProviderService>> fetchOrUpdateServiceMetaData(String remoteAppKey, String groupName) {
        final Map<String, List<ProviderService>> providerServiceMap = Maps.newConcurrentMap();
        //连接zk
        synchronized (ZKRegisterCenter.class) {
            if (zkClient == null) {
                zkClient = new ZkClient(ZK_SERVICE, ZK_SESSION_TIME_OUT, ZK_CONNECTION_TIME_OUT, new SerializableSerializer());
            }
        }

        //从ZK获取服务提供者列表
        String providePath = ROOT_PATH + "/" + remoteAppKey + "/" + groupName;
        List<String> providerServices = zkClient.getChildren(providePath);

        for (String serviceName : providerServices) {
            String servicePath = providePath + "/" + serviceName + "/" + PROVIDER_TYPE;
            List<String> ipPathList = zkClient.getChildren(servicePath);
            for (String ipPath : ipPathList) {
                String serverIp = StringUtils.split(ipPath, "|")[0];
                String serverPort = StringUtils.split(ipPath, "|")[1];
                int weight = Integer.parseInt(StringUtils.split(ipPath, "|")[2]);
                int workerThreads = Integer.parseInt(StringUtils.split(ipPath, "|")[3]);
                String group = StringUtils.split(ipPath, "|")[4];

                List<ProviderService> providerServiceList = providerServiceMap.get(serviceName);
                if (providerServiceList == null) {
                    providerServiceList = Lists.newArrayList();
                }
                ProviderService providerService = new ProviderService();

                try {
                    providerService.setServiceItf(ClassUtils.getClass(serviceName));
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException(e);
                }

                providerService.setServerIp(serverIp);
                providerService.setServerPort(Integer.parseInt(serverPort));
                providerService.setWeight(weight);
                providerService.setWorkerThreads(workerThreads);
                providerService.setGroupName(group);
                providerServiceList.add(providerService);

                providerServiceMap.put(serviceName, providerServiceList);
            }

            //监听注册服务的变化,同时更新数据到本地缓存
            zkClient.subscribeChildChanges(servicePath, new IZkChildListener() {
                @Override
                public void handleChildChange(String parentPath, List<String> currentChilds) throws Exception {
                    if (currentChilds == null) {
                        currentChilds = Lists.newArrayList();
                    }
                    currentChilds = Lists.newArrayList(Lists.transform(currentChilds, new Function<String, String>() {
                        @Override
                        public String apply(String input) {
                            return StringUtils.split(input, "|")[0];
                        }
                    }));
                    refreshServiceMetaDataMap(currentChilds);
                }
            });
        }
        return providerServiceMap;
    }

    @Override
    public Pair<List<ProviderService>, List<InvokerService>> queryProvidersAndInvokers(String serviceName, String appKey) {
        //服务消费者列表
        List<InvokerService> invokerServices = Lists.newArrayList();
        //服务提供者列表
        List<ProviderService> providerServices = Lists.newArrayList();

        //连接zk
        if (zkClient == null) {
            synchronized (ZKRegisterCenter.class) {
                if (zkClient == null) {
                    zkClient = new ZkClient(ZK_SERVICE, ZK_SESSION_TIME_OUT, ZK_CONNECTION_TIME_OUT, new SerializableSerializer());
                }
            }
        }

        String parentPath = ROOT_PATH + "/" + appKey;
        //获取 ROOT_PATH + APP_KEY注册中心子目录列表
        List<String> groupServiceList = zkClient.getChildren(parentPath);
        if (CollectionUtils.isEmpty(groupServiceList)) {
            return Pair.of(providerServices, invokerServices);
        }

        for (String group : groupServiceList) {
            String groupPath = parentPath + "/" + group;
            //获取ROOT_PATH + APP_KEY + group 注册中心子目录列表
            List<String> serviceList = zkClient.getChildren(groupPath);
            if (CollectionUtils.isEmpty(serviceList)) {
                continue;
            }
            for (String service : serviceList) {
                //获取ROOT_PATH + APP_KEY + group +service 注册中心子目录列表
                String servicePath = groupPath + "/" + service;
                List<String> serviceTypes = zkClient.getChildren(servicePath);
                if (CollectionUtils.isEmpty(serviceTypes)) {
                    continue;
                }
                for (String serviceType : serviceTypes) {
                    if (StringUtils.equals(serviceType, PROVIDER_TYPE)) {
                        //获取ROOT_PATH + APP_KEY + group +service+serviceType 注册中心子目录列表
                        String providerPath = servicePath + "/" + serviceType;
                        List<String> providers = zkClient.getChildren(providerPath);
                        if (CollectionUtils.isEmpty(providers)) {
                            continue;
                        }

                        //获取服务提供者信息
                        for (String provider : providers) {
                            String[] providerNodeArr = StringUtils.split(provider, "|");

                            ProviderService providerService = new ProviderService();
                            providerService.setAppKey(appKey);
                            providerService.setGroupName(group);
                            providerService.setServerIp(providerNodeArr[0]);
                            providerService.setServerPort(Integer.parseInt(providerNodeArr[1]));
                            providerService.setWeight(Integer.parseInt(providerNodeArr[2]));
                            providerService.setWorkerThreads(Integer.parseInt(providerNodeArr[3]));
                            providerServices.add(providerService);
                        }

                    } else if (StringUtils.equals(serviceType, INVOKER_TYPE)) {
                        //获取ROOT_PATH + APP_KEY + group +service+serviceType 注册中心子目录列表
                        String invokerPath = servicePath + "/" + serviceType;
                        List<String> invokers = zkClient.getChildren(invokerPath);
                        if (CollectionUtils.isEmpty(invokers)) {
                            continue;
                        }

                        //获取服务消费者信息
                        for (String invoker : invokers) {
                            InvokerService invokerService = new InvokerService();
                            invokerService.setRemoteAppKey(appKey);
                            invokerService.setGroupName(group);
                            invokerService.setInvokerIp(invoker);
                            invokerServices.add(invokerService);
                        }
                    }
                }
            }

        }
        //Obtains an immutable pair of from two objects inferring the generic types.
        return Pair.of(providerServices, invokerServices);
    }
}
