package org.obeonetwork.capella.sysmlv2.loader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class ModelLoadResultTest {

    @Test
    void loadRequestStoresProjectAndRelativeModelPath() {
        var request = new ModelLoadRequest(Path.of("project"), Path.of("model.capella"));

        assertThat(request.projectDirectory()).isEqualTo(Path.of("project"));
        assertThat(request.modelPath()).isEqualTo(Path.of("model.capella"));
    }

    @Test
    void rejectsAbsoluteModelPath() {
        assertThatIllegalArgumentException().isThrownBy(() ->
                new ModelLoadRequest(Path.of("project"), Path.of("/tmp/model.capella")));
    }
}
