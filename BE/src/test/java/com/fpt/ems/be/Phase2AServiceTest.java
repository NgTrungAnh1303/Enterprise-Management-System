package com.fpt.ems.be;

import com.fpt.ems.be.dto.master.EquipmentCreateRequest;
import com.fpt.ems.be.dto.master.EquipmentResponse;
import com.fpt.ems.be.dto.master.EquipmentUpdateRequest;
import com.fpt.ems.be.dto.plan.ChecklistItemDto;
import com.fpt.ems.be.dto.plan.ChecklistUpdateRequest;
import com.fpt.ems.be.dto.plan.MaintenancePlanRequest;
import com.fpt.ems.be.dto.plan.MaintenancePlanResponse;
import com.fpt.ems.be.dto.workorder.*;
import com.fpt.ems.be.exception.AccessDeniedBusinessException;
import com.fpt.ems.be.exception.BusinessException;
import com.fpt.ems.be.model.*;
import com.fpt.ems.be.model.enums.*;
import com.fpt.ems.be.repository.*;
import com.fpt.ems.be.security.AuthorizationPolicy;
import com.fpt.ems.be.service.*;
import com.fpt.ems.be.statemachine.EquipmentStateMachine;
import com.fpt.ems.be.statemachine.WorkOrderStateMachine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class Phase2AServiceTest {

    @Mock
    private EquipmentRepository equipmentRepository;
    @Mock
    private EquipmentTypeRepository typeRepository;
    @Mock
    private LocationRepository locationRepository;
    @Mock
    private MaintenancePlanRepository planRepository;
    @Mock
    private MaintenanceChecklistRepository checklistRepository;
    @Mock
    private MaintenanceWorkOrderRepository workOrderRepository;
    @Mock
    private WorkOrderAssignmentRepository assignmentRepository;
    @Mock
    private InspectionResultRepository inspectionResultRepository;
    @Mock
    private IncidentReportRepository incidentRepository;
    @Mock
    private AppUserRepository userRepository;
    @Mock
    private WorkOrderAcceptanceRepository acceptanceRepository;
    @Mock
    private WorkOrderAdjustmentRepository adjustmentRepository;
    @Mock
    private StatusChangeRequestRepository statusChangeRequestRepository;

    @Mock
    private MaintenanceHistoryService historyService;
    @Mock
    private AuditService auditService;
    @Mock
    private AuthorizationPolicy authorizationPolicy;

    @Spy
    private WorkOrderStateMachine workOrderStateMachine = new WorkOrderStateMachine();
    @Spy
    private EquipmentStateMachine equipmentStateMachine = new EquipmentStateMachine();

    private EquipmentService equipmentService;
    private MaintenancePlanService planService;
    private MaintenanceChecklistService checklistService;
    private WorkOrderService workOrderService;

    private Equipment sampleEquipment;
    private EquipmentType sampleType;
    private Location sampleLocation;
    private AppUser sampleManager;
    private AppUser sampleTech;

    @BeforeEach
    void setUp() {
        equipmentService = new EquipmentService(
                equipmentRepository, typeRepository, locationRepository,
                planRepository, statusChangeRequestRepository, historyService, auditService, authorizationPolicy
        );

        planService = new MaintenancePlanService(
                planRepository, equipmentRepository, auditService
        );

        checklistService = new MaintenanceChecklistService(
                checklistRepository, typeRepository, auditService
        );

        workOrderService = new WorkOrderService(
                workOrderRepository, assignmentRepository, inspectionResultRepository,
                checklistRepository, equipmentRepository, planRepository,
                incidentRepository, userRepository, acceptanceRepository, adjustmentRepository,
                workOrderStateMachine, equipmentStateMachine, authorizationPolicy,
                historyService, auditService
        );

        sampleType = EquipmentType.builder().typeId(1L).name("Centrifuge").build();
        sampleLocation = Location.builder().locationId(1L).room("Lab 101").area("North Wing").build();

        sampleEquipment = Equipment.builder()
                .equipmentId(10L)
                .code("EQ-100")
                .type(sampleType)
                .location(sampleLocation)
                .status(EquipmentStatus.ACTIVE)
                .build();

        Role managerRole = Role.builder().roleId(2).name(RoleName.MANAGER).build();
        sampleManager = AppUser.builder().userId(1L).username("manager1").fullName("Manager One").role(managerRole).build();

        Role techRole = Role.builder().roleId(3).name(RoleName.TECHNICIAN).build();
        sampleTech = AppUser.builder().userId(2L).username("tech1").fullName("Tech One").role(techRole).status(UserStatus.ACTIVE).build();
    }

    


    @Test
    @DisplayName("2.1 Equipment: Create equipment successfully initializes ACTIVE status and logs history/audit")
    void testCreateEquipment_Success() {
        EquipmentCreateRequest req = EquipmentCreateRequest.builder()
                .code("EQ-NEW")
                .typeId(1L)
                .locationId(1L)
                .build();

        when(equipmentRepository.existsByCode("EQ-NEW")).thenReturn(false);
        when(typeRepository.findById(1L)).thenReturn(Optional.of(sampleType));
        when(locationRepository.findById(1L)).thenReturn(Optional.of(sampleLocation));
        when(equipmentRepository.save(any(Equipment.class))).thenAnswer(inv -> {
            Equipment eq = inv.getArgument(0);
            eq.setEquipmentId(99L);
            return eq;
        });

        EquipmentResponse resp = equipmentService.createEquipment(req);

        assertNotNull(resp);
        assertEquals("EQ-NEW", resp.getCode());
        assertEquals(EquipmentStatus.ACTIVE, resp.getStatus());
        verify(historyService).record(any(), isNull(), eq("EQUIPMENT_CREATE"), isNull(), eq("ACTIVE"), isNull(), anyString());
        verify(auditService).record(eq("EQUIPMENT_CREATE"), eq("EQUIPMENT"), eq("99"), eq(AuditOutcome.SUCCESS), eq("OK"), isNull(), any());
    }

    @Test
    @DisplayName("2.1 Equipment: Duplicate code throws 409 EQUIPMENT_CODE_EXISTS")
    void testCreateEquipment_DuplicateCode_Throws409() {
        EquipmentCreateRequest req = EquipmentCreateRequest.builder()
                .code("EQ-100")
                .typeId(1L)
                .locationId(1L)
                .build();

        when(equipmentRepository.existsByCode("EQ-100")).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class, () -> equipmentService.createEquipment(req));
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        assertEquals("EQUIPMENT_CODE_EXISTS", ex.getCode());
    }

    @Test
    @DisplayName("2.1 Equipment: Update equipment preserves original code and status")
    void testUpdateEquipment_PreservesCodeAndStatus() {
        EquipmentUpdateRequest req = EquipmentUpdateRequest.builder()
                .brand("Brand B")
                .model("Model B")
                .typeId(1L)
                .locationId(1L)
                .build();

        when(equipmentRepository.findByIdWithLock(10L)).thenReturn(Optional.of(sampleEquipment));
        when(typeRepository.findById(1L)).thenReturn(Optional.of(sampleType));
        when(locationRepository.findById(1L)).thenReturn(Optional.of(sampleLocation));
        when(equipmentRepository.save(any(Equipment.class))).thenAnswer(inv -> inv.getArgument(0));

        EquipmentResponse resp = equipmentService.updateEquipment(10L, req);

        assertEquals("EQ-100", resp.getCode());
        assertEquals(EquipmentStatus.ACTIVE, resp.getStatus());
        assertEquals("Brand B", resp.getBrand());
        verify(historyService).record(any(), isNull(), eq("EQUIPMENT_UPDATE"), eq("ACTIVE"), eq("ACTIVE"), isNull(), anyString());
    }

    @Test
    @DisplayName("2.1 Equipment Detail: MANAGER receives pendingStatusRequest")
    void testGetEquipmentById_Manager_ReceivesPendingStatusRequest() {
        StatusChangeRequest pendingReq = StatusChangeRequest.builder()
                .requestId(50L)
                .equipment(sampleEquipment)
                .proposedStatus("OUT_OF_SERVICE")
                .reason("Motor failure")
                .status(StatusRequestStatus.PENDING)
                .proposedBy(sampleTech)
                .build();

        when(equipmentRepository.findById(10L)).thenReturn(Optional.of(sampleEquipment));
        when(planRepository.findByEquipment_EquipmentId(10L)).thenReturn(Optional.empty());
        when(statusChangeRequestRepository.findByEquipment_EquipmentIdAndStatus(10L, StatusRequestStatus.PENDING))
                .thenReturn(List.of(pendingReq));

        EquipmentResponse resp = equipmentService.getEquipmentById(10L, 1L, "MANAGER");

        assertNotNull(resp);
        assertNotNull(resp.getPendingStatusRequest());
        assertEquals(50L, resp.getPendingStatusRequest().getRequestId());
        assertEquals("OUT_OF_SERVICE", resp.getPendingStatusRequest().getProposedStatus());
    }

    @Test
    @DisplayName("2.1 Equipment Detail: TECHNICIAN who proposed OOS receives pendingStatusRequest")
    void testGetEquipmentById_TechnicianProposer_ReceivesPendingStatusRequest() {
        StatusChangeRequest pendingReq = StatusChangeRequest.builder()
                .requestId(50L)
                .equipment(sampleEquipment)
                .proposedStatus("OUT_OF_SERVICE")
                .reason("Motor failure")
                .status(StatusRequestStatus.PENDING)
                .proposedBy(sampleTech) 
                .build();

        when(equipmentRepository.findById(10L)).thenReturn(Optional.of(sampleEquipment));
        when(planRepository.findByEquipment_EquipmentId(10L)).thenReturn(Optional.empty());
        when(statusChangeRequestRepository.findByEquipment_EquipmentIdAndStatus(10L, StatusRequestStatus.PENDING))
                .thenReturn(List.of(pendingReq));

        EquipmentResponse resp = equipmentService.getEquipmentById(10L, 2L, "TECHNICIAN");

        assertNotNull(resp);
        assertNotNull(resp.getPendingStatusRequest());
        assertEquals(50L, resp.getPendingStatusRequest().getRequestId());
    }

    @Test
    @DisplayName("2.1 Equipment Detail: Other TECHNICIAN receives null pendingStatusRequest")
    void testGetEquipmentById_OtherTechnician_ReceivesNullPendingStatusRequest() {
        StatusChangeRequest pendingReq = StatusChangeRequest.builder()
                .requestId(50L)
                .equipment(sampleEquipment)
                .proposedStatus("OUT_OF_SERVICE")
                .reason("Motor failure")
                .status(StatusRequestStatus.PENDING)
                .proposedBy(sampleTech) 
                .build();

        when(equipmentRepository.findById(10L)).thenReturn(Optional.of(sampleEquipment));
        when(planRepository.findByEquipment_EquipmentId(10L)).thenReturn(Optional.empty());
        when(statusChangeRequestRepository.findByEquipment_EquipmentIdAndStatus(10L, StatusRequestStatus.PENDING))
                .thenReturn(List.of(pendingReq));

        EquipmentResponse resp = equipmentService.getEquipmentById(10L, 99L, "TECHNICIAN"); 

        assertNotNull(resp);
        assertNull(resp.getPendingStatusRequest());
    }

    @Test
    @DisplayName("2.1 Equipment Detail: EQUIPMENT_USER receives null pendingStatusRequest")
    void testGetEquipmentById_EquipmentUser_ReceivesNullPendingStatusRequest() {
        StatusChangeRequest pendingReq = StatusChangeRequest.builder()
                .requestId(50L)
                .equipment(sampleEquipment)
                .proposedStatus("OUT_OF_SERVICE")
                .reason("Motor failure")
                .status(StatusRequestStatus.PENDING)
                .proposedBy(sampleTech)
                .build();

        when(equipmentRepository.findById(10L)).thenReturn(Optional.of(sampleEquipment));
        when(planRepository.findByEquipment_EquipmentId(10L)).thenReturn(Optional.empty());

        EquipmentResponse resp = equipmentService.getEquipmentById(10L, 3L, "EQUIPMENT_USER");

        assertNotNull(resp);
        assertNull(resp.getPendingStatusRequest());
    }

    @Test
    @DisplayName("2.1 Equipment Detail: ADMIN receives null pendingStatusRequest")
    void testGetEquipmentById_Admin_ReceivesNullPendingStatusRequest() {
        StatusChangeRequest pendingReq = StatusChangeRequest.builder()
                .requestId(50L)
                .equipment(sampleEquipment)
                .proposedStatus("OUT_OF_SERVICE")
                .reason("Motor failure")
                .status(StatusRequestStatus.PENDING)
                .proposedBy(sampleTech)
                .build();

        when(equipmentRepository.findById(10L)).thenReturn(Optional.of(sampleEquipment));
        when(planRepository.findByEquipment_EquipmentId(10L)).thenReturn(Optional.empty());

        EquipmentResponse resp = equipmentService.getEquipmentById(10L, 4L, "ADMIN");

        assertNotNull(resp);
        assertNull(resp.getPendingStatusRequest());
    }

    

    @Test
    @DisplayName("2.2 Plan: Invalid cycle throws 400 INVALID_CYCLE_DAYS")
    void testSavePlan_InvalidCycle_Throws400() {
        MaintenancePlanRequest req = MaintenancePlanRequest.builder().cycleDays(45).build();

        BusinessException ex = assertThrows(BusinessException.class, () -> planService.saveOrUpdatePlan(10L, req));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals("INVALID_CYCLE_DAYS", ex.getCode());
    }

    @Test
    @DisplayName("2.2 Plan: Valid cycle 30/90/180 computes nextDueDate = startDate + cycleDays")
    void testSavePlan_NewPlan_CalculatesNextDueDate() {
        LocalDate start = LocalDate.of(2026, 1, 1);
        MaintenancePlanRequest req = MaintenancePlanRequest.builder()
                .cycleDays(90)
                .startDate(start)
                .build();

        when(equipmentRepository.findByIdWithLock(10L)).thenReturn(Optional.of(sampleEquipment));
        when(planRepository.findByEquipment_EquipmentId(10L)).thenReturn(Optional.empty());
        when(planRepository.save(any(MaintenancePlan.class))).thenAnswer(inv -> {
            MaintenancePlan p = inv.getArgument(0);
            p.setPlanId(50L);
            return p;
        });

        MaintenancePlanResponse resp = planService.saveOrUpdatePlan(10L, req);

        assertNotNull(resp);
        assertEquals(90, resp.getCycleDays());
        assertEquals(LocalDate.of(2026, 4, 1), resp.getNextDueDate());
        verify(auditService).record(eq("PLAN_UPDATE"), eq("MAINTENANCE_PLAN"), eq("50"), eq(AuditOutcome.SUCCESS), eq("OK"), isNull(), any());
    }

    @Test
    @DisplayName("2.2 Checklist: Empty items list throws 400 CHECKLIST_EMPTY")
    void testUpdateChecklist_Empty_Throws400() {
        ChecklistUpdateRequest req = ChecklistUpdateRequest.builder().items(List.of()).build();
        when(typeRepository.findById(1L)).thenReturn(Optional.of(sampleType));

        BusinessException ex = assertThrows(BusinessException.class, () -> checklistService.updateChecklist(1L, req));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals("CHECKLIST_EMPTY", ex.getCode());
    }

    @Test
    @DisplayName("2.2 Checklist: Blank item text throws 400 CHECKLIST_ITEM_EMPTY")
    void testUpdateChecklist_BlankItem_Throws400() {
        ChecklistUpdateRequest req = ChecklistUpdateRequest.builder()
                .items(List.of(ChecklistItemDto.builder().item("   ").build()))
                .build();
        when(typeRepository.findById(1L)).thenReturn(Optional.of(sampleType));

        BusinessException ex = assertThrows(BusinessException.class, () -> checklistService.updateChecklist(1L, req));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals("CHECKLIST_ITEM_EMPTY", ex.getCode());
    }

  

    @Test
    @DisplayName("2.3 WorkOrder: Periodic WO requires ACTIVE equipment")
    void testCreatePeriodicWo_EquipmentNotActive_Throws409() {
        sampleEquipment.setStatus(EquipmentStatus.UNDER_MAINTENANCE);
        CreateWorkOrderRequest req = CreateWorkOrderRequest.builder()
                .equipmentId(10L)
                .type(WorkOrderType.PERIODIC)
                .build();

        when(equipmentRepository.findByIdWithLock(10L)).thenReturn(Optional.of(sampleEquipment));

        BusinessException ex = assertThrows(BusinessException.class, () -> workOrderService.createWorkOrder(req, 1L));
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        assertEquals("EQUIPMENT_NOT_ACTIVE", ex.getCode());
    }

    @Test
    @DisplayName("2.3 WorkOrder: Server computes period from plan nextDueDate (ignoring client period)")
    void testCreatePeriodicWo_ServerComputesPeriod_IgnoresClient() {
        sampleEquipment.setStatus(EquipmentStatus.ACTIVE);
        LocalDate nextDue = LocalDate.of(2026, 11, 20);
        MaintenancePlan plan = MaintenancePlan.builder().planId(1L).equipment(sampleEquipment).nextDueDate(nextDue).build();

        CreateWorkOrderRequest req = CreateWorkOrderRequest.builder()
                .equipmentId(10L)
                .type(WorkOrderType.PERIODIC)
                .period("CLIENT_FAKE_PERIOD") 
                .build();

        when(equipmentRepository.findByIdWithLock(10L)).thenReturn(Optional.of(sampleEquipment));
        when(planRepository.findByEquipment_EquipmentId(10L)).thenReturn(Optional.of(plan));
        when(workOrderRepository.existsByEquipment_EquipmentIdAndTypeAndStatusNot(
                10L, WorkOrderType.PERIODIC, WorkOrderStatus.CLOSED)).thenReturn(false);

        when(workOrderRepository.save(any(MaintenanceWorkOrder.class))).thenAnswer(inv -> {
            MaintenanceWorkOrder wo = inv.getArgument(0);
            wo.setWoId(100L);
            return wo;
        });

        when(workOrderRepository.findById(100L)).thenAnswer(inv -> Optional.of(
                MaintenanceWorkOrder.builder()
                        .woId(100L)
                        .woCode("WO-20261113-1000")
                        .equipment(sampleEquipment)
                        .type(WorkOrderType.PERIODIC)
                        .period("2026-11-20")
                        .status(WorkOrderStatus.OPEN)
                        .build()
        ));
        when(checklistRepository.findByEquipmentType_TypeId(any())).thenReturn(List.of());
        when(inspectionResultRepository.findByWorkOrder_WoId(100L)).thenReturn(List.of());
        when(acceptanceRepository.findByWorkOrder_WoIdOrderByAcceptedAtDesc(100L)).thenReturn(List.of());

        WorkOrderDetailResponse resp = workOrderService.createWorkOrder(req, 1L);

        assertNotNull(resp);
        assertEquals("2026-11-20", resp.getPeriod());
        assertEquals(WorkOrderStatus.OPEN, resp.getStatus());
    }

    @Test
    @DisplayName("2.3 WorkOrder: Duplicate periodic period throws 409 WO_PERIOD_DUPLICATE")
    void testCreatePeriodicWo_DuplicatePeriod_Throws409() {
        sampleEquipment.setStatus(EquipmentStatus.ACTIVE);
        LocalDate nextDue = LocalDate.of(2026, 11, 20);
        MaintenancePlan plan = MaintenancePlan.builder().planId(1L).equipment(sampleEquipment).nextDueDate(nextDue).build();

        CreateWorkOrderRequest req = CreateWorkOrderRequest.builder()
                .equipmentId(10L)
                .type(WorkOrderType.PERIODIC)
                .build();

        when(equipmentRepository.findByIdWithLock(10L)).thenReturn(Optional.of(sampleEquipment));
        when(planRepository.findByEquipment_EquipmentId(10L)).thenReturn(Optional.of(plan));
        when(workOrderRepository.existsByEquipment_EquipmentIdAndTypeAndStatusNot(
                10L, WorkOrderType.PERIODIC, WorkOrderStatus.CLOSED)).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class, () -> workOrderService.createWorkOrder(req, 1L));
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        assertEquals("WO_PERIOD_DUPLICATE", ex.getCode());
    }

    @Test
    @DisplayName("2.3 WorkOrder: Repair WO requires CLASSIFIED incident")
    void testCreateRepairWo_IncidentNotClassified_Throws409() {
        IncidentReport incident = IncidentReport.builder()
                .incidentId(5L)
                .status(IncidentStatus.REPORTED)  
                .equipment(sampleEquipment)
                .build();

        CreateWorkOrderRequest req = CreateWorkOrderRequest.builder()
                .equipmentId(10L)
                .type(WorkOrderType.REPAIR)
                .incidentId(5L)
                .build();

        when(incidentRepository.findByIdWithLock(5L)).thenReturn(Optional.of(incident));

        BusinessException ex = assertThrows(BusinessException.class, () -> workOrderService.createWorkOrder(req, 1L));
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        assertEquals("STATE_VIOLATION", ex.getCode());
    }

    @Test
    @DisplayName("2.3 WorkOrder: Assigning repair WO transitions incident to ASSIGNED in same transaction")
    void testAssignRepairWo_TransitionsIncidentToAssigned() {
        IncidentReport incident = IncidentReport.builder()
                .incidentId(5L)
                .status(IncidentStatus.CLASSIFIED)
                .equipment(sampleEquipment)
                .build();

        MaintenanceWorkOrder wo = MaintenanceWorkOrder.builder()
                .woId(100L)
                .status(WorkOrderStatus.OPEN)
                .type(WorkOrderType.REPAIR)
                .incident(incident)
                .equipment(sampleEquipment)
                .build();

        when(workOrderRepository.findByIdWithLock(100L)).thenReturn(Optional.of(wo));
        when(userRepository.findById(2L)).thenReturn(Optional.of(sampleTech));
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleManager));
        when(incidentRepository.findByIdWithLock(5L)).thenReturn(Optional.of(incident));

        when(workOrderRepository.findById(100L)).thenReturn(Optional.of(wo));
        when(checklistRepository.findByEquipmentType_TypeId(any())).thenReturn(List.of());
        when(inspectionResultRepository.findByWorkOrder_WoId(100L)).thenReturn(List.of());
        when(acceptanceRepository.findByWorkOrder_WoIdOrderByAcceptedAtDesc(100L)).thenReturn(List.of());

        AssignWorkOrderRequest req = AssignWorkOrderRequest.builder().technicianId(2L).build();
        workOrderService.assignWorkOrder(100L, req, 1L);

        assertEquals(WorkOrderStatus.ASSIGNED, wo.getStatus());
        assertEquals(IncidentStatus.ASSIGNED, incident.getStatus()); 
        verify(incidentRepository).save(incident);
        verify(assignmentRepository).save(any(WorkOrderAssignment.class));
    }

    @Test
    @DisplayName("2.3 WorkOrder: Start repair WO on OUT_OF_SERVICE equipment preserves OUT_OF_SERVICE status (Decision 7)")
    void testStartWo_OutOfServiceRemainsOutOfService() {
        sampleEquipment.setStatus(EquipmentStatus.OUT_OF_SERVICE);
        MaintenanceWorkOrder wo = MaintenanceWorkOrder.builder()
                .woId(100L)
                .status(WorkOrderStatus.ASSIGNED)
                .type(WorkOrderType.REPAIR)
                .equipment(sampleEquipment)
                .build();

        when(workOrderRepository.findByIdWithLock(100L)).thenReturn(Optional.of(wo));
        when(equipmentRepository.findByIdWithLock(10L)).thenReturn(Optional.of(sampleEquipment));
        when(workOrderRepository.findById(100L)).thenReturn(Optional.of(wo));
        when(checklistRepository.findByEquipmentType_TypeId(any())).thenReturn(List.of());
        when(inspectionResultRepository.findByWorkOrder_WoId(100L)).thenReturn(List.of());
        when(acceptanceRepository.findByWorkOrder_WoIdOrderByAcceptedAtDesc(100L)).thenReturn(List.of());

        workOrderService.startWorkOrder(100L, 2L);

        assertEquals(WorkOrderStatus.IN_PROGRESS, wo.getStatus());
        assertEquals(EquipmentStatus.OUT_OF_SERVICE, sampleEquipment.getStatus()); 
    }

    @Test
    @DisplayName("2.3 WorkOrder: Start WO on ACTIVE equipment switches equipment to UNDER_MAINTENANCE")
    void testStartWo_ActiveSwitchesToUnderMaintenance() {
        sampleEquipment.setStatus(EquipmentStatus.ACTIVE);
        MaintenanceWorkOrder wo = MaintenanceWorkOrder.builder()
                .woId(100L)
                .status(WorkOrderStatus.ASSIGNED)
                .type(WorkOrderType.PERIODIC)
                .equipment(sampleEquipment)
                .build();

        when(workOrderRepository.findByIdWithLock(100L)).thenReturn(Optional.of(wo));
        when(equipmentRepository.findByIdWithLock(10L)).thenReturn(Optional.of(sampleEquipment));
        when(workOrderRepository.findById(100L)).thenReturn(Optional.of(wo));
        when(checklistRepository.findByEquipmentType_TypeId(any())).thenReturn(List.of());
        when(inspectionResultRepository.findByWorkOrder_WoId(100L)).thenReturn(List.of());
        when(acceptanceRepository.findByWorkOrder_WoIdOrderByAcceptedAtDesc(100L)).thenReturn(List.of());

        workOrderService.startWorkOrder(100L, 2L);

        assertEquals(WorkOrderStatus.IN_PROGRESS, wo.getStatus());
        assertEquals(EquipmentStatus.UNDER_MAINTENANCE, sampleEquipment.getStatus());
    }

    @Test
    @DisplayName("2.3 WorkOrder: Submit WO with incomplete checklist throws 422 CHECKLIST_INCOMPLETE")
    void testSubmitWo_IncompleteChecklist_Throws422() {
        MaintenanceWorkOrder wo = MaintenanceWorkOrder.builder()
                .woId(100L)
                .status(WorkOrderStatus.IN_PROGRESS)
                .equipment(sampleEquipment)
                .build();

        MaintenanceChecklist req1 = MaintenanceChecklist.builder().checklistId(1L).item("Check Oil").isRequired(true).build();
        MaintenanceChecklist req2 = MaintenanceChecklist.builder().checklistId(2L).item("Check Pressure").isRequired(true).build();

        when(workOrderRepository.findByIdWithLock(100L)).thenReturn(Optional.of(wo));
        when(checklistRepository.findByEquipmentType_TypeId(1L)).thenReturn(List.of(req1, req2));

        
        InspectionResult res1 = InspectionResult.builder().checklist(req1).done(true).build();
        when(inspectionResultRepository.findByWorkOrder_WoIdAndChecklist_ChecklistId(100L, 1L)).thenReturn(Optional.of(res1));
        when(inspectionResultRepository.findByWorkOrder_WoIdAndChecklist_ChecklistId(100L, 2L)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, () -> workOrderService.submitWorkOrder(100L, 2L));
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, ex.getStatus());
        assertEquals("CHECKLIST_INCOMPLETE", ex.getCode());
        assertNotNull(ex.getDetails());
    }

    @Test
    @DisplayName("2.3 WorkOrder: Submit WO with all required checklist done transitions to SUBMITTED")
    void testSubmitWo_CompleteChecklist_TransitionsToSubmitted() {
        MaintenanceWorkOrder wo = MaintenanceWorkOrder.builder()
                .woId(100L)
                .status(WorkOrderStatus.IN_PROGRESS)
                .equipment(sampleEquipment)
                .build();

        MaintenanceChecklist req1 = MaintenanceChecklist.builder().checklistId(1L).item("Check Oil").isRequired(true).build();

        when(workOrderRepository.findByIdWithLock(100L)).thenReturn(Optional.of(wo));
        when(checklistRepository.findByEquipmentType_TypeId(1L)).thenReturn(List.of(req1));

        InspectionResult res1 = InspectionResult.builder().checklist(req1).done(true).build();
        when(inspectionResultRepository.findByWorkOrder_WoIdAndChecklist_ChecklistId(100L, 1L)).thenReturn(Optional.of(res1));

        when(workOrderRepository.findById(100L)).thenReturn(Optional.of(wo));
        when(inspectionResultRepository.findByWorkOrder_WoId(100L)).thenReturn(List.of(res1));
        when(acceptanceRepository.findByWorkOrder_WoIdOrderByAcceptedAtDesc(100L)).thenReturn(List.of());

        WorkOrderDetailResponse resp = workOrderService.submitWorkOrder(100L, 2L);

        assertEquals(WorkOrderStatus.SUBMITTED, wo.getStatus());
        verify(historyService).record(any(), eq(wo), eq("WO_SUBMIT"), eq("IN_PROGRESS"), eq("SUBMITTED"), isNull(), anyString());
    }

    @Test
    @DisplayName("2.3 WorkOrder: List work orders by Admin or EquipmentUser throws 403 ACCESS_DENIED")
    void testGetWorkOrders_UserOrAdmin_Throws403() {
        assertThrows(AccessDeniedBusinessException.class, () ->
                workOrderService.getWorkOrders(null, null, 0, 20, 1L, RoleName.ADMIN.name()));

        assertThrows(AccessDeniedBusinessException.class, () ->
                workOrderService.getWorkOrders(null, null, 0, 20, 3L, RoleName.EQUIPMENT_USER.name()));
    }
}
