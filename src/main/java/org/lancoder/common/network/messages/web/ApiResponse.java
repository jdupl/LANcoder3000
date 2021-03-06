package org.lancoder.common.network.messages.web;

import java.io.Serializable;

public class ApiResponse implements Serializable {

	private static final long serialVersionUID = 2890084482426925083L;

	private boolean success;
	private String message;

	public ApiResponse(boolean success) {
		this.success = success;
		this.message = "";
	}

	public ApiResponse(boolean success, String message) {
		this.success = success;
		this.message = message;
	}

	public boolean isSuccess() {
		return success;
	}

	public void setSuccess(boolean success) {
		this.success = success;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

}
