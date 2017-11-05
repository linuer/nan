package com.linuer.nan.netty.server;

import com.alibaba.fastjson.JSON;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.Maps;
import com.linuer.nan.model.ProviderService;
import com.linuer.nan.model.RpcRequest;
import com.linuer.nan.model.RpcResponse;
import com.linuer.nan.registry.IRegisterCenter4Provider;
import com.linuer.nan.registry.impl.zookeeper.ZKRegisterCenter;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * 描述:
 *
 * @author linuer
 * @create 2017-11-05 11:18
 */
@ChannelHandler.Sharable
public class NettyServerInvokeHandler  extends SimpleChannelInboundHandler<RpcRequest> {

    private static final Logger logger = LoggerFactory.getLogger(NettyServerInvokeHandler.class);

    //服务端限流
    private static final Map<String, Semaphore> serviceKeySemaphoreMap = Maps.newConcurrentMap();

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        //发生异常,关闭链路
        ctx.close();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RpcRequest request) throws Exception {

        if (ctx.channel().isWritable()) {
            //从服务调用对象里获取服务提供者信息
            ProviderService metaDataModel = request.getProviderService();
            long consumeTimeOut = request.getInvokeTimeout();
            final String methodName = request.getInvokedMethodName();

            //根据方法名称定位到具体某一个服务提供者
            String serviceKey = metaDataModel.getServiceItf().getName();
            //获取限流工具类
            int workerThread = metaDataModel.getWorkerThreads();
            Semaphore semaphore = serviceKeySemaphoreMap.get(serviceKey);
            if (semaphore == null) {
                synchronized (serviceKeySemaphoreMap) {
                    semaphore = serviceKeySemaphoreMap.get(serviceKey);
                    if (semaphore == null) {
                        semaphore = new Semaphore(workerThread);  //信号量：允许多个线程同时访问（服务端线程数）
                        serviceKeySemaphoreMap.put(serviceKey, semaphore);
                    }
                }
            }

            //获取注册中心服务
            IRegisterCenter4Provider registerCenter4Provider = ZKRegisterCenter.singleton();
            List<ProviderService> localProviderCaches = registerCenter4Provider.getProviderServiceMap().get(serviceKey);

            Object result = null;
            boolean acquire = false;

            try {
                ProviderService localProviderCache = Collections2.filter(localProviderCaches, new Predicate<ProviderService>() {
                    @Override
                    public boolean apply(ProviderService input) {
                        return StringUtils.equals(input.getServiceMethod().getName(), methodName); //根据这个条件，选出localProviderCache
                    }
                }).iterator().next();
                Object serviceObject = localProviderCache.getServiceObject();

                //利用反射发起服务调用
                Method method = localProviderCache.getServiceMethod();
                //利用semaphore实现限流
                acquire = semaphore.tryAcquire(consumeTimeOut, TimeUnit.MILLISECONDS);
                if (acquire) {
                    result = method.invoke(serviceObject, request.getArgs());
                    //System.out.println("---------------"+result);
                }
            } catch (Exception e) {
                System.out.println(JSON.toJSONString(localProviderCaches) + "  " + methodName+" "+e.getMessage());
                result = e;
            } finally {
                if (acquire) {
                    semaphore.release();
                }
            }

            //根据服务调用结果组装调用返回对象
            RpcResponse response = new RpcResponse();
            response.setInvokeTimeout(consumeTimeOut);
            response.setUniqueKey(request.getUniqueKey());
            response.setResult(result);

            //将服务调用返回对象回写到消费端
            ctx.writeAndFlush(response);


        } else {
            logger.error("------------channel closed!---------------");
        }
    }



}
