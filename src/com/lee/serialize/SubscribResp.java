package com.lee.serialize;

import java.io.Serializable;

public class SubscribResp implements Serializable{
	
	private static final long serialVersionUID = 7272873112575909411L;

	private Integer subReqID;
	
	private Integer respCode;
	
	private String desc;

	public Integer getSubReqID() {
		return subReqID;
	}

	public void setSubReqID(Integer subReqID) {
		this.subReqID = subReqID;
	}

	public Integer getRespCode() {
		return respCode;
	}

	public void setRespCode(Integer respCode) {
		this.respCode = respCode;
	}

	public String getDesc() {
		return desc;
	}

	public void setDesc(String desc) {
		this.desc = desc;
	}

	@Override
	public String toString() {
		return "SubscribResp [subReqID=" + subReqID + ", respCode=" + respCode + ", desc=" + desc + "]";
	}
	
	
}
