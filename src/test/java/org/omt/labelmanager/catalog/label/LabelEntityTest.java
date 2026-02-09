package org.omt.labelmanager.catalog.label;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LabelEntityTest {

    @Test
    void setters_updateFields() {
        var entity = new LabelEntity("Original", "old@email.com", "https://old.com");

        entity.setName("Updated");
        entity.setEmail("new@email.com");
        entity.setWebsite("https://new.com");

        assertThat(entity.getName()).isEqualTo("Updated");
        assertThat(entity.getEmail()).isEqualTo("new@email.com");
        assertThat(entity.getWebsite()).isEqualTo("https://new.com");
    }
}
