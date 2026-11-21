package com.fpt.ems.be.statemachine;

import com.fpt.ems.be.exception.BusinessException;
import com.fpt.ems.be.model.enums.IncidentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.*;

public class IncidentStateMachineTest {

    private IncidentStateMachine stateMachine;

    @BeforeEach
    void setUp() {
        stateMachine = new IncidentStateMachine();
    }

    @Test
    void testValidTransitions() {
        assertTrue(stateMachine.isValidTransition(null, IncidentStatus.REPORTED));
        assertTrue(stateMachine.isValidTransition(IncidentStatus.REPORTED, IncidentStatus.CLASSIFIED));
        assertTrue(stateMachine.isValidTransition(IncidentStatus.CLASSIFIED, IncidentStatus.ASSIGNED));
        assertTrue(stateMachine.isValidTransition(IncidentStatus.ASSIGNED, IncidentStatus.IN_REPAIR));
        assertTrue(stateMachine.isValidTransition(IncidentStatus.IN_REPAIR, IncidentStatus.RESOLVED));
        assertTrue(stateMachine.isValidTransition(IncidentStatus.RESOLVED, IncidentStatus.CLOSED));
        assertTrue(stateMachine.isValidTransition(IncidentStatus.REPORTED, IncidentStatus.REPORTED));
    }

    @Test
    void testInvalidTransitions() {
        assertFalse(stateMachine.isValidTransition(IncidentStatus.REPORTED, IncidentStatus.ASSIGNED));
        assertFalse(stateMachine.isValidTransition(IncidentStatus.REPORTED, IncidentStatus.RESOLVED));
        assertFalse(stateMachine.isValidTransition(IncidentStatus.CLASSIFIED, IncidentStatus.RESOLVED));
        assertFalse(stateMachine.isValidTransition(IncidentStatus.CLOSED, IncidentStatus.REPORTED));
    }

    @Test
    void testClosedIsImmutable() {
        BusinessException ex = assertThrows(BusinessException.class, () ->
                stateMachine.validateTransition(IncidentStatus.CLOSED, IncidentStatus.RESOLVED));
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        assertEquals("INCIDENT_CLOSED_IMMUTABLE", ex.getCode());
    }

    @Test
    void testValidateTransitionThrowsOnInvalid() {
        BusinessException ex = assertThrows(BusinessException.class, () ->
                stateMachine.validateTransition(IncidentStatus.REPORTED, IncidentStatus.RESOLVED));
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        assertEquals("STATE_VIOLATION", ex.getCode());
    }

    @Test
    void testAssertCanClassify() {
        assertDoesNotThrow(() -> stateMachine.assertCanClassify(IncidentStatus.REPORTED));
        assertThrows(BusinessException.class, () -> stateMachine.assertCanClassify(IncidentStatus.CLASSIFIED));
        assertThrows(BusinessException.class, () -> stateMachine.assertCanClassify(IncidentStatus.CLOSED));
    }

    @Test
    void testAssertCanCreateRepairWo() {
        
        assertDoesNotThrow(() -> stateMachine.assertCanCreateRepairWo(IncidentStatus.CLASSIFIED));
        assertThrows(BusinessException.class, () -> stateMachine.assertCanCreateRepairWo(IncidentStatus.REPORTED));
        assertThrows(BusinessException.class, () -> stateMachine.assertCanCreateRepairWo(IncidentStatus.ASSIGNED));
        assertThrows(BusinessException.class, () -> stateMachine.assertCanCreateRepairWo(IncidentStatus.CLOSED));
    }

    @Test
    void testAssertCanProgress() {
        assertDoesNotThrow(() -> stateMachine.assertCanProgress(IncidentStatus.ASSIGNED));
        assertDoesNotThrow(() -> stateMachine.assertCanProgress(IncidentStatus.IN_REPAIR));
        assertThrows(BusinessException.class, () -> stateMachine.assertCanProgress(IncidentStatus.REPORTED));
        assertThrows(BusinessException.class, () -> stateMachine.assertCanProgress(IncidentStatus.CLOSED));
    }

    @Test
    void testAssertCanClose() {
        assertDoesNotThrow(() -> stateMachine.assertCanClose(IncidentStatus.RESOLVED));
        assertThrows(BusinessException.class, () -> stateMachine.assertCanClose(IncidentStatus.IN_REPAIR));
        assertThrows(BusinessException.class, () -> stateMachine.assertCanClose(IncidentStatus.REPORTED));
        assertThrows(BusinessException.class, () -> stateMachine.assertCanClose(IncidentStatus.CLOSED));
    }
}

