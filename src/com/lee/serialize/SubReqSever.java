package com.lee.serialize;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

public class SubReqSever {

	public void bind(int port) throws Exception{
		EventLoopGroup bossGroup = new NioEventLoopGroup();
		EventLoopGroup workerGroup = new NioEventLoopGroup();
		try{
			ServerBootstrap boots = new ServerBootstrap();
			boots.group(bossGroup, workerGroup)
				.channel(NioServerSocketChannel.class)
				.option(ChannelOption.SO_BACKLOG, 100)
				.handler(new LoggingHandler(LogLevel.INFO))
				.childHandler(new ChannelInitializer<SocketChannel>() {
					@Override
					public void initChannel(SocketChannel ch){
						ch.pipeline().addLast(new ObjectDecoder(1024 * 1024, ClassResolvers.weakCachingConcurrentResolver(this.getClass().getClassLoader())));
						ch.pipeline().addLast(new ObjectEncoder());
						ch.pipeline().addLast(new SubReqServerHandler());
					}
				});
			ChannelFuture future = boots.bind(port).sync();
			future.channel().closeFuture().sync();
		}finally{
			bossGroup.shutdownGracefully();
			workerGroup.shutdownGracefully();
		}
	}
	
	public static void main(String[] args) throws Exception{
		int port = 8080;
		if(args != null && args.length >0){
			try{
				port = Integer.valueOf(args[0]);
			}catch (Exception e) {
				// TODO: handle exception
			}
		}
		new SubReqSever().bind(port);
	}
}

class SubReqServerHandler extends ChannelHandlerAdapter{
	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg){
		SubscribReq req = (SubscribReq) msg;
		if("lee".equalsIgnoreCase(req.getUserName())){
			System.out.println("Server accept client subsricbe req : [" + req.toString() + "]");
			ctx.writeAndFlush(resp(req.getSubReqId()));  //将订购成功应答消息发送给client
		}
	}
	
	private SubscribResp resp(int subreqID){
		SubscribResp resp = new SubscribResp();
		resp.setSubReqID(subreqID);
		resp.setRespCode(0);
		resp.setDesc("Netty book order succeed, 3 days lateer, send to the designated address");
		return resp;
	}
	
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
		cause.printStackTrace();
		ctx.close();
	}
}