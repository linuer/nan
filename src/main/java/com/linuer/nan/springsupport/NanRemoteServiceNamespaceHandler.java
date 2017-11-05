package com.linuer.nan.springsupport;

import org.springframework.beans.factory.xml.NamespaceHandlerSupport;

/**
 * 描述:
 * 服务引入自定义标签
 * @author linuer
 * @create 2017-11-05 9:59
 */
public class NanRemoteServiceNamespaceHandler extends NamespaceHandlerSupport {

    @Override
    public void init() {
    registerBeanDefinitionParser("service", new ProviderFactoryBeanDefinitionParser());
}
}
