package com.linuer.test;

/**
 * 描述:
 *
 * @author linuer
 * @create 2017-11-05 20:28
 */
public class HelloServiceImpl implements HelloService {

    @Override
    public String sayHello(String somebody) {
        return "hello " + somebody + "!";
    }
}
