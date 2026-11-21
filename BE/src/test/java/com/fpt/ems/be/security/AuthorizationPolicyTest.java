package com.fpt.ems.be.security;

import com.fpt.ems.be.exception.AccessDeniedBusinessException;
import com.fpt.ems.be.exception.BusinessException;
import com.fpt.ems.be.model.AppUser;
import com.fpt.ems.be.model.MaintenanceWorkOrder;
import com.fpt.ems.be.model.WorkOrderAssignment;
import com.fpt.ems.be.model.enums.RoleName;
import com.fpt.ems.be.model.enums.UserStatus;
import com.fpt.ems.be.model.enums.WorkOrderStatus;
import com.fpt.ems.be.repository.AppUserRepository;
import com.fpt.ems.be.repository.IncidentReportRepository;
import com.fpt.ems.be.repository.InspectionResultRepository;
import com.fpt.ems.be.repository.MaintenanceWorkOrderRepository;
import com.fpt.ems.be.repository.WorkOrderAssignmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AuthorizationPolicyTest {

    @Mock
    private MaintenanceWorkOrderRepository workOrderRepository;
    @Mock
    private WorkOrderAssignmentRepository assignmentRepository;
    @Mock
    private InspectionResultRepository inspectionResultRepository;
    @Mock
    private IncidentReportRepository incidentReportRepository;
    @Mock
    private AppUserRepository userRepository;

    private AuthorizationPolicy policy;

    @BeforeEach
    void setUp() {
        policy = new AuthorizationPolicy(
                workOrderRepository, assignmentRepository, inspectionResultRepository,
                incidentReportRepository, userRepository
        );
    }

    @Test
    @DisplayName("AZ-01: Technician assigned can access; unassigned throws 403 WO_NOT_ASSIGNED")
    void testTechnicianAssigned_Check() {
        MaintenanceWorkOrder wo = MaintenanceWorkOrder.builder().woId(1L).status(WorkOrderStatus.IN_PROGRESS).build();
        when(workOrderRepository.findById(1L)).thenReturn(Optional.of(wo));

        AppUser techA = AppUser.builder().userId(10L).build();
        WorkOrderAssignment currentAssign = WorkOrderAssignment.builder().technician(techA).isCurrent(true).build();
        when(assignmentRepository.findByWorkOrder_WoIdAndIsCurrentTrue(1L)).thenReturn(Optional.of(currentAssign));

        
        assertDoesNotThrow(() -> policy.assertTechnicianAssigned(1L, 10L));

     
        AccessDeniedBusinessException ex = assertThrows(AccessDeniedBusinessException.class,
                () -> policy.assertTechnicianAssigned(1L, 20L));
        assertEquals("WO_NOT_ASSIGNED", ex.getReasonCode());
    }

    @Test
    @DisplayName("AZ-03: Anti-self-acceptance: Manager who was ever assigned throws 403 SELF_ACCEPT")
    void testSelfAcceptance_PreviouslyAssigned_Throws403() {
        AppUser tech = AppUser.builder().userId(5L).build();
        WorkOrderAssignment oldAssign = WorkOrderAssignment.builder().technician(tech).isCurrent(false).build();
        when(assignmentRepository.findByWorkOrder_WoId(1L)).thenReturn(List.of(oldAssign));

        AccessDeniedBusinessException ex = assertThrows(AccessDeniedBusinessException.class,
                () -> policy.assertNotSelfAcceptance(1L, 5L));
        assertEquals("SELF_ACCEPT", ex.getReasonCode());
    }

    @Test
    @DisplayName("AZ-03: Anti-self-acceptance: Manager who entered inspection result throws 403 SELF_ACCEPT")
    void testSelfAcceptance_EnteredResult_Throws403() {
        when(assignmentRepository.findByWorkOrder_WoId(1L)).thenReturn(List.of());
        when(inspectionResultRepository.existsByWorkOrder_WoIdAndEnteredBy_UserId(1L, 5L)).thenReturn(true);

        AccessDeniedBusinessException ex = assertThrows(AccessDeniedBusinessException.class,
                () -> policy.assertNotSelfAcceptance(1L, 5L));
        assertEquals("SELF_ACCEPT", ex.getReasonCode());
    }

    @Test
    @DisplayName("Read WO: Manager passes, Technician assigned passes, unassigned Technician throws 403, Admin/User throws 403")
    void testCanReadWorkOrder() {
        when(assignmentRepository.existsByWorkOrder_WoIdAndTechnician_UserId(1L, 10L)).thenReturn(true);
        when(assignmentRepository.existsByWorkOrder_WoIdAndTechnician_UserId(1L, 20L)).thenReturn(false);

     
        assertDoesNotThrow(() -> policy.assertCanReadWorkOrder(1L, 1L, RoleName.MANAGER.name()));

     
        assertDoesNotThrow(() -> policy.assertCanReadWorkOrder(1L, 10L, RoleName.TECHNICIAN.name()));

        
        AccessDeniedBusinessException exTech = assertThrows(AccessDeniedBusinessException.class,
                () -> policy.assertCanReadWorkOrder(1L, 20L, RoleName.TECHNICIAN.name()));
        assertEquals("WO_NOT_ASSIGNED", exTech.getReasonCode());

      
        AccessDeniedBusinessException exAdmin = assertThrows(AccessDeniedBusinessException.class,
                () -> policy.assertCanReadWorkOrder(1L, 99L, RoleName.ADMIN.name()));
        assertEquals("ACCESS_DENIED", exAdmin.getReasonCode());

       
        AccessDeniedBusinessException exUser = assertThrows(AccessDeniedBusinessException.class,
                () -> policy.assertCanReadWorkOrder(1L, 88L, RoleName.EQUIPMENT_USER.name()));
        assertEquals("ACCESS_DENIED", exUser.getReasonCode());
    }

    @Test
    @DisplayName("AZ-12: Read Equipment History: Manager passes, Technician related passes, unassigned Tech / Admin / User throws 403")
    void testCanReadEquipmentHistory() {
        when(assignmentRepository.existsByWorkOrder_Equipment_EquipmentIdAndTechnician_UserId(10L, 10L)).thenReturn(true);
        when(assignmentRepository.existsByWorkOrder_Equipment_EquipmentIdAndTechnician_UserId(10L, 20L)).thenReturn(false);

     
        assertDoesNotThrow(() -> policy.assertCanReadEquipmentHistory(10L, 1L, RoleName.MANAGER.name()));

        
        assertDoesNotThrow(() -> policy.assertCanReadEquipmentHistory(10L, 10L, RoleName.TECHNICIAN.name()));

      
        AccessDeniedBusinessException exTech = assertThrows(AccessDeniedBusinessException.class,
                () -> policy.assertCanReadEquipmentHistory(10L, 20L, RoleName.TECHNICIAN.name()));
        assertEquals("ACCESS_DENIED", exTech.getReasonCode());

     
        AccessDeniedBusinessException exAdmin = assertThrows(AccessDeniedBusinessException.class,
                () -> policy.assertCanReadEquipmentHistory(10L, 99L, RoleName.ADMIN.name()));
        assertEquals("ACCESS_DENIED", exAdmin.getReasonCode());

      
        AccessDeniedBusinessException exUser = assertThrows(AccessDeniedBusinessException.class,
                () -> policy.assertCanReadEquipmentHistory(10L, 88L, RoleName.EQUIPMENT_USER.name()));
        assertEquals("ACCESS_DENIED", exUser.getReasonCode());
    }
}

