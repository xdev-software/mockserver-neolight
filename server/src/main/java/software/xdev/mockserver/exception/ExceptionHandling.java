/*
 * Copyright © 2024 XDEV Software (https://xdev.software)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package software.xdev.mockserver.exception;

import java.net.ConnectException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SocketChannel;
import java.security.SignatureException;
import java.security.cert.CertPathValidatorException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import javax.net.ssl.SSLException;
import javax.net.ssl.SSLHandshakeException;

import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.ssl.NotSslRecordException;
import io.netty.util.internal.PlatformDependent;
import software.xdev.mockserver.httpclient.SocketConnectionException;


public final class ExceptionHandling
{
	private static final Predicate<String> IGNORABLE_CLASS_IN_STACK_PRE_CHECK =
		s -> s.contains("Channel");
	@SuppressWarnings("java:S5852")
	private static final Pattern IGNORABLE_CLASS_IN_STACK =
		Pattern.compile("^.*(?:Socket|Datagram|Sctp|Udt)Channel.*$");
	
	private static final Predicate<String> IGNORABLE_ERROR_MESSAGE_PRE_CHECK =
		s -> s.toLowerCase().contains("connection") || s.toLowerCase().contains("broken");
	@SuppressWarnings("java:S5852")
	private static final Pattern IGNORABLE_ERROR_MESSAGE =
		Pattern.compile("^.*(?:connection.*(?:reset|closed|abort|broken)|broken.*pipe).*$", Pattern.CASE_INSENSITIVE);
	
	/**
	 * Closes the specified channel after all queued write requests are flushed.
	 */
	public static void closeOnFlush(final Channel ch)
	{
		if(ch != null && ch.isActive())
		{
			ch.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
		}
	}
	
	/**
	 * returns true is the exception was caused by the connection being closed
	 */
	@SuppressWarnings("java:S1872") // Not always given
	public static boolean connectionClosedException(final Throwable throwable)
	{
		final String message = String.valueOf(throwable.getMessage()).toLowerCase();
		
		// is ssl exception
		if(throwable.getCause() instanceof SSLException || throwable instanceof DecoderException
			|| throwable instanceof NotSslRecordException)
		{
			return false;
		}
		
		// first try to match connection reset / broke peer based on the regex.
		// This is the fastest way but may fail on different jdk impls or OS's
		if(IGNORABLE_ERROR_MESSAGE_PRE_CHECK.test(message) && IGNORABLE_ERROR_MESSAGE.matcher(message).matches())
		{
			return false;
		}
		
		// Inspect the StackTraceElements to see if it was a connection reset / broken pipe or not
		final StackTraceElement[] elements = throwable.getStackTrace();
		for(final StackTraceElement element : elements)
		{
			final String classname = element.getClassName();
			final String methodname = element.getMethodName();
			
			// skip all classes that belong to the io.netty package
			if(classname.startsWith("io.netty."))
			{
				continue;
			}
			
			// check if the method name is read if not skip it
			if(!"read".equals(methodname))
			{
				continue;
			}
			
			// This will also match against SocketInputStream which is used by openjdk 7 and maybe
			// also others
			if(IGNORABLE_CLASS_IN_STACK_PRE_CHECK.test(classname)
				&& IGNORABLE_CLASS_IN_STACK.matcher(classname).matches())
			{
				return false;
			}
			
			try
			{
				// No match by now. Try to load the class via classloader and inspect it.
				// This is mainly done as other JDK implementations may differ in name of
				// the impl.
				final Class<?> clazz = PlatformDependent.getClassLoader(ExceptionHandling.class).loadClass(classname);
				
				if(SocketChannel.class.isAssignableFrom(clazz)
					|| DatagramChannel.class.isAssignableFrom(clazz))
				{
					return false;
				}
				
				// also match against SctpChannel via String matching as it may not present.
				if("com.sun.nio.sctp.SctpChannel".equals(clazz.getSuperclass().getName()))
				{
					return false;
				}
			}
			catch(final ClassNotFoundException e)
			{
				// This should not happen just ignore
			}
		}
		return true;
	}
	
	private static final List<Class<? extends Exception>> SSL_HANDSHAKE_FAILURE_CLASSES =
		Arrays.asList(
			SSLException.class,
			SSLHandshakeException.class,
			CertPathValidatorException.class,
			SignatureException.class);
	
	public static boolean sslHandshakeException(final Throwable throwable)
	{
		for(final Class<? extends Throwable> cause : getCauses(throwable))
		{
			if(SSL_HANDSHAKE_FAILURE_CLASSES.contains(cause))
			{
				return true;
			}
		}
		return false;
	}
	
	private static final List<Class<? extends Exception>> CONNECTION_EXCEPTION_CLASSES =
		Arrays.asList(SocketConnectionException.class, ConnectException.class);
	
	public static boolean connectionException(final Throwable throwable)
	{
		for(final Class<? extends Throwable> cause : getCauses(throwable))
		{
			if(CONNECTION_EXCEPTION_CLASSES.contains(cause))
			{
				return true;
			}
		}
		return false;
	}
	
	private static List<Class<? extends Throwable>> getCauses(final Throwable throwable)
	{
		if(throwable.getCause() != null)
		{
			if(throwable.getClass().equals(throwable.getCause().getClass()))
			{
				return new ArrayList<>(Collections.singletonList(throwable.getClass()));
			}
			else
			{
				final List<Class<? extends Throwable>> causes = getCauses(throwable.getCause());
				causes.add(throwable.getClass());
				return causes;
			}
		}
		else
		{
			return new ArrayList<>(Collections.singletonList(throwable.getClass()));
		}
	}
	
	private ExceptionHandling()
	{
	}
}
