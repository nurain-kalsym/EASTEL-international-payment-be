package com.kalsym.ekedai.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

@Component
@Order(1)
public class CustomHeaderFilter implements Filter {
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z");
        dateFormat.setTimeZone(TimeZone.getTimeZone("Asia/Kuala_Lumpur"));
        Date now = new Date();
        httpResponse.setHeader("Date", dateFormat.format(now));
        chain.doFilter(request, response);
    }

    // Implement other methods of the Filter interface (init, destroy) if needed
}
