package com.lee.serialize;

import java.io.Serializable;

public class SubscribReq implements Serializable{
	
	private static final long serialVersionUID = -2696163106133384840L;

	private Integer subReqId;
	
	private String userName;
	
	private String productName;
	
	private String phneNumber;
	
	private String address;

	public Integer getSubReqId() {
		return subReqId;
	}

	public void setSubReqId(Integer subReqId) {
		this.subReqId = subReqId;
	}

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

    public void setProductName(String productName) {
		this.productName = productName;
	}
    
    public String getProductName() {
		return productName;
	}

	public String getPhneNumber() {
		return phneNumber;
	}

	public void setPhneNumber(String phneNumber) {
		this.phneNumber = phneNumber;
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	@Override
	public String toString() {
		return "SubscribReq [subReqId=" + subReqId + ", userName=" + userName + ", productName=" + productName
				+ ", phneNumber=" + phneNumber + ", address=" + address + "]";
	}

	
}
