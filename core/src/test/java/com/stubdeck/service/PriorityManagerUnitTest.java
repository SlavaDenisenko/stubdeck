package com.stubdeck.service;

import com.stubdeck.model.PriorityBatchItem;
import com.stubdeck.model.WireMockMapping;
import com.stubdeck.service.source.MappingSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static com.stubdeck.support.TestUtils.mapping;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PriorityManagerUnitTest {

    @Mock
    MappingSource mappingSource;

    PriorityManager priorityManager;

    @BeforeEach
    void setUp() {
        priorityManager = new PriorityManager(mappingSource);
    }

    @Test
    void updateBatch_setsExplicitPriorities() {
        WireMockMapping m1 = mapping("id-1");
        WireMockMapping m2 = mapping("id-2");
        when(mappingSource.loadAll()).thenReturn(List.of(m1, m2));

        priorityManager.updateBatch(List.of(
                batchItem("id-1", 5),
                batchItem("id-2", 10)
        ));

        ArgumentCaptor<WireMockMapping> captor = ArgumentCaptor.forClass(WireMockMapping.class);
        verify(mappingSource, times(2)).save(captor.capture());

        List<WireMockMapping> saved = captor.getAllValues();
        assertThat(saved).anySatisfy(m -> {
            assertThat(m.getId()).isEqualTo("id-1");
            assertThat(m.getPriority()).isEqualTo(5);
        });
        assertThat(saved).anySatisfy(m -> {
            assertThat(m.getId()).isEqualTo("id-2");
            assertThat(m.getPriority()).isEqualTo(10);
        });
    }

    @Test
    void updateBatch_unknownId_skipped() {
        when(mappingSource.loadAll()).thenReturn(List.of(mapping("id-1")));

        priorityManager.updateBatch(List.of(batchItem("nonexistent", 3)));

        verify(mappingSource, never()).save(any());
    }

    @Test
    void updateBatch_emptyList_noSaveCalls() {
        when(mappingSource.loadAll()).thenReturn(List.of(mapping("id-1")));

        priorityManager.updateBatch(List.of());

        verify(mappingSource, never()).save(any());
    }

    @Test
    void updatePriorities_unknownId_skipped() {
        when(mappingSource.loadAll()).thenReturn(List.of(mapping("id-1")));

        priorityManager.updatePriorities(List.of("id-1", "nonexistent"));

        ArgumentCaptor<WireMockMapping> captor = ArgumentCaptor.forClass(WireMockMapping.class);
        verify(mappingSource, times(1)).save(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo("id-1");
        assertThat(captor.getValue().getPriority()).isEqualTo(1);
    }

    @Test
    void updatePriorities_assignsPositionBasedIndex() {
        WireMockMapping m1 = mapping("id-1");
        WireMockMapping m2 = mapping("id-2");
        WireMockMapping m3 = mapping("id-3");
        when(mappingSource.loadAll()).thenReturn(List.of(m1, m2, m3));

        priorityManager.updatePriorities(List.of("id-3", "id-1", "id-2"));

        verify(mappingSource, times(3)).save(any());
        assertThat(m3.getPriority()).isEqualTo(1);
        assertThat(m1.getPriority()).isEqualTo(2);
        assertThat(m2.getPriority()).isEqualTo(3);
    }

    private PriorityBatchItem batchItem(String id, int priority) {
        PriorityBatchItem item = new PriorityBatchItem();
        item.setId(id);
        item.setPriority(priority);
        return item;
    }
}
