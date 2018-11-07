package com.lee;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class SocketChannelTest {
	public static void main(String[] args){
		try{
			SocketChannel channel = SocketChannel.open();
			channel.connect(new InetSocketAddress("http://jenkov.com", 80));
			ByteBuffer buffer = ByteBuffer.allocate(48);
			int readCnt = channel.read(buffer);
			while(readCnt != -1){
				buffer.flip();
				while(buffer.hasRemaining()){
					System.out.print((char)buffer.get());
				}
				buffer.clear();
				readCnt = channel.read(buffer);
			}

		}catch (Exception e) {
			// TODO: handle exception
		}
		
	}
}
