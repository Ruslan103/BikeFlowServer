package com.example.demo.service.client;

import com.example.demo.repository.ClientBicycleRepository;
import com.example.demo.repository.ClientContactRepository;
import com.example.demo.repository.ClientRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ClientSearchServiceTests {
    private final ClientRepository clients = mock(ClientRepository.class);
    private final ClientBicycleRepository bicycles = mock(ClientBicycleRepository.class);
    private final ClientContactRepository contacts = mock(ClientContactRepository.class);
    private final ClientSearchService service = new ClientSearchServiceImpl(clients, bicycles, contacts);

    @Test
    void normalizesPhoneAndSkipsFirstThreeDigits() {
        when(clients.findByPhoneSuffix("91234567")).thenReturn(List.of());
        assertTrue(service.findByPhone("+7 (999) 123-45-67").isEmpty());
        verify(clients).findByPhoneSuffix("91234567");
        verifyNoInteractions(bicycles, contacts);
    }

    @Test
    void rejectsMissingShortAndInvalidPhoneWithoutQueryingDatabase() {
        for (String phone : new String[]{null, "", "   ", "+7 (99)", "---", "test1234567890", "a@1234567890", "1".repeat(256)}) {
            var exception = assertThrows(ResponseStatusException.class, () -> service.findByPhone(phone));
            assertEquals(400, exception.getStatusCode().value());
        }
        verifyNoInteractions(clients, bicycles, contacts);
    }
}
