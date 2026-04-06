package com.kalsym.ekedai.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@ToString
@Getter
@Setter
public class ChangeUserProfile {

	private String email;

	private String phoneNumber;

	private String fullName;

	private String channel;

	private String nationality;

}
