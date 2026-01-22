package software.xdev.mockserver.serialization;

import tools.jackson.databind.ObjectWriter;
import tools.jackson.databind.json.JsonMapper;


public abstract class AbstractSerializer<T> implements Serializer<T>
{
	protected final ObjectWriter objectWriter;
	protected final JsonMapper objectMapper;
	
	protected AbstractSerializer()
	{
		this(ObjectMappers.PRETTY_PRINT_WRITER);
	}
	
	protected AbstractSerializer(final ObjectWriter objectWriter)
	{
		this(objectWriter, ObjectMappers.DEFAULT_MAPPER);
	}
	
	protected AbstractSerializer(final ObjectWriter objectWriter, final JsonMapper objectMapper)
	{
		this.objectWriter = objectWriter;
		this.objectMapper = objectMapper;
	}
}
