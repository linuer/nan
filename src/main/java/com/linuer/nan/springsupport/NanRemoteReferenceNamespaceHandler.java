package com.linuer.nan.springsupport;

import org.springframework.beans.factory.xml.NamespaceHandlerSupport;

/**
 * 描述:
 *  服务引入自定义标签
 * @author linuer
 * @create 2017-11-05 0:34
 */
public class NanRemoteReferenceNamespaceHandler extends NamespaceHandlerSupport {

    public void init() {
        registerBeanDefinitionParser("reference", new RevokerFactoryBeanDefinitionParser());
    }
}
