package app;

import adapters.viewmodel.GameViewModel;
import adapters.viewmodel.ViewState;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class ClientAppBuilderTest {
    @Test
    void rejectsOffEventThreadConstruction() {
        assertThrows(IllegalStateException.class, () ->
                new ClientAppBuilder().buildLocalGame(null, null, new ViewState<>(GameViewModel.empty())));
    }
}
