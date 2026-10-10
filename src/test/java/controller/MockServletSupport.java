package controller;

import jakarta.servlet.FilterChain;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

/**
 * Lightweight test fixture providing dynamic proxy mocks for Jakarta Servlet API.
 */
public class MockServletSupport {

    public static class TestSession {
        private final Map<String, Object> attributes = new HashMap<>();
        private boolean invalidated = false;

        public Object getAttribute(String name) {
            return attributes.get(name);
        }

        public void setAttribute(String name, Object value) {
            attributes.put(name, value);
        }

        public void removeAttribute(String name) {
            attributes.remove(name);
        }

        public void invalidate() {
            this.invalidated = true;
            this.attributes.clear();
        }

        public boolean isInvalidated() {
            return invalidated;
        }

        public HttpSession asProxy() {
            return (HttpSession) Proxy.newProxyInstance(
                    HttpSession.class.getClassLoader(),
                    new Class<?>[]{HttpSession.class},
                    (proxy, method, args) -> {
                        String name = method.getName();
                        if ("getAttribute".equals(name)) {
                            return getAttribute((String) args[0]);
                        } else if ("setAttribute".equals(name)) {
                            setAttribute((String) args[0], args[1]);
                            return null;
                        } else if ("removeAttribute".equals(name)) {
                            removeAttribute((String) args[0]);
                            return null;
                        } else if ("invalidate".equals(name)) {
                            invalidate();
                            return null;
                        } else if ("getId".equals(name)) {
                            return "TEST_SESSION_ID";
                        }
                        return defaultValue(method.getReturnType());
                    }
            );
        }
    }

    public static class TestRequest {
        public final Map<String, String> parameters = new HashMap<>();
        public final Map<String, Object> attributes = new HashMap<>();
        public TestSession session;
        public String contextPath = "";
        public String servletPath = "";
        public String forwardPath;
        public String method = "GET";

        public TestRequest(String servletPath) {
            this.servletPath = servletPath;
        }

        public void setParameter(String key, String value) {
            parameters.put(key, value);
        }

        public HttpServletRequest asProxy() {
            return (HttpServletRequest) Proxy.newProxyInstance(
                    HttpServletRequest.class.getClassLoader(),
                    new Class<?>[]{HttpServletRequest.class},
                    (proxy, method, args) -> {
                        String name = method.getName();
                        if ("getMethod".equals(name)) {
                            return this.method;
                        } else if ("getParameter".equals(name)) {
                            return parameters.get((String) args[0]);
                        } else if ("getAttribute".equals(name)) {
                            return attributes.get((String) args[0]);
                        } else if ("setAttribute".equals(name)) {
                            attributes.put((String) args[0], args[1]);
                            return null;
                        } else if ("removeAttribute".equals(name)) {
                            attributes.remove((String) args[0]);
                            return null;
                        } else if ("getContextPath".equals(name)) {
                            return contextPath;
                        } else if ("getServletPath".equals(name)) {
                            return servletPath;
                        } else if ("getRequestURI".equals(name)) {
                            return contextPath + servletPath;
                        } else if ("getSession".equals(name)) {
                            if (args == null || args.length == 0) {
                                if (session == null || session.isInvalidated()) {
                                    session = new TestSession();
                                }
                                return session.asProxy();
                            } else {
                                boolean create = (Boolean) args[0];
                                if (create && (session == null || session.isInvalidated())) {
                                    session = new TestSession();
                                }
                                return (session != null && !session.isInvalidated()) ? session.asProxy() : null;
                            }
                        } else if ("getRequestDispatcher".equals(name)) {
                            forwardPath = (String) args[0];
                            return (RequestDispatcher) Proxy.newProxyInstance(
                                    RequestDispatcher.class.getClassLoader(),
                                    new Class<?>[]{RequestDispatcher.class},
                                    (dispProxy, dispMethod, dispArgs) -> null
                            );
                        }
                        return defaultValue(method.getReturnType());
                    }
            );
        }
    }

    public static class TestResponse {
        public String redirectedUrl;
        public int status = 200;
        public String errorMessage;
        public final Map<String, String> headers = new HashMap<>();

        public HttpServletResponse asProxy() {
            return (HttpServletResponse) Proxy.newProxyInstance(
                    HttpServletResponse.class.getClassLoader(),
                    new Class<?>[]{HttpServletResponse.class},
                    (proxy, method, args) -> {
                        String name = method.getName();
                        if ("sendRedirect".equals(name)) {
                            redirectedUrl = (String) args[0];
                            return null;
                        } else if ("setStatus".equals(name)) {
                            status = (Integer) args[0];
                            return null;
                        } else if ("setHeader".equals(name)) {
                            headers.put((String) args[0], (String) args[1]);
                            return null;
                        } else if ("getHeader".equals(name)) {
                            return headers.get((String) args[0]);
                        } else if ("sendError".equals(name)) {
                            status = (Integer) args[0];
                            if (args.length > 1) {
                                errorMessage = (String) args[1];
                            }
                            return null;
                        }
                        return defaultValue(method.getReturnType());
                    }
            );
        }
    }

    public static class TestFilterChain implements FilterChain {
        public boolean invoked = false;

        @Override
        public void doFilter(ServletRequest request, ServletResponse response) {
            this.invoked = true;
        }
    }

    private static Object defaultValue(Class<?> returnType) {
        if (returnType == boolean.class) return false;
        if (returnType == int.class) return 0;
        if (returnType == long.class) return 0L;
        if (returnType == double.class) return 0.0;
        if (returnType == float.class) return 0.0f;
        return null;
    }
}
