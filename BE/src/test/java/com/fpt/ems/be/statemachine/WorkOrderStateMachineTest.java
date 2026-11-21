package com.fpt.ems.be.statemachine;

import com.fpt.ems.be.exception.BusinessException;
import com.fpt.ems.be.model.enums.WorkOrderStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.*;

public class WorkOrderStateMachineTest {

    private WorkOrderStateMachine stateMachine;

    @BeforeEach
    void setUp() {
        stateMachine = new WorkOrderStateMachine();
    }

    @Test
    void testValidTransitions() {
        assertTrue(stateMachine.isValidTransition(null, WorkOrderStatus.OPEN));
        assertTrue(stateMachine.isValidTransition(WorkOrderStatus.OPEN, WorkOrderStatus.ASSIGNED));
        assertTrue(stateMachine.isValidTransition(WorkOrderStatus.ASSIGNED, WorkOrderStatus.ASSIGNED)); 
        assertTrue(stateMachine.isValidTransition(WorkOrderStatus.ASSIGNED, WorkOrderStatus.IN_PROGRESS));
        assertTrue(stateMachine.isValidTransition(WorkOrderStatus.IN_PROGRESS, WorkOrderStatus.SUBMITTED));
        assertTrue(stateMachine.isValidTransition(WorkOrderStatus.SUBMITTED, WorkOrderStatus.CLOSED));
        assertTrue(stateMachine.isValidTransition(WorkOrderStatus.SUBMITTED, WorkOrderStatus.IN_PROGRESS)); 
    }

    @Test
    void testInvalidTransitions() {
        assertFalse(stateMachine.isValidTransition(WorkOrderStatus.OPEN, WorkOrderStatus.IN_PROGRESS));
        assertFalse(stateMachine.isValidTransition(WorkOrderStatus.OPEN, WorkOrderStatus.CLOSED));
        assertFalse(stateMachine.isValidTransition(WorkOrderStatus.ASSIGNED, WorkOrderStatus.CLOSED));
        assertFalse(stateMachine.isValidTransition(WorkOrderStatus.IN_PROGRESS, WorkOrderStatus.CLOSED));
        assertFalse(stateMachine.isValidTransition(WorkOrderStatus.CLOSED, WorkOrderStatus.OPEN));
        assertFalse(stateMachine.isValidTransition(WorkOrderStatus.CLOSED, WorkOrderStatus.IN_PROGRESS));
    }

    @Test
    void testClosedIsImmutable() {
        BusinessException ex = assertThrows(BusinessException.class, () ->
                stateMachine.validateTransition(WorkOrderStatus.CLOSED, WorkOrderStatus.IN_PROGRESS));
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        assertEquals("WO_CLOSED_IMMUTABLE", ex.getCode());
    }

    @Test
    void testValidateTransitionThrowsOnInvalid() {
        BusinessException ex = assertThrows(BusinessException.class, () ->
                stateMachine.validateTransition(WorkOrderStatus.OPEN, WorkOrderStatus.SUBMITTED));
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        assertEquals("STATE_VIOLATION", ex.getCode());
    }

    @Test
    void testAssertCanAssign() {
        assertDoesNotThrow(() -> stateMachine.assertCanAssign(WorkOrderStatus.OPEN));
        assertDoesNotThrow(() -> stateMachine.assertCanAssign(WorkOrderStatus.ASSIGNED));
        assertDoesNotThrow(() -> stateMachine.assertCanAssign(WorkOrderStatus.IN_PROGRESS));

        assertThrows(BusinessException.class, () -> stateMachine.assertCanAssign(WorkOrderStatus.CLOSED));
        assertThrows(BusinessException.class, () -> stateMachine.assertCanAssign(WorkOrderStatus.SUBMITTED));
    }

    @Test
    void testAssertCanStart() {
        assertDoesNotThrow(() -> stateMachine.assertCanStart(WorkOrderStatus.ASSIGNED));
        assertThrows(BusinessException.class, () -> stateMachine.assertCanStart(WorkOrderStatus.OPEN));
        assertThrows(BusinessException.class, () -> stateMachine.assertCanStart(WorkOrderStatus.IN_PROGRESS));
        assertThrows(BusinessException.class, () -> stateMachine.assertCanStart(WorkOrderStatus.CLOSED));
    }

    @Test
    void testAssertCanSubmit() {
        assertDoesNotThrow(() -> stateMachine.assertCanSubmit(WorkOrderStatus.IN_PROGRESS));
        assertThrows(BusinessException.class, () -> stateMachine.assertCanSubmit(WorkOrderStatus.ASSIGNED));
        assertThrows(BusinessException.class, () -> stateMachine.assertCanSubmit(WorkOrderStatus.OPEN));
        assertThrows(BusinessException.class, () -> stateMachine.assertCanSubmit(WorkOrderStatus.CLOSED));
    }

    @Test
    void testAssertCanAccept() {
        assertDoesNotThrow(() -> stateMachine.assertCanAccept(WorkOrderStatus.SUBMITTED));
        assertThrows(BusinessException.class, () -> stateMachine.assertCanAccept(WorkOrderStatus.IN_PROGRESS));
        assertThrows(BusinessException.class, () -> stateMachine.assertCanAccept(WorkOrderStatus.ASSIGNED));
        assertThrows(BusinessException.class, () -> stateMachine.assertCanAccept(WorkOrderStatus.CLOSED));
    }

    @Test
    void testAssertCanAdjust() {
        assertDoesNotThrow(() -> stateMachine.assertCanAdjust(WorkOrderStatus.CLOSED));
        assertThrows(BusinessException.class, () -> stateMachine.assertCanAdjust(WorkOrderStatus.SUBMITTED));
        assertThrows(BusinessException.class, () -> stateMachine.assertCanAdjust(WorkOrderStatus.IN_PROGRESS));
    }
}

