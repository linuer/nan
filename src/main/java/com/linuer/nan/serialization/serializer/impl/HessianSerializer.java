package com.linuer.nan.serialization.serializer.impl;

import com.caucho.hessian.io.HessianInput;
import com.caucho.hessian.io.HessianOutput;
import com.linuer.nan.serialization.serializer.ISerializer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

/**
 * 描述:
 *
 * @author linuer
 * @create 2017-11-04 19:33
 */
public class HessianSerializer implements ISerializer {
    public byte[] serialize(Object obj) {
        if (obj == null)
            throw new NullPointerException();

        try {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            HessianOutput ho = new HessianOutput(os);
            ho.writeObject(obj);
            return os.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public <T> T deserialize(byte[] data, Class<T> clazz) {
        if (data == null)
            throw new NullPointerException();

        try {
            ByteArrayInputStream is = new ByteArrayInputStream(data);
            HessianInput hi = new HessianInput(is);
            return (T) hi.readObject();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }
}
