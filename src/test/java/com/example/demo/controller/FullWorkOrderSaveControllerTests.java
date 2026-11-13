package com.example.demo.controller;

import com.example.demo.dto.WorkOrderResponse;
import com.example.demo.service.client.ContactTypeService;
import com.example.demo.service.workorder.WorkOrderNumberService;
import com.example.demo.service.workorder.WorkOrderService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class FullWorkOrderSaveControllerTests {
    private final WorkOrderService workOrders = mock(WorkOrderService.class);
    private final ContactTypeService types = mock(ContactTypeService.class);
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(
            new WorkOrderController(mock(WorkOrderNumberService.class), workOrders), new ContactTypeController(types))
            .setControllerAdvice(new ApiExceptionHandler()).build();
    @Test void acceptsSelectedCardIdsAndVersions() throws Exception {
        WorkOrderResponse response = mock(WorkOrderResponse.class);
        when(response.blankNumber()).thenReturn("MANUAL-01");when(workOrders.create(any())).thenReturn(response);
        mvc.perform(post("/api/work-orders").contentType(MediaType.APPLICATION_JSON).content("""
                {"clientId":1,"clientBicycleId":2,"services":[],"statusCode":"NEW","blankNumber":"MANUAL-01",
                 "snapshotAction":"REFRESH","expectedClientVersion":3,"expectedBicycleVersion":4}
                """))
                .andExpect(status().isCreated()).andExpect(header().string("Location", "/api/work-orders/MANUAL-01"));
        verify(workOrders).create(argThat(request -> request.clientId()==1L && request.expectedClientVersion()==3L && request.details()==null));
    }
    @Test void invalidOrderStructureReturnsValidationErrors() throws Exception {
        for (String body : List.of("{}", "{\"services\":[null],\"statusCode\":\"NEW\"}", "{\"services\":[],\"statusCode\":\"\"}")) {
            mvc.perform(post("/api/work-orders").contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isBadRequest());
        }
        verifyNoInteractions(workOrders);
    }
    @Test void listsContactTypes() throws Exception {
        when(types.findAll()).thenReturn(List.of(new com.example.demo.dto.client.ContactTypeResponse(1L, "EMAIL", "Почта")));
        mvc.perform(get("/api/contact-types")).andExpect(status().isOk()).andExpect(jsonPath("$[0].code").value("EMAIL"));
    }
}
