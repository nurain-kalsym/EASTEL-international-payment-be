package com.kalsym.ekedai.model.dao;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProfileServiceResponse {
    public ProfileServiceResponse(String message, String error, int code, Object data, Object context) {
        this.message = message;
        this.error = error;
        this.code = code;
        this.data = data;
        this.context = context;
    }

    // No-argument constructor (default constructor)
    public ProfileServiceResponse() {
    }

    private String message;
    private String error;
    private int code;
    private Object data;
    private Object context;

}
