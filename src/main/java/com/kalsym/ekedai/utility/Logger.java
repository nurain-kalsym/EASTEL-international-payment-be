package com.kalsym.ekedai.utility;

import org.slf4j.LoggerFactory;

public class Logger {

    public static final org.slf4j.Logger application = LoggerFactory.getLogger("application");
    
    public static String pattern = "[v{}][{}] {}{}";
}
