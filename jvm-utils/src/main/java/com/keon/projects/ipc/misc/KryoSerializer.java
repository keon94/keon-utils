package com.keon.projects.ipc.misc;

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
import java.lang.invoke.SerializedLambda;

public class KryoSerializer {

    private static final ThreadLocal<Kryo> LOCAL_KRYO = ThreadLocal.withInitial(KryoSerializer::newKryo);

    public byte[] serialize(final Object object) throws KryoException {
        if (object == null)
            return null;
        try(final ByteArrayOutputStream outputStream = new ByteArrayOutputStream(2048)) {
            final Kryo kryo = LOCAL_KRYO.get();
            kryo.setClassLoader(this.getClass().getClassLoader());
            try (final Output output = new Output(outputStream)) {
                kryo.writeClassAndObject(output, object);
            }
            return outputStream.toByteArray();
        } catch (final IOException e) {
            throw new KryoException(e);
        }
    }

    public <O> O deserialize(final byte[] bytes) throws KryoException {
        if (bytes == null || bytes.length == 0 || isEmpty(bytes))
            return null;
        try(final InputStream inputStream = new ByteArrayInputStream(bytes)) {
            final Kryo kryo = LOCAL_KRYO.get();
            kryo.setClassLoader(this.getClass().getClassLoader());
            try(final Input input = new Input(inputStream)) {
                return (O) kryo.readClassAndObject(input);
            }
        } catch (final IOException e) {
            throw new KryoException(e);
        }
    }

    private static Kryo newKryo() {
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

    private static boolean isEmpty(final byte[] bytes) {
        for(final byte b : bytes) {
            if(b != 0)
                return false;
        }
        return true;
    }
}
