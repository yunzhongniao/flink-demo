package org.yunzhong.cep.monitoring.test;

import java.io.Serializable;

public class OrderEvent implements Serializable{
	private static final long serialVersionUID = -4909746807392757784L;
	private String userId;
	private String type;

	public OrderEvent(String userId, String type) {
		this.userId = userId;
		this.type = type;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}
}
