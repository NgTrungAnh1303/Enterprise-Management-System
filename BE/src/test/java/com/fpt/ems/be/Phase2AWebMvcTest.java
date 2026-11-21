package com.fpt.ems.be;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fpt.ems.be.controller.*;
import com.fpt.ems.be.dto.plan.MaintenancePlanRequest;
import com.fpt.ems.be.exception.BusinessException;
import com.fpt.ems.be.exception.GlobalExceptionHandler;
import com.fpt.ems.be.security.UserPrincipal;
import com.fpt.ems.be.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
public class Phase2AWebMvcTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private EquipmentService equipmentService;
    @Mock
    private LocationService locationService;
    @Mock
    private EquipmentTypeService equipmentTypeService;
    @Mock
    private MaintenancePlanService maintenancePlanService;
    @Mock
    private MaintenanceChecklistService maintenanceChecklistService;
    @Mock
    private WorkOrderService workOrderService;
    @Mock
    private AuditService auditService;

    // Custom resolver to mock @AuthenticationPrincipal
    private UserPrincipal currentPrincipal;

    @BeforeEach
    void setUp() {
        objectMapper = new com.fpt.ems.be.config.JacksonConfig().objectMapper();
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter(objectMapper);

        HandlerMethodArgumentResolver authPrincipalResolver = new HandlerMethodArgumentResolver() {
            @Override
            public boolean supportsParameter(MethodParameter parameter) {
                return parameter.hasParameterAnnotation(AuthenticationPrincipal.class);
            }

            @Override
            public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                          NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
                return currentPrincipal;
            }
        };

        mockMvc = MockMvcBuilders.standaloneSetup(
                        new EquipmentController(equipmentService),
                        new LocationController(locationService),
                        new EquipmentTypeController(equipmentTypeService),
                        new MaintenancePlanController(maintenancePlanService),
                        new MaintenanceChecklistController(maintenanceChecklistService),
                        new WorkOrderController(workOrderService)
                )
                .setMessageConverters(converter)
                .setCustomArgumentResolvers(authPrincipalResolver)
                .setControllerAdvice(new GlobalExceptionHandler(auditService))
                .build();
    }

    @Test
    @DisplayName("2.1 Equipment: ADMIN and MANAGER can create equipment successfully")
    void testCreateEquipment_Success_AdminAndManager() throws Exception {
        currentPrincipal = UserPrincipal.builder().userId(99L).username("admin01").role("ADMIN").build();

        when(equipmentService.createEquipment(any()))
                .thenReturn(com.fpt.ems.be.dto.master.EquipmentResponse.builder().code("EQ-ADMIN").build());

        String payload = """
                {
                    "code": "EQ-ADMIN",
                    "typeId": 1,
                    "locationId": 1
                }
                """;

         mockMvc.perform(post("/api/v1/equipment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("EQ-ADMIN"));
    }

    @Test
    @DisplayName("2.1 Equipment: Unknown property in CreateRequest returns 400 UNKNOWN_PROPERTY")
    void testCreateEquipment_UnknownProperty_Returns400() throws Exception {
        currentPrincipal = UserPrincipal.builder().userId(1L).username("manager1").role("MANAGER").build();

         String payload = """
                {
                    "code": "EQ-999",
                    "typeId": 1,
                    "locationId": 1,
                    "status": "UNDER_MAINTENANCE"
                }
                """;

        mockMvc.perform(post("/api/v1/equipment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("UNKNOWN_PROPERTY"));
    }

    @Test
    @DisplayName("2.1 Equipment: Unknown property 'code' in UpdateRequest returns 400 UNKNOWN_PROPERTY")
    void testUpdateEquipment_CodeInUpdate_Returns400() throws Exception {
        currentPrincipal = UserPrincipal.builder().userId(1L).username("manager1").role("MANAGER").build();

         String payload = """
                {
                    "code": "NEW-CODE-HACK",
                    "typeId": 1,
                    "locationId": 1
                }
                """;

        mockMvc.perform(put("/api/v1/equipment/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("UNKNOWN_PROPERTY"));
    }

    @Test
    @DisplayName("2.2 Plan: Invalid cycle throws 400")
    void testUpdatePlan_InvalidCycle_Returns400() throws Exception {
        currentPrincipal = UserPrincipal.builder().userId(1L).username("manager1").role("MANAGER").build();

        MaintenancePlanRequest req = MaintenancePlanRequest.builder().cycleDays(45).build();
        when(maintenancePlanService.saveOrUpdatePlan(eq(10L), any()))
                .thenThrow(new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_CYCLE_DAYS", "Chu kỳ không hợp lệ"));

        mockMvc.perform(put("/api/v1/equipment/10/plan")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_CYCLE_DAYS"));
    }

    @Test
    @DisplayName("2.3 WorkOrder: Submit with incomplete checklist returns 422 CHECKLIST_INCOMPLETE")
    void testSubmitWorkOrder_IncompleteChecklist_Returns422() throws Exception {
        currentPrincipal = UserPrincipal.builder().userId(2L).username("tech1").role("TECHNICIAN").build();

        when(workOrderService.submitWorkOrder(eq(100L), eq(2L)))
                .thenThrow(new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY, "CHECKLIST_INCOMPLETE",
                        "Chưa hoàn thành checklist", List.of("Kiểm tra áp suất")));

        mockMvc.perform(post("/api/v1/work-orders/100/submit"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("CHECKLIST_INCOMPLETE"));
    }
}
