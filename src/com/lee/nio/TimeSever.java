package com.lee.nio;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Date;
import java.util.Iterator;
import java.util.Set;

public class TimeSever {
	public static void main(String[] args) throws Exception {
		int port = 8080;
		if (args != null && args.length > 0) {
			try {
				port = Integer.valueOf(args[0]);
			} catch (Exception e) {
				System.out.println("the formatt of port is wrong");
			}
		}
		MultiplexerTimeServer timeSvr = new MultiplexerTimeServer(port);
		new Thread(timeSvr, "NIO-MutilplexerTimeSever-001").start();
	}
}

class MultiplexerTimeServer implements Runnable {
	private Selector selector;
	private ServerSocketChannel svrChannel;
	private volatile boolean stop; //volatile, 确保线程安全

	public MultiplexerTimeServer() {

	};

	public MultiplexerTimeServer(Integer port) {
		// 初始化ServerSokectChannel
		try {
			selector = Selector.open();
			svrChannel = ServerSocketChannel.open();
			svrChannel.configureBlocking(false);
			svrChannel.socket().bind(new InetSocketAddress(port), 1024); //绑定IP和端口
			svrChannel.register(selector, SelectionKey.OP_ACCEPT); //注册channel到Selector, 兴趣点为OP_ACCEPT
			System.out.println("The time server is start in port : " + port);
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	};

	@Override
	public void run() {
		while (!stop) {
			try {
				selector.select(1000);
				Set<SelectionKey> slctKeySet = selector.selectedKeys();
				Iterator<SelectionKey> iterSlctKey = slctKeySet.iterator();
				SelectionKey slctKey = null;
				//轮询Channel
				while (iterSlctKey.hasNext()) {
					slctKey = iterSlctKey.next();
					iterSlctKey.remove();
					try {
						handleInput(slctKey);
					} catch (Exception e) {
						if(slctKey != null){
							slctKey.cancel();
						}
						if(slctKey.channel() != null){
							slctKey.channel().close();
						}
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			} 
		}
		if (selector != null) {
			try {
				selector.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private void handleInput(SelectionKey key) throws Exception {
		if (key.isValid()) {
			if (key.isAcceptable()) {
				ServerSocketChannel channel = (ServerSocketChannel) key.channel();
				SocketChannel sc = channel.accept();
				sc.configureBlocking(false);
				sc.register(selector, SelectionKey.OP_READ);
			}
			if (key.isReadable()) {
				SocketChannel sc = (SocketChannel) key.channel();
				ByteBuffer buffer = ByteBuffer.allocate(1024);
				int readBytes = sc.read(buffer);
				if (readBytes > 0) {
					buffer.flip();
					byte[] bytes = new byte[buffer.remaining()];
					buffer.get(bytes);
					String body = new String(bytes, "UTF-8");
					System.out.println("The time server receive order : " + body);
					String currentTime = "QUERY TIME ORDER".equalsIgnoreCase(body)
							? new Date(System.currentTimeMillis()).toString() : "BAD ORDER";
					doWrite(sc, currentTime);
				} else if (readBytes < 0) {
					key.cancel();
					sc.close();
				} else {
					// 读到0字节，忽略
				}
			}
		}

	}

	private void doWrite(SocketChannel sc, String responseStr) throws Exception {
		if (responseStr != null && responseStr.trim().length() > 0) {
			byte[] bytes = responseStr.getBytes();
			ByteBuffer writeBuffer = ByteBuffer.allocate(bytes.length);
			writeBuffer.put(bytes);
			writeBuffer.flip();
			sc.write(writeBuffer);
		}
	}
}
