package com.lee.aio;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.CountDownLatch;

public class TimeClient {
	public static void main(String[] args){
		Integer port = 8080;
		if(args != null && args.length > 0){
			try {
				port = Integer.valueOf(args[0]);
			} catch (Exception e) {
				// TODO: handle exception
			}
		}
		new Thread(new AsyncTimeClientHandler("127.0.0.1", port), "AIO-AsynchronousTimeClientHandler-001").start();
	}
}

class AsyncTimeClientHandler implements Runnable, CompletionHandler<Void, AsyncTimeClientHandler>{
	
	private AsynchronousSocketChannel client;
	private String host;
	private Integer port;
	private CountDownLatch latch;
	public AsyncTimeClientHandler() {
		// TODO Auto-generated constructor stub
	}
	public AsyncTimeClientHandler(String host, Integer port) {
		this.host = host;
		this.port = port;
		try {
			client = AsynchronousSocketChannel.open();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void run(){
		latch = new CountDownLatch(1);
		client.connect(new InetSocketAddress(host, port), this, this);
		try {
			latch.await();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		try {
			client.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	@Override
	public void completed(Void result, AsyncTimeClientHandler attachment) {
		byte[] req = "QUERY TIME ORDER".getBytes();
		ByteBuffer writeBuffer = ByteBuffer.allocate(req.length);
		writeBuffer.put(req);
		writeBuffer.flip();
		client.write(writeBuffer, writeBuffer, new CompletionHandler<Integer, ByteBuffer>() {

			@Override
			public void completed(Integer result, ByteBuffer buffer) {
				if(buffer.hasRemaining()){
					client.write(buffer, buffer, this);
				}else{
					ByteBuffer readBuffer = ByteBuffer.allocate(1024);
					client.read(readBuffer, readBuffer, new CompletionHandler<Integer, ByteBuffer>() {

						@Override
						public void completed(Integer result, ByteBuffer readbuBuffer) {
							readbuBuffer.flip();
							byte[] bytes = new byte[buffer.remaining()];
							readbuBuffer.get(bytes);
							String body;
							try {
								body = new String(bytes, "UTF-8");
								System.out.println("Now is : " + body);
								latch.countDown();
							} catch (UnsupportedEncodingException e) {
								e.printStackTrace();
							}
							
						}

						@Override
						public void failed(Throwable exc, ByteBuffer attachment) {
							try {
								client.close();
								latch.countDown();
							} catch (IOException e) {
								e.printStackTrace();
							}
						}
					});
				}
				
			}

			@Override
			public void failed(Throwable paramThrowable, ByteBuffer paramA) {
				try {
					client.close();
					latch.countDown();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			
		});
	}
	@Override
	public void failed(Throwable exc, AsyncTimeClientHandler attachment) {
		// TODO Auto-generated method stub
		
	}
}
