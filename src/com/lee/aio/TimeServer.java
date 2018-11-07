package com.lee.aio;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.Date;
import java.util.concurrent.CountDownLatch;

public class TimeServer {
	public static void main(String[] args){
		Integer port = 8080;
		if(args != null && args.length > 0){
			try{
				port = Integer.valueOf(args[0]);
			}catch (Exception e) {
				System.out.println("port format type error.");
			}
		}
		AsyncTimeServerHandler atsh = new AsyncTimeServerHandler(port);
		new Thread(atsh, "AIO-AsyncTimeServerHandle-001").start();
	}
	
}
class AsyncTimeServerHandler implements Runnable{
	CountDownLatch latch;
	AsynchronousServerSocketChannel asynchronousServerSocketChannel;
	AsyncTimeServerHandler(){
		
	}
	public AsyncTimeServerHandler(Integer port){
		try{
			asynchronousServerSocketChannel = AsynchronousServerSocketChannel.open();
			asynchronousServerSocketChannel.bind(new InetSocketAddress(port));
			System.out.println("The time server is start in port : " + port);
		}catch (Exception e) {
			e.printStackTrace();
		}
	}
	@Override
	public void run(){
		latch = new CountDownLatch(1);
		doAccept();
		try{
			latch.await();
		}catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	public void doAccept(){
		asynchronousServerSocketChannel.accept(this, new AcceptCompletionHandler());
	}
}

class AcceptCompletionHandler implements CompletionHandler<AsynchronousSocketChannel, AsyncTimeServerHandler>{

	@Override
	public void completed(AsynchronousSocketChannel result, AsyncTimeServerHandler attachment) {
		attachment.asynchronousServerSocketChannel.accept(attachment, this);
		ByteBuffer buffer = ByteBuffer.allocate(1024);
		result.read(buffer, buffer, new ReadCompletionHandler(result));
		
	}

	@Override
	public void failed(Throwable exc, AsyncTimeServerHandler attachment) {
		exc.printStackTrace();
		attachment.latch.countDown();
	}

}

class ReadCompletionHandler implements CompletionHandler<Integer, ByteBuffer>{
	
	private AsynchronousSocketChannel channel;
	
    public ReadCompletionHandler(AsynchronousSocketChannel channel) {
		if(this.channel == null){
			this.channel = channel;
		}
	}
    
	@Override
	public void completed(Integer result, ByteBuffer attachment) {
		attachment.flip();
		byte[] body = new byte[attachment.remaining()];
		attachment.get(body);
		try{
			String req = new String(body, "UTF-8");
			System.out.println("The time server receive order : " + req);
			String currentTime = "QUERY TIME ORDER".equals(req) ? new Date(System.currentTimeMillis()).toString() : "BAD ORDER";
			doWrite(currentTime);
		}catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
	private void doWrite(String currentTime){
		if(currentTime != null && currentTime.length() > 0){
			byte[] bytes = currentTime.getBytes();
			ByteBuffer writeBuffer = ByteBuffer.allocate(bytes.length);
			writeBuffer.put(bytes);
			writeBuffer.flip();
			channel.write(writeBuffer, writeBuffer, new CompletionHandler<Integer, ByteBuffer>() {
				@Override
				public void completed(Integer result, ByteBuffer buffer){
					if(buffer.hasRemaining()){
						channel.write(buffer, buffer, this);
					}
				}
				
				@Override
				public void failed(Throwable exc, ByteBuffer attachment){
					try{
						channel.close();
					}catch (Exception e) {
						e.printStackTrace();
					}
				}
			});
		}
	}
	
	@Override
	public void failed(Throwable paramThrowable, ByteBuffer paramA) {
		try{
			channel.close();
		}catch (Exception e) {
			e.printStackTrace();
		}
	}
	
}



