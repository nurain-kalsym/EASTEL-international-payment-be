package com.kalsym.ekedai.utility;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import java.util.Date;
import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

@Getter
@Setter
@JsonInclude(Include.NON_NULL)
public class HttpResponse {

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Kuala_Lumpur")
    private Date timestamp;

    private int status;
    private String message;
    private String error; 
    private Object data;
    private String path;
    private String errorCode;
    private int code;

    // Constructors
    public HttpResponse() {
        this.timestamp = DateTimeUtil.currentTimestamp();
    }

    public HttpResponse(String requestUri) {
        this.timestamp = DateTimeUtil.currentTimestamp();
        this.path = requestUri;
    }

    /**
     * Sets status and message from HttpStatus
     */
    public void setStatus(HttpStatus status) {
        this.status = status.value();
        this.message = status.getReasonPhrase();
    }

    public void setStatus(HttpStatus status, String message) {
        this.status = status.value();
        this.message = message;
    }

    public void setStatus(HttpStatus status, String message, String errorCode) {
        this.status = status.value();
        this.message = message;
        this.errorCode = errorCode;
    }

    public void setStatus(HttpStatusCode status) {
        this.status = status.value();
        this.message = status.toString();
    }

}

