package com.luke.playhub.interceptor;

import com.luke.playhub.context.UserContext;
import jakarta.annotation.Nonnull;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.HandlerInterceptor;

@Slf4j
public class UserInterceptor implements HandlerInterceptor {

    /**
     * 请求处理前执行：提取 userId 并存入 ThreadLocal
     */
    @Override
    public boolean preHandle(
            @Nonnull HttpServletRequest request,
            @Nonnull HttpServletResponse response,
            @Nonnull Object handler) throws Exception {
        // 从请求中获取 userId 参数（支持 GET 请求参数、POST 表单参数）
        String userIdStr = request.getParameter("userId");
        
        if (userIdStr != null && !userIdStr.isEmpty()) {
            try {
                // 转换为 Long 类型（根据实际类型调整，如 String 则直接存储）
                Long userId = Long.parseLong(userIdStr);
                UserContext.setUserId(userId); // 存入 ThreadLocal
                log.info("UserInterceptor 设置 userId: {}", userId);
            } catch (NumberFormatException e) {
                // 若参数格式错误，可根据业务需求处理（如返回错误、忽略等）
                response.getWriter().write("userId 参数格式错误");
                return false; // 阻止请求继续处理
            }
        }
        
        // 即使没有 userId，也放行（根据业务需求决定是否必须有 userId）
        return true;
    }

    /**
     * 请求处理后执行：清除 ThreadLocal，避免内存泄漏
     */
    @Override
    public void afterCompletion(
            @Nonnull HttpServletRequest request, @Nonnull HttpServletResponse response, @Nonnull Object handler, Exception ex) {
        UserContext.removeUserId(); // 必须清除，否则线程池复用会导致数据混乱
    }
}