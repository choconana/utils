package com.chinaums.spread.common.crypto.filter;

import com.chinaums.spread.common.crypto.interceptor.CryptoRequestWrapper;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

public class CryptoFilter implements Filter {
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        CryptoRequestWrapper cryptoRequestWrapper = new CryptoRequestWrapper((HttpServletRequest) servletRequest);
        filterChain.doFilter(cryptoRequestWrapper, servletResponse);
    }

    @Override
    public void destroy() {

    }
}
