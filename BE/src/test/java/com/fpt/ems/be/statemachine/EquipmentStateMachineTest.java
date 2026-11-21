package com.fpt.ems.be.statemachine;

import com.fpt.ems.be.exception.BusinessException;
import com.fpt.ems.be.model.enums.EquipmentStatus;
import com.fpt.ems.be.model.enums.WorkOrderType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.*;

public class EquipmentStateMachineTest {

    private EquipmentStateMachine stateMachine;

    @BeforeEach
    void setUp() {
        stateMachine = new EquipmentStateMachine();
    }

    @Test
    void testValidTransitions() {
        assertTrue(stateMachine.isValidTransition(null, EquipmentStatus.ACTIVE));
        assertTrue(stateMachine.isValidTransition(EquipmentStatus.ACTIVE, EquipmentStatus.UNDER_MAINTENANCE));
        assertTrue(stateMachine.isValidTransition(EquipmentStatus.ACTIVE, EquipmentStatus.OUT_OF_SERVICE));
        assertTrue(stateMachine.isValidTransition(EquipmentStatus.UNDER_MAINTENANCE, EquipmentStatus.ACTIVE));
        assertTrue(stateMachine.isValidTransition(EquipmentStatus.UNDER_MAINTENANCE, EquipmentStatus.OUT_OF_SERVICE));
        assertTrue(stateMachine.isValidTransition(EquipmentStatus.OUT_OF_SERVICE, EquipmentStatus.ACTIVE));
        assertTrue(stateMachine.isValidTransition(EquipmentStatus.ACTIVE, EquipmentStatus.ACTIVE));
    }

    @Test
    void testInvalidTransitions() {
        assertFalse(stateMachine.isValidTransition(EquipmentStatus.OUT_OF_SERVICE, EquipmentStatus.UNDER_MAINTENANCE));
    }

    @Test
    void testValidateTransitionThrowsOnInvalid() {
        BusinessException ex = assertThrows(BusinessException.class, () ->
                stateMachine.validateTransition(EquipmentStatus.OUT_OF_SERVICE, EquipmentStatus.UNDER_MAINTENANCE));
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        assertEquals("STATE_VIOLATION", ex.getCode());
    }

    @Test
    void testDetermineStatusOnWoStart_OutOfServiceRemainsOutOfService() {
         assertEquals(EquipmentStatus.OUT_OF_SERVICE,
                stateMachine.determineStatusOnWoStart(EquipmentStatus.OUT_OF_SERVICE, WorkOrderType.REPAIR));
    }

    @Test
    void testDetermineStatusOnWoStart_ActiveBecomesUnderMaintenance() {
        assertEquals(EquipmentStatus.UNDER_MAINTENANCE,
                stateMachine.determineStatusOnWoStart(EquipmentStatus.ACTIVE, WorkOrderType.PERIODIC));
        assertEquals(EquipmentStatus.UNDER_MAINTENANCE,
                stateMachine.determineStatusOnWoStart(EquipmentStatus.ACTIVE, WorkOrderType.REPAIR));
    }

    @Test
    void testDetermineStatusOnWoPass_ReturnsActive() {
        assertEquals(EquipmentStatus.ACTIVE,
                stateMachine.determineStatusOnWoPass(EquipmentStatus.UNDER_MAINTENANCE, WorkOrderType.PERIODIC));
        assertEquals(EquipmentStatus.ACTIVE,
                stateMachine.determineStatusOnWoPass(EquipmentStatus.OUT_OF_SERVICE, WorkOrderType.REPAIR));
    }
}

