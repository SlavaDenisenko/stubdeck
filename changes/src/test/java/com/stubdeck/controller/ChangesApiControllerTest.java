package com.stubdeck.controller;

import com.stubdeck.service.access.AccessGuard;
import com.stubdeck.service.context.instance.InstanceContext;
import com.stubdeck.service.context.user.UserContext;
import com.stubdeck.service.MappingService;
import com.stubdeck.service.PendingChangeTracker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ChangesApiController.class)
class ChangesApiControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    PendingChangeTracker tracker;

    @MockitoBean
    MappingService mappingService;

    @MockitoBean
    UserContext userContext;

    @MockitoBean
    AccessGuard accessGuard;

    @MockitoBean
    InstanceContext instanceContext;

    @BeforeEach
    void setUp() {
        when(userContext.getCurrentUser()).thenReturn(Optional.empty());
        when(mappingService.getSourceType()).thenReturn("FILE");
        when(accessGuard.canPersist()).thenReturn(true);
        when(accessGuard.canDiscard()).thenReturn(true);
    }

    @Test
    void discardSelected_callsTrackerAndReturnsOk() throws Exception {
        mockMvc.perform(post("/api/changes/discard-selected")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[\"id-1\",\"id-2\"]"))
                .andExpect(status().isOk());

        verify(tracker).discardSelected(List.of("id-1", "id-2"));
    }

    @Test
    void saveAll_callsTrackerAndReturnsOk() throws Exception {
        mockMvc.perform(post("/api/changes/save-all"))
                .andExpect(status().isOk());

        verify(tracker).persistAll();
    }

    @Test
    void saveSelected_callsTrackerAndReturnsOk() throws Exception {
        mockMvc.perform(post("/api/changes/save-selected")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[\"id-3\"]"))
                .andExpect(status().isOk());

        verify(tracker).persistSelected(List.of("id-3"));
    }

    @Test
    void untrack_callsTrackerAndReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/changes/stub-42"))
                .andExpect(status().isNoContent());

        verify(tracker).untrack("stub-42");
    }
}
