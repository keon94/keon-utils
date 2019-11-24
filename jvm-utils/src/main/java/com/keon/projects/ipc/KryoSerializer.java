package com.keon.projects.ipc;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoException;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.serializers.ClosureSerializer;
import com.esotericsoftware.kryo.serializers.CompatibleFieldSerializer;
import com.esotericsoftware.kryo.util.DefaultInstantiatorStrategy;
import org.objenesis.strategy.StdInstantiatorStrategy;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.invoke.SerializedLambda;

class KryoSerializer {

    private static final ThreadLocal<Kryo> LOCAL_KRYO = ThreadLocal.withInitial(() -> newKryo());

    public byte[] serialize(final Object object) throws KryoException {
        if (object == null)
            return null;
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream(2048);
        serialize(object, outputStream);
        return outputStream.toByteArray();
    }

    public <O> O deserialize(final byte[] bytes) throws KryoException {
        if (bytes == null || bytes.length == 0 || isNull(bytes))
            return null;
        return deserialize(new ByteArrayInputStream(bytes));
    }

    private int serialize(final Object object, final OutputStream outputStream) {
        if (object == null) {
            try {
                outputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return 0;
        }
        final Kryo kryo = LOCAL_KRYO.get();
        kryo.setClassLoader(this.getClass().getClassLoader());
        final Output output = new Output(outputStream);
        try {
            kryo.writeClassAndObject(output, object);
        } finally {
            output.close();
        }
        return (int) output.total();
    }

    private <O> O deserialize(final InputStream inputStream) {
        if (inputStream == null) {
            return null;
        }
        final Kryo kryo = LOCAL_KRYO.get();
        kryo.setClassLoader(this.getClass().getClassLoader());
        final Input input = new Input(inputStream);
        try {
            return (O) kryo.readClassAndObject(input);
        } finally {
            input.close();
        }
    }

    private static final Kryo newKryo() {
        final Kryo kryo = new Kryo();
        final DefaultInstantiatorStrategy strategy = new DefaultInstantiatorStrategy();
        strategy.setFallbackInstantiatorStrategy(new StdInstantiatorStrategy());
        kryo.setInstantiatorStrategy(strategy);
        kryo.setDefaultSerializer(CompatibleFieldSerializer.class);
        kryo.setRegistrationRequired(false);
        kryo.register(Object[].class);
        kryo.register(Class.class);
        kryo.register(SerializedLambda.class);
        kryo.register(ClosureSerializer.Closure.class, new ClosureSerializer());
        return kryo;
    }

    private static boolean isNull(final byte[] bytes) {
        for(final byte b : bytes) {
            if(b != 0)
                return false;
        }
        return true;
    }
}
