package org.grails.plugins.springsession.converters;

import org.springframework.core.convert.converter.Converter;
import org.springframework.core.serializer.support.DeserializingConverter;
import org.springframework.core.serializer.support.SerializingConverter;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;
import org.springframework.util.Assert;

public class LazyDeserializationRedisSerializer implements RedisSerializer<Object> {


	private Converter<Object, byte[]> serializer = new SerializingConverter();
	private Converter<byte[], Object> deserializer;

	public LazyDeserializationRedisSerializer(ClassLoader classLoader) {
		Assert.notNull(classLoader);
		deserializer = new DeserializingConverter(new JdkDeserializer(classLoader, false));
	}

	@Override
	public byte[] serialize(Object object) throws SerializationException {
		if (object == null) {
			return new byte[0];
		}

		try {
			if (object instanceof LazyDeserializationObject) {
				LazyDeserializationObject lazyDeserializationObject = (LazyDeserializationObject) object;
				lazyDeserializationObject.setSerialized(serializer.convert(lazyDeserializationObject.getDeserializaded()));
				return serializer.convert(lazyDeserializationObject);
			} else {
				return serializer.convert(object);
			}
		} catch (Exception ex) {
			throw new SerializationException("Cannot serialize", ex);
		}
	}

	@Override
	public Object deserialize(byte[] bytes) throws SerializationException {
		if (bytes == null || bytes.length == 0) {
			return null;
		}

		try {
			return deserializer.convert(bytes);
		} catch (Exception ex) {
			throw new SerializationException("Cannot deserialize", ex);
		}
	}


}
