package com.oa.controller;

import com.oa.oa.controller.*;
import com.oa.system.controller.SysDeptController;
import com.oa.system.controller.SysRoleController;
import com.oa.system.controller.SysUserController;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.RequestMapping;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ControllerRouteContractTest {

    @Test
    void controllerRoutesShouldMatchFrontendContracts() {
        assertRoute(OaAnnouncementController.class, "/oa/announcement");
        assertRoute(OaDocFlowController.class, "/oa/docFlow");
        assertRoute(OaIncomingDocController.class, "/oa/incomingDoc");
        assertRoute(OaLeaveController.class, "/oa/leave");
        assertRoute(OaOutgoingDocController.class, "/oa/outgoingDoc");
        assertRoute(OaReimbursementController.class, "/oa/reimbursement");
        assertRoute(OaScheduleController.class, "/oa/schedule");
        assertRoute(OaTodoController.class, "/oa/todo");
        assertRoute(SysUserController.class, "/system/user");
        assertRoute(SysRoleController.class, "/system/role");
        assertRoute(SysDeptController.class, "/system/dept");
    }

    private void assertRoute(Class<?> controller, String expected) {
        RequestMapping mapping = controller.getAnnotation(RequestMapping.class);
        assertEquals(expected, mapping.value()[0], controller.getSimpleName());
    }
}