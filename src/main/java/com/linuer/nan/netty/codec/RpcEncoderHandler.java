package com.linuer.nan.netty.codec;

import com.linuer.nan.serialization.SerializeType;
import com.linuer.nan.serialization.engine.SerializerEngine;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

/**
 * 描述:
 *
 * @author linuer
 * @create 2017-11-05 11:15
 */
public class RpcEncoderHandler extends MessageToByteEncoder {

    //序列化类型
    private SerializeType serializeType;

    public RpcEncoderHandler(SerializeType serializeType) {
        this.serializeType = serializeType;
    }

    @Override
    public void encode(ChannelHandlerContext ctx, Object in, ByteBuf out) throws Exception {
        //将对象序列化为字节数组
        byte[] data = SerializerEngine.serialize(in, serializeType.getSerializeType());
        //将字节数组(消息体)的长度作为消息头写入,解决半包/粘包问题
        out.writeInt(data.length);
        //写入序列化后得到的字节数组
        out.writeBytes(data);
    }
}
