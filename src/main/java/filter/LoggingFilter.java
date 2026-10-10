package filter;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * Filter that logs incoming HTTP requests, response timing, and paths.
 */
@WebFilter(filterName = "LoggingFilter", urlPatterns = {"/*"})
public class LoggingFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {}

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        long startTime = System.currentTimeMillis();

        String uri = httpRequest.getRequestURI();
        String method = httpRequest.getMethod();

        try {
            chain.doFilter(request, response);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            if (!uri.contains("/css/") && !uri.contains("/js/") && !uri.contains("/images/")) {
                System.out.printf("[HTTP %s] %s (Time: %d ms)%n", method, uri, duration);
            }
        }
    }

    @Override
    public void destroy() {}
}
