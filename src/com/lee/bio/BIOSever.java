package com.lee.bio;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public abstract class BIOSever {
	public static void main(String[] args) {
		ServerSocket server = null;
		try {
			server = new ServerSocket(8080);
			System.out.println("server start...");
			Socket socket = null;
			while (true) {
				socket = server.accept();
				new Thread(new BIOServerHandler(socket)).start();
			}
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
	}

}

class BIOServerHandler implements Runnable {
	private Socket socket;

	public BIOServerHandler() {

	}

	public BIOServerHandler(Socket socket) {
		this.socket = socket;
	}

	@Override
	public void run() {
		BufferedReader in = null;
		PrintWriter out = null;
		try {
			in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			out = new PrintWriter(socket.getOutputStream(), true);
			String body = null;
			while (true) {
				body = in.readLine();
				if (body == null) {
					break;
				}
				System.out.println("Server receive request content is :" + body);
				out.print("sever push data\n");
			}
		} catch (Exception e) {

		} finally {
			if (socket != null) {
				try {
					this.socket.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
				socket = null;
			}
			if (out != null) {
				try {
					out.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
				out = null;
			}
			if (in != null) {
				try {
					in.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
				in = null;
			}
		}
	}
}
