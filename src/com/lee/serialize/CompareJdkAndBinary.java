package com.lee.serialize;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.ByteBuffer;

public class CompareJdkAndBinary {
	public static void main(String[] args) throws Exception{
		UserInfo userInfo = new UserInfo();
		userInfo.setUserName("welecome to netty.");
		userInfo.setUserId(200);
		System.out.println(serializeByJdk(userInfo).length);
		System.out.println(serializeByBinary(userInfo).length);
	}

	public static byte[] serializeByJdk(UserInfo userInfo) throws Exception {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(bos);
		try {
			oos.writeObject(userInfo);
			oos.flush();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			oos.close();
		}

		return bos.toByteArray();
	}

	public static byte[] serializeByBinary(UserInfo userInfo) throws Exception{
		ByteBuffer buffer = ByteBuffer.allocate(1024);
		byte[] userName = userInfo.getUserName().getBytes();
		buffer.putInt(userName.length);
		buffer.put(userName);
		buffer.putInt(userInfo.getUserId());
		buffer.flip();
		userName = null;
		byte[] result = new byte[buffer.remaining()];
		buffer.get(result);
		return result;
	}

}

class UserInfo implements Serializable {

	private static final long serialVersionUID = 6837963349012377025L;

	private String userName;

	private int userId;

	public void setUserId(int userId) {
		this.userId = userId;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public int getUserId() {
		return userId;
	}

	public String getUserName() {
		return userName;
	}

}