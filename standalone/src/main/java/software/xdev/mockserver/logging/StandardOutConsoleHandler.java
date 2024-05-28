package software.xdev.mockserver.logging;

import java.util.logging.LogRecord;
import java.util.logging.StreamHandler;


// Used by MockServerLoggerConfiguration
public class StandardOutConsoleHandler extends StreamHandler
{
	@SuppressWarnings("java:S106")
	public StandardOutConsoleHandler()
	{
		this.setOutputStream(System.out);
	}
	
	@Override
	public void publish(final LogRecord r)
	{
		super.publish(r);
		this.flush();
	}
	
	@Override
	public void close()
	{
		this.flush();
	}
}
