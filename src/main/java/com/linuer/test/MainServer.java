package com.linuer.test;

import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * 描述:
 *
 * @author linuer
 * @create 2017-11-05 20:29
 */
public class MainServer {

    public static void main(String[] args) throws Exception {

        //发布服务
        final ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("nan-server.xml");
        System.out.println(" 服务发布完成");
    }
}
