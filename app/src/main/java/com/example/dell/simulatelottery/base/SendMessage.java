package com.example.dell.simulatelottery.base;

public class SendMessage {

	private String 				user_id  	= "1";
	private String 				mac		 	= "1001";

    public SendMessage() {
	}

	private static SendMessage instance = new SendMessage();

	public static SendMessage getInstance() {
		return instance;
	}

	public String getUser_id() {
		return user_id;
	}

	public String getMac() {
		return mac;
	}

	public static void setInstance(SendMessage instance) {
		SendMessage.instance = instance;
	}

	public void setMac(String mac) {
		this.mac = mac;
	}

	public void setUser_id(String user_id) {
		this.user_id = user_id;
	}


}