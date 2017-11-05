package com.linuer.nan.serialization.engine;

import com.google.common.collect.Maps;
import com.linuer.nan.serialization.serializer.ISerializer;
import com.linuer.nan.serialization.SerializeType;
import com.linuer.nan.serialization.serializer.impl.HessianSerializer;
import com.linuer.nan.serialization.serializer.impl.JSONSerializer;
import com.linuer.nan.serialization.serializer.impl.ProtoStuffSerializer;

import java.util.Map;

/**
 * 描述:
 *
 * @author linuer
 * @create 2017-11-04 19:35
 */
public class SerializerEngine {
    public static final Map<SerializeType, ISerializer> serializerMap = Maps.newConcurrentMap();

    static {
        serializerMap.put(SerializeType.HessianSerializer, new HessianSerializer());
        serializerMap.put(SerializeType.JSONSerializer, new JSONSerializer());
        serializerMap.put(SerializeType.ProtoStuffSerializer, new ProtoStuffSerializer());
    }

    public static <T> byte[] serialize(T obj, String serializeType) {
        SerializeType serialize = SerializeType.queryByType(serializeType);
        if (serialize == null) {
            throw new RuntimeException("serialize is null");
        }

        ISerializer serializer = serializerMap.get(serialize);
        if (serializer == null) {
            throw new RuntimeException("serialize error");
        }

        try {
            return serializer.serialize(obj);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> T deserialize(byte[] data, Class<T> clazz, String serializeType) {

        SerializeType serialize = SerializeType.queryByType(serializeType);
        if (serialize == null) {
            throw new RuntimeException("serialize is null");
        }
        ISerializer serializer = serializerMap.get(serialize);
        if (serializer == null) {
            throw new RuntimeException("serialize error");
        }

        try {
            return serializer.deserialize(data, clazz);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
