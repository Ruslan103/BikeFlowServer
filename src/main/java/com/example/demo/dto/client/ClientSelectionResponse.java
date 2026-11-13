package com.example.demo.dto.client;

import com.example.demo.model.workorder.WorkOrderClientSnapshot;

public record ClientSelectionResponse(WorkOrderClientSnapshot snapshot, Long expectedClientVersion, Long expectedBicycleVersion) { }
