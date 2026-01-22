package software.xdev.mockserver.serialization;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;


public abstract class AbstractSerializer<T> implements Serializer<T>
{
	protected final ObjectWriter objectWriter;
	protected final ObjectMapper objectMapper;
	
	protected AbstractSerializer()
	{
		this(ObjectMappers.PRETTY_PRINT_WRITER);
	}
	
	protected AbstractSerializer(final ObjectWriter objectWriter)
	{
		this(objectWriter, ObjectMappers.DEFAULT_MAPPER);
	}
	
	protected AbstractSerializer(final ObjectWriter objectWriter, final ObjectMapper objectMapper)
	{
		this.objectWriter = objectWriter;
		this.objectMapper = objectMapper;
	}
}
