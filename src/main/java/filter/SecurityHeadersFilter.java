package filter;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.UUID;

/**
 * Filter enforcing HTTP security response headers and session-based CSRF token issuance.
 * 
 * Mitigations:
 * - MIME-sniffing: X-Content-Type-Options: nosniff
 * - Clickjacking: X-Frame-Options: SAMEORIGIN / frame-ancestors 'self'
 * - Legacy XSS Filters: X-XSS-Protection: 1; mode=block
 * - Information Leakage: Referrer-Policy: strict-origin-when-cross-origin
 * - Content Injection: Content-Security-Policy
 * - Cross-Site Request Forgery (CSRF): Generates and binds unique session CSRF token
 */
@WebFilter(filterName = "SecurityHeadersFilter", urlPatterns = {"/*"})
public class SecurityHeadersFilter implements Filter {

    public static final String CSRF_TOKEN_ATTR = "csrfToken";
    public static final String CSRF_PARAM_NAME = "csrfToken";
    public static final String CSRF_HEADER_NAME = "X-CSRF-Token";

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {}

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // 1. Set Defensive HTTP Security Response Headers
        httpResponse.setHeader("X-Content-Type-Options", "nosniff");
        httpResponse.setHeader("X-Frame-Options", "SAMEORIGIN");
        httpResponse.setHeader("X-XSS-Protection", "1; mode=block");
        httpResponse.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
        httpResponse.setHeader("Content-Security-Policy",
                "default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline'; img-src 'self' data:; font-src 'self'; connect-src 'self'; frame-ancestors 'self';");

        // 2. Ensure active sessions have a unique cryptographically random CSRF token
        HttpSession session = httpRequest.getSession(false);
        if (session != null) {
            String token = (String) session.getAttribute(CSRF_TOKEN_ATTR);
            if (token == null || token.isBlank()) {
                token = UUID.randomUUID().toString();
                session.setAttribute(CSRF_TOKEN_ATTR, token);
            }
            httpRequest.setAttribute(CSRF_TOKEN_ATTR, token);

            // 3. Optional CSRF Validation on Authenticated Mutating POST requests
            // Public endpoints (login, register, logout) manage their own authentication flow
            String path = httpRequest.getRequestURI().substring(httpRequest.getContextPath().length());
            if ("POST".equalsIgnoreCase(httpRequest.getMethod()) && !isPublicAuthPath(path)) {
                String submittedToken = httpRequest.getParameter(CSRF_PARAM_NAME);
                if (submittedToken == null || submittedToken.isBlank()) {
                    submittedToken = httpRequest.getHeader(CSRF_HEADER_NAME);
                }

                // If a CSRF token parameter is provided in the request, validate it strictly
                if (submittedToken != null && !token.equals(submittedToken)) {
                    httpResponse.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    httpRequest.setAttribute("errorCode", 403);
                    httpRequest.setAttribute("errorMessage", "Invalid CSRF security token. Request denied.");
                    httpRequest.getRequestDispatcher("/WEB-INF/views/error.jsp").forward(request, response);
                    return;
                }
            }
        }

        chain.doFilter(request, response);
    }

    private boolean isPublicAuthPath(String path) {
        return path.equals("/login")
                || path.equals("/register")
                || path.equals("/logout")
                || path.startsWith("/login?")
                || path.startsWith("/register?")
                || path.startsWith("/logout?");
    }

    @Override
    public void destroy() {}
}
