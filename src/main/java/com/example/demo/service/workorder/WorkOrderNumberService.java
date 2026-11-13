package com.example.demo.service.workorder;

import com.example.demo.repository.WorkOrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class WorkOrderNumberService {

    private final WorkOrderRepository workOrderRepository;

    public String generate() {
        log.info("Начата генерация номера бланка");
        while (true) {
            Long sequenceNumber = workOrderRepository.nextBlankNumberSequenceValue();
            if (sequenceNumber == null) {
                log.error("Не удалось получить следующее значение последовательности бланков");
                throw new IllegalStateException("Could not generate a work order number");
            }
            String number = "%05d".formatted(sequenceNumber);
            if (!workOrderRepository.existsByBlankNumber(number)) {
                log.info("Номер бланка успешно сгенерирован");
                return number;
            }
        }
    }
}
