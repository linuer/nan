package com.linuer.nan.serialization.serializer;

/**
 * 描述:
 *  序列化接口
 * @author linuer
 * @create 2017-11-04 10:32
 */
public interface ISerializer {

    /**
     * 序列化
     * @param obj
     * @param <T>
     * @return
     */
    public <T> byte[] serialize(T obj);

    /**
     * 反序列化
     * @param data
     * @param clazz
     * @param <T>
     * @return
     */
    public <T> T deserialize(byte[] data, Class<T> clazz);
}
