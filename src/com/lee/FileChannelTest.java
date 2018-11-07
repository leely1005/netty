package com.lee;

import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

public class FileChannelTest {
	public static void main(String[] args) {
		String filaName = "f:/test-file/a.txt";
		readFile(filaName);
		writeFile(filaName);
	}
	private static void writeFile(String fileName) {
		try{
			RandomAccessFile file = new RandomAccessFile(fileName, "rw");
			FileChannel fileChannel = file.getChannel();
			fileChannel.position(-1);
			ByteBuffer buffer = ByteBuffer.allocate(48);
			String content = "write file by channel";
			buffer.put(content.getBytes());
			buffer.flip();
			while(buffer.hasRemaining()){
				fileChannel.write(buffer);
			}
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	public static void readFile(String filaName){
		try {
			RandomAccessFile randomAccessFile = new RandomAccessFile("f:/test-file/a.txt", "rw");
			FileChannel fileChannel = randomAccessFile.getChannel();
			ByteBuffer buffer = ByteBuffer.allocate(1);
			int bytesRead = fileChannel.read(buffer);
			while (bytesRead != -1) {
				buffer.flip();
				while (buffer.hasRemaining()) {
					System.out.print((char)buffer.get());
				}
				
				buffer.clear();
				bytesRead = fileChannel.read(buffer);
			}
		} catch (Exception e) {
			// TODO: handle exception'
			e.printStackTrace();
		}
	}
}
