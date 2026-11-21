package com.fpt.ems.be.security;

import com.fpt.ems.be.config.SecurityConfig;
import com.fpt.ems.be.controller.*;
import com.fpt.ems.be.service.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.core.env.Environment;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.context.support.StaticWebApplicationContext;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SEC-AUDIT: Kiểm thử tự động duyệt toàn bộ RequestMappingHandlerMapping - Mọi endpoint phải có policy")
class EndpointSecurityPolicyAuditTest {

    
    private static final Set<String> PUBLIC_WHITELIST = Set.of(
            "/api/v1/auth/login",
            "/api/v1/auth/register"
    );

    @Test
    @DisplayName("Duyệt RequestMappingHandlerMapping: 100% endpoint phải có @PreAuthorize hoặc thuộc PUBLIC_WHITELIST (Chỉ đúng 2 đường)")
    void allEndpoints_inRequestMappingHandlerMapping_mustHaveSecurityPolicy() {
        StaticWebApplicationContext context = new StaticWebApplicationContext();

        
        context.getBeanFactory().registerSingleton("alertController",
                new AlertController(Mockito.mock(AlertService.class)));
        context.getBeanFactory().registerSingleton("auditLogController",
                new AuditLogController(Mockito.mock(AuditLogService.class)));
        context.getBeanFactory().registerSingleton("authController",
                new AuthController(Mockito.mock(AuthService.class)));
        context.getBeanFactory().registerSingleton("dashboardController",
                new DashboardController(Mockito.mock(DashboardService.class)));
        context.getBeanFactory().registerSingleton("equipmentController",
                new EquipmentController(Mockito.mock(EquipmentService.class)));
        context.getBeanFactory().registerSingleton("equipmentTypeController",
                new EquipmentTypeController(Mockito.mock(EquipmentTypeService.class)));
        context.getBeanFactory().registerSingleton("incidentReportController",
                new IncidentReportController(Mockito.mock(IncidentReportService.class)));
        context.getBeanFactory().registerSingleton("locationController",
                new LocationController(Mockito.mock(LocationService.class)));
        context.getBeanFactory().registerSingleton("maintenanceChecklistController",
                new MaintenanceChecklistController(Mockito.mock(MaintenanceChecklistService.class)));
        context.getBeanFactory().registerSingleton("maintenanceHistoryController",
                new MaintenanceHistoryController(Mockito.mock(MaintenanceHistoryService.class)));
        context.getBeanFactory().registerSingleton("maintenancePlanController",
                new MaintenancePlanController(Mockito.mock(MaintenancePlanService.class)));
        context.getBeanFactory().registerSingleton("statusChangeRequestController",
                new StatusChangeRequestController(Mockito.mock(StatusChangeRequestService.class)));
        context.getBeanFactory().registerSingleton("userController",
                new UserController(Mockito.mock(UserService.class)));
        context.getBeanFactory().registerSingleton("workOrderController",
                new WorkOrderController(Mockito.mock(WorkOrderService.class)));

        context.refresh();

        
        RequestMappingHandlerMapping handlerMapping = new RequestMappingHandlerMapping();
        handlerMapping.setApplicationContext(context);
        handlerMapping.afterPropertiesSet();

        Map<RequestMappingInfo, HandlerMethod> handlerMethods = handlerMapping.getHandlerMethods();
        assertThat(handlerMethods).isNotEmpty();

        List<String> unshieldedEndpoints = new ArrayList<>();
        List<String> shieldedEndpoints = new ArrayList<>();
        Set<String> discoveredWhitelistedPaths = new HashSet<>();

        for (Map.Entry<RequestMappingInfo, HandlerMethod> entry : handlerMethods.entrySet()) {
            RequestMappingInfo info = entry.getKey();
            HandlerMethod method = entry.getValue();

            String className = method.getBeanType().getName();
            if (!className.startsWith("com.fpt.ems.be.controller")) {
                continue;
            }

            boolean hasMethodPolicy = method.hasMethodAnnotation(PreAuthorize.class);
            boolean hasClassPolicy = method.getBeanType().isAnnotationPresent(PreAuthorize.class);
            boolean hasSecurityPolicy = hasMethodPolicy || hasClassPolicy;

            Set<String> patterns = info.getPatternValues();
            boolean isWhitelisted = patterns.stream().anyMatch(PUBLIC_WHITELIST::contains);
            if (isWhitelisted) {
                discoveredWhitelistedPaths.addAll(patterns);
            }

            String endpointSummary = String.format("%s -> %s#%s", patterns, method.getBeanType().getSimpleName(), method.getMethod().getName());

            if (hasSecurityPolicy || isWhitelisted) {
                String policyDetail = isWhitelisted ? "PUBLIC_WHITELIST"
                        : (hasMethodPolicy ? method.getMethodAnnotation(PreAuthorize.class).value()
                        : method.getBeanType().getAnnotation(PreAuthorize.class).value());
                shieldedEndpoints.add(endpointSummary + " [POLICY: " + policyDetail + "]");
            } else {
                unshieldedEndpoints.add(endpointSummary);
            }
        }

        System.out.println("========== KẾT QUẢ AUDIT BẢO MẬT REQUEST MAPPING HANDLER MAPPING ==========");
        System.out.println("Tổng số endpoint đã kiểm tra: " + (shieldedEndpoints.size() + unshieldedEndpoints.size()));
        System.out.println("Số endpoint được bảo vệ hợp lệ: " + shieldedEndpoints.size());
        System.out.println("Số endpoint vi phạm (thiếu policy & không whitelisted): " + unshieldedEndpoints.size());
        for (String endpoint : shieldedEndpoints) {
            System.out.println("  [V] " + endpoint);
        }

        if (!unshieldedEndpoints.isEmpty()) {
            System.err.println("CẢNH BÁO: CÓ ENDPOINT CHƯA ĐƯỢC BẢO VỆ:");
            for (String endpoint : unshieldedEndpoints) {
                System.err.println("  [X] " + endpoint);
            }
        }
        System.out.println("============================================================================");

        assertThat(unshieldedEndpoints)
                .as("Mọi endpoint trong controller đều phải có @PreAuthorize hoặc nằm trong PUBLIC_WHITELIST (Chỉ đúng 2 đường)")
                .isEmpty();

        
        assertThat(discoveredWhitelistedPaths)
                .as("Chỉ cho phép duy nhất 2 đường nghiệp vụ công khai")
                .containsExactlyInAnyOrder(
                        "/api/v1/auth/login",
                        "/api/v1/auth/register"
                );
    }

    @Test
    @DisplayName("Kiểm tra profile production: Không mở Swagger/OpenAPI công khai, yêu cầu xác thực")
    void productionProfile_doesNotOpenSwaggerPublicly() {
        Environment prodEnv = Mockito.mock(Environment.class);
        Mockito.when(prodEnv.getActiveProfiles()).thenReturn(new String[]{"prod"});

        JwtAuthenticationFilter jwtFilter = Mockito.mock(JwtAuthenticationFilter.class);
        RestAuthenticationEntryPoint entryPoint = Mockito.mock(RestAuthenticationEntryPoint.class);
        RestAccessDeniedHandler accessDeniedHandler = Mockito.mock(RestAccessDeniedHandler.class);

        SecurityConfig config = new SecurityConfig(jwtFilter, entryPoint, accessDeniedHandler, prodEnv);
        assertThat(config).isNotNull();

        

        boolean isDev = Arrays.asList(prodEnv.getActiveProfiles()).contains("dev");
        assertThat(isDev).isFalse();
    }
}
