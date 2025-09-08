package software.xdev.mockserver.codec;

import io.netty.handler.codec.http.HttpContentDecompressor;


@SuppressWarnings("java:S110")
public class LimitedHttpContentDecompressor extends HttpContentDecompressor
{
	@SuppressWarnings("checkstyle:MagicNumber")
	public LimitedHttpContentDecompressor()
	{
		super(1_000_000_000); // 1 GB
	}
}
