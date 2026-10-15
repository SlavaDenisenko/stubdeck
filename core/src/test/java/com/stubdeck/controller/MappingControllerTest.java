package com.stubdeck.controller;

import com.stubdeck.config.WireMockProperties;
import com.stubdeck.model.SaveResult;
import com.stubdeck.service.access.AccessGuard;
import com.stubdeck.service.ConflictDetector;
import com.stubdeck.service.context.instance.InstanceContext;
import com.stubdeck.service.context.user.UserContext;
import com.stubdeck.service.changes.ChangeTracker;
import com.stubdeck.service.MappingService;
import com.stubdeck.service.PriorityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.stubdeck.support.TestUtils.mapping;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MappingController.class)
class MappingControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    MappingService mappingService;

    @MockitoBean
    ConflictDetector conflictDetector;

    @MockitoBean
    PriorityManager priorityManager;

    @MockitoBean
    ChangeTracker changeTracker;

    @MockitoBean
    WireMockProperties properties;

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
        when(properties.getConflict()).thenReturn(new WireMockProperties.Conflict());
        when(accessGuard.canEdit()).thenReturn(true);
        when(accessGuard.canDelete()).thenReturn(true);
        when(accessGuard.canPersist()).thenReturn(true);
    }

    @Test
    void getAllMappings_returnsJsonList() throws Exception {
        when(mappingService.getAllMappings()).thenReturn(List.of(mapping("id-1", "GET", "/api/test")));

        mockMvc.perform(get("/api/mappings").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("id-1"));
    }

    @Test
    void getAllMappings_emptyList_returnsEmptyArray() throws Exception {
        when(mappingService.getAllMappings()).thenReturn(List.of());

        mockMvc.perform(get("/api/mappings").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void getConflicts_noConflicts_returnsEmptyMap() throws Exception {
        when(mappingService.getAllMappings()).thenReturn(List.of());
        when(conflictDetector.findConflicts(any())).thenReturn(Map.of());

        mockMvc.perform(get("/api/mappings/conflicts").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isMap());
    }

    @Test
    void getById_found_returns200() throws Exception {
        when(mappingService.getMappingById("id-1")).thenReturn(Optional.of(mapping("id-1", "GET", "/api/test")));

        mockMvc.perform(get("/api/mappings/id-1").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("id-1"));
    }

    @Test
    void getById_notFound_returns404() throws Exception {
        when(mappingService.getMappingById("missing")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/mappings/missing").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    void createMapping_success_returnsSavedTrue() throws Exception {
        when(mappingService.save(any(), eq(false))).thenReturn(SaveResult.ok());

        mockMvc.perform(post("/api/mappings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"request\":{\"method\":\"GET\",\"url\":\"/api/test\"},\"response\":{\"status\":200}}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.saved").value(true))
                .andExpect(jsonPath("$.hasConflict").value(false));
    }

    @Test
    void createMapping_conflictBlocked_returnsSavedFalse() throws Exception {
        when(mappingService.save(any(), eq(false))).thenReturn(SaveResult.conflict("GET /api/test"));

        mockMvc.perform(post("/api/mappings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"request\":{\"method\":\"GET\",\"url\":\"/api/test\"}}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.saved").value(false))
                .andExpect(jsonPath("$.hasConflict").value(true))
                .andExpect(jsonPath("$.conflictKey").value("GET /api/test"));
    }

    @Test
    void createMapping_forceFlag_passedToService() throws Exception {
        when(mappingService.save(any(), eq(true))).thenReturn(SaveResult.warnAndSave("GET /api/test"));

        mockMvc.perform(post("/api/mappings")
                        .param("force", "true")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"request\":{\"method\":\"GET\",\"url\":\"/api/test\"}}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.saved").value(true))
                .andExpect(jsonPath("$.hasConflict").value(true));
    }

    @Test
    void updateMapping_setsIdFromPath() throws Exception {
        when(mappingService.save(any(), eq(false))).thenReturn(SaveResult.ok());

        mockMvc.perform(put("/api/mappings/stub-99/edit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"request\":{\"method\":\"POST\",\"url\":\"/api/data\"}}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.saved").value(true));

        verify(mappingService).save(argThat(m -> "stub-99".equals(m.getId())), eq(false));
    }

    @Test
    void deleteMapping_success_returns204() throws Exception {
        mockMvc.perform(delete("/api/mappings/id-1"))
                .andExpect(status().isNoContent());

        verify(mappingService).delete("id-1");
    }

    @Test
    void deleteMapping_notFound_returns404() throws Exception {
        doThrow(new IllegalArgumentException("Mapping not found: missing"))
                .when(mappingService).delete("missing");

        mockMvc.perform(delete("/api/mappings/missing"))
                .andExpect(status().isNotFound());
    }

    @Test
    void parseJson_validInput_returnsMapping() throws Exception {
        when(mappingService.parseRawJson(any())).thenReturn(mapping("id-1", "GET", "/api/test"));

        mockMvc.perform(post("/api/mappings/parse")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"json\":\"{}\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("id-1"));
    }

    @Test
    void parseJson_invalidJson_returns400() throws Exception {
        when(mappingService.parseRawJson(any())).thenThrow(new IOException("bad json"));

        mockMvc.perform(post("/api/mappings/parse")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"json\":\"not-json\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getTemplates_returnsNonEmptyList() throws Exception {
        mockMvc.perform(get("/api/mappings/templates").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").exists())
                .andExpect(jsonPath("$[0].label").exists());
    }

    @Test
    void updatePriority_delegatesToPriorityManager() throws Exception {
        mockMvc.perform(post("/api/mappings/priority")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderedIds\":[\"id-2\",\"id-1\"]}"))
                .andExpect(status().isOk());

        verify(priorityManager).updatePriorities(List.of("id-2", "id-1"));
    }

    @Test
    void updatePriorityBatch_delegatesToPriorityManager() throws Exception {
        mockMvc.perform(post("/api/mappings/priority/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[{\"id\":\"id-1\",\"priority\":3}]"))
                .andExpect(status().isOk());

        verify(priorityManager).updateBatch(anyList());
    }

    @Test
    void persist_delegatesToMappingService() throws Exception {
        mockMvc.perform(post("/api/mappings/persist"))
                .andExpect(status().isOk());

        verify(mappingService).persist();
    }
}
