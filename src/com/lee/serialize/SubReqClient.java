package com.lee.serialize;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;

public class SubReqClient {
	public void connet(int port, String host) throws Exception{
		EventLoopGroup group = new NioEventLoopGroup();
		try{
			Bootstrap boots = new Bootstrap();
			boots.group(group)
				.channel(NioSocketChannel.class)
				.option(ChannelOption.TCP_NODELAY, true)
				.handler(new ChannelInitializer<SocketChannel>() {
					@Override
					public void initChannel(SocketChannel ch){
						ch.pipeline().addLast(new ObjectDecoder(1024, ClassResolvers.cacheDisabled(this.getClass().getClassLoader())));
						ch.pipeline().addLast(new ObjectEncoder());
						ch.pipeline().addLast(new SubReqClientHanler());
					}
				});
			ChannelFuture future = boots.connect(host, port).sync();
			future.channel().closeFuture().sync();
		}finally{
			group.shutdownGracefully();
		}
	}
	
	public static void main(String[] args) throws Exception{
		int port = 8080;
		if(args != null && args.length > 0){
			try{
				port = Integer.valueOf(args[0]);
			}catch (NumberFormatException e) {
				// TODO: handle exception
			}
		}
		new SubReqClient().connet(port, "127.0.0.1");
		
	}
}

class SubReqClientHanler extends ChannelHandlerAdapter{
	
	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception{
		System.out.println("Receive server response : [" + msg + "]");
	}
	
	@Override
	public void channelActive(ChannelHandlerContext ctx){
		for(int i = 0; i < 10; i++){
			ctx.write(subReq(i));
		}
		ctx.flush();
	}
	
	@Override
	public void channelReadComplete(ChannelHandlerContext ctx) throws Exception{
		ctx.flush();
	}
	
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception{
		cause.printStackTrace();
		ctx.close();
	}
	
	private SubscribReq subReq(int i){
		SubscribReq req = new SubscribReq();
		req.setAddress("深圳市软件产业基地");
		req.setPhneNumber("138XXXXXXXX");
		req.setProductName("Netty 权威指南");
		req.setSubReqId(i);
		req.setUserName("lee");
		return req;
	}
}