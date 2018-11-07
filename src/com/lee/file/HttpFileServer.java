package com.lee.file;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.regex.Pattern;

import javax.activation.MimetypesFileTypeMap;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelProgressiveFuture;
import io.netty.channel.ChannelProgressiveFutureListener;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.stream.ChunkedFile;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.util.CharsetUtil;

public class HttpFileServer {
	private static final String DEFAULT_URL = "/src/com/lee/netty/";
	
	public void run(final int port, final String url) throws Exception{
		EventLoopGroup bossGroup = new NioEventLoopGroup();
		EventLoopGroup workGroup = new NioEventLoopGroup();
		try{
			ServerBootstrap boots = new ServerBootstrap();
			boots.group(bossGroup, workGroup)
				.channel(NioServerSocketChannel.class)
				.childHandler(new ChannelInitializer<SocketChannel>() {
					@Override
					public void initChannel(SocketChannel ch) throws Exception{
						ch.pipeline().addLast("http-decoder", new HttpRequestDecoder());	//添加请求解码器
						ch.pipeline().addLast("http-aggregator", new HttpObjectAggregator(65536));
						ch.pipeline().addLast("http-encoder", new HttpResponseEncoder()); //添加响应编码器
						ch.pipeline().addLast("http-chunked", new ChunkedWriteHandler());
						ch.pipeline().addLast("fileSeverHandler", new HttpFileServerHandler(url));
					}
				});
			ChannelFuture cf = boots.bind("127.0.0.1", port).sync();
			System.out.println("HTTP 文件服务器启动，网址是：" + "http://127.0.0.1:" + port + url);
			cf.channel().closeFuture().sync();
		}finally{
			bossGroup.shutdownGracefully();
			workGroup.shutdownGracefully();
		}
	}
	
	public static void main(String[] args) throws Exception{
		int port = 8080;
		if(args.length > 0){
			try{
				port = Integer.valueOf(args[0]);
			}catch (NumberFormatException e) {
				e.printStackTrace();
			}
		}
		String url = DEFAULT_URL;
		if(args.length > 1){
			url = args[1];
		}
		new HttpFileServer().run(port, url);
	}
}	

class HttpFileServerHandler extends SimpleChannelInboundHandler<FullHttpRequest>{
	private final String url;
	
	public HttpFileServerHandler(String url){
		this.url = url;
	}

	@Override
	protected void messageReceived(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {
//		if(request.getUri().equals("/favicon.ico")){
//			return;
//		}
		if(!request.getDecoderResult().isSuccess()){
			sendError(ctx, HttpResponseStatus.BAD_REQUEST);
			return;
		}
		if(request.getMethod() != HttpMethod.GET){
			sendError(ctx, HttpResponseStatus.METHOD_NOT_ALLOWED);
			return;
		}
		final String uri = request.getUri();
		final String path = sanitizeUri(uri);
		if(path == null){
			sendError(ctx, HttpResponseStatus.FORBIDDEN);
			return;
		}
		File file = new File(path);
		if(file.isHidden() || !file.exists()){
			sendError(ctx, HttpResponseStatus.NOT_FOUND);
		}
		if(file.isDirectory()){
			if(uri.endsWith("/")){
				sendListing(ctx, file);
			}else{
				sendRedirect(ctx, uri + "/");
			}
			return;
		}
		if(!file.isFile()){
			sendError(ctx, HttpResponseStatus.FORBIDDEN);
			return;
		}
		RandomAccessFile randomAccessFile = null;
		try{
			randomAccessFile = new RandomAccessFile(file, "r");
		}catch (FileNotFoundException e) {
			sendError(ctx, HttpResponseStatus.NOT_FOUND);
			return;
		}
		long fileLength = randomAccessFile.length();
		HttpResponse httpResponse = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
		HttpHeaders.setContentLength(httpResponse, fileLength);
		setContentTypeHeader(httpResponse, file);
		if(HttpHeaders.isKeepAlive(request)){
			httpResponse.headers().set(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
		}
		ctx.write(httpResponse);
		ChannelFuture sendFileFuture; 
		sendFileFuture = ctx.write(new ChunkedFile(randomAccessFile, 0, fileLength, 8192), ctx.newProgressivePromise());
		sendFileFuture.addListener(new ChannelProgressiveFutureListener() {
			
			@Override
			public void operationComplete(ChannelProgressiveFuture arg0) throws Exception {
				System.out.println("Transfer complete.");
			}
			
			@Override
			public void operationProgressed(ChannelProgressiveFuture future, long progress, long total) throws Exception {
				if(total < 0){
					System.err.println("Transfer progress : " + progress);
				}else{
					System.err.print("Transfer progress : " + progress + "/" + total);
				}
			}  
		});
		ChannelFuture lastContentFuture = ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);
		if(!HttpHeaders. isKeepAlive(request)){
			lastContentFuture.addListener(ChannelFutureListener.CLOSE);
		}
	}
	
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception{
		cause.printStackTrace();
		if(ctx.channel().isActive()){
			sendError(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR);
		}
	}
		
	private static final Pattern INSECURE_URI = Pattern.compile(".*[<>&\"].*");
	
	private String sanitizeUri(String uri){
			try {
				uri = URLDecoder.decode(uri, "UTF-8");
			} catch (UnsupportedEncodingException e) {
				try{
					uri = URLDecoder.decode(uri, "ISO-8859-1");
				}catch (UnsupportedEncodingException e1) {
					throw new Error();
				}
			}
			if(!uri.startsWith(url)){
				return null;
			}
			if(!uri.startsWith("/")){
				return null;
			}
			uri = uri.replace('/', File.separatorChar);
			if(uri.contains(File.separator + ".")
					|| uri.contains("." + File.separator)
					|| uri.startsWith(".")
					|| uri.endsWith(".")
					|| INSECURE_URI.matcher(uri).matches()){
				return null;
			}
			return System.getProperty("user.dir") + uri;
	}
	private static final Pattern ALLOWED_FILE_NAME = Pattern.compile("[A-Za-z0-9][-_A-Za-z0-9\\.]*");

	private static void sendListing(ChannelHandlerContext ctx, File dir){
		FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
		response.headers().set("Content-Type", "text/html;charset=UTF-8");
		StringBuilder buf = new StringBuilder();
		String dirPath = dir.getPath();
		buf.append("<!DOCTYPE html>\r\n");
		buf.append("<html><head><title>");
		buf.append(dirPath);
		buf.append(" 目录: ");
		buf.append("</title><link rel=\"icon\" href=\"data:;base64,=\"></head><body>\r\n");
		buf.append("<h3>");
		buf.append(dirPath).append(" 目录: ");
		buf.append("</h3>\r\n");
		buf.append("<ul>");
		buf.append("<li>链接：<a href=\"../\">..</a></li>\r\n");
		for(File f : dir.listFiles()){
			if(f.isHidden() || !f.canRead()){
				continue;
			}
			String name = f.getName();
			if(!ALLOWED_FILE_NAME.matcher(name).matches()){
				continue;
			}
		    buf.append("<li>链接：<a href=\"");
            buf.append(name);
            buf.append("\">");
            buf.append(name);
			buf.append("</a></li>\r\n");
		}
		buf.append("</ul></body></html>\r\n");
		ByteBuf buffer = Unpooled.copiedBuffer(buf, CharsetUtil.UTF_8);
		response.content().writeBytes(buffer);
		buffer.release();
		ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
	}
	
	private static void sendRedirect(ChannelHandlerContext ctx, String newUri){
		FullHttpResponse respon = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.FOUND);
		respon.headers().set(HttpHeaders.Names.LOCATION, newUri);
		ctx.writeAndFlush(respon).addListener(ChannelFutureListener.CLOSE);
	}
	
	private static void sendError(ChannelHandlerContext ctx, HttpResponseStatus status){
		FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status, Unpooled.copiedBuffer("Failure: " + status.toString() + "\r\n", CharsetUtil.UTF_8));
		response.headers().set(HttpHeaders.Names.CONTENT_TYPE, "text/plain; charset=UTF-8");
		ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
	}
	
	private static void setContentTypeHeader(HttpResponse response, File file){
		MimetypesFileTypeMap mieTypesMap = new MimetypesFileTypeMap();
		response.headers().set(HttpHeaders.Names.CONTENT_TYPE, mieTypesMap.getContentType(file.getPath()));
	}
}
