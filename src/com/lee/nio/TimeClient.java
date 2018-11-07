package com.lee.nio;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

public class TimeClient {
	public static void main(String[] args){
		int port = 8080;
		if(args != null && args.length > 0){
			try{
				port = Integer.valueOf(args[0]);
			}catch (Exception e) {
				System.out.println("port type convert error.");
				e.printStackTrace();
			}
		}
		new Thread(new TimeClientHandle("127.0.0.1", port), "TimeClient-001").start();
	}
}

class TimeClientHandle implements Runnable{
	private String host;
	private Integer port;
	private SocketChannel sc;
	private Selector selector;
	private volatile boolean stop;
	public TimeClientHandle(){
		
	}
	public TimeClientHandle(String host, Integer port){
		this.host = (host != null ? host : "127.0.0.1");
		this.port = port;
		try{
			this.selector = Selector.open();
			this.sc = SocketChannel.open();
			this.sc.configureBlocking(false);
		}catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
	public void run(){
		try{
			doConnet();
		}catch (Exception e) {
			e.printStackTrace();
		}
		while(!stop){
			try{
				selector.select(1000);
				Set<SelectionKey> slctKeySet = selector.selectedKeys();
				Iterator<SelectionKey> iterSlectKey = slctKeySet.iterator();
				SelectionKey key = null;
				while(iterSlectKey.hasNext()){
					key = iterSlectKey.next();
					iterSlectKey.remove();
					try{
						handleInput(key);
					}catch (Exception e) {
						if(key != null){
							key.cancel();
							if(key.channel() != null){
								key.channel().close();
							}
						}
					}
				}
			}catch (Exception e) {
				e.printStackTrace();
				System.exit(1);
			}
		}
		if(selector != null){
			try{
				selector.close();
			}catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	private void handleInput(SelectionKey key) throws Exception{
		if(key.isValid()){
			SocketChannel sc = (SocketChannel) key.channel();
			if(key.isConnectable()){
				if(sc.finishConnect()){
					sc.register(selector, SelectionKey.OP_READ);
					doWrite(sc);
				}else{
					System.out.println("connet fail, proess exit.");
					System.exit(1);
				}
			}
			if(key.isReadable()){
				ByteBuffer readBuffer = ByteBuffer.allocate(1024);
				int readBytes = sc.read(readBuffer);
				if(readBytes > 0){
					readBuffer.flip();
					byte[] bytes = new byte[readBuffer.remaining()];
					readBuffer.get(bytes);
					String body = new String(bytes, "UTF-8");
					System.out.println("Now is : " + body);
					this.stop = true;
				}else if(readBytes < 0){
					key.cancel();
					sc.close();
				}else{
					;// read zero byte, ignore.
				}
			}
		}
	}
	
	private void doConnet() throws Exception {
		if(sc.connect(new InetSocketAddress(host, port))){
			sc.register(selector, SelectionKey.OP_READ);
			doWrite(sc);
		}else{
			sc.register(selector, SelectionKey.OP_CONNECT);
		}
		
	}
	private void doWrite(SocketChannel sc) throws Exception{
		byte[] req = "QUERY TIME ORDER".getBytes();
		ByteBuffer writeBuffer = ByteBuffer.allocate(req.length);
		writeBuffer.put(req);
		writeBuffer.flip();
		sc.write(writeBuffer);
		if(!writeBuffer.hasRemaining()){
			System.out.println("Send order 2 server succeed.");
		}
	}
}