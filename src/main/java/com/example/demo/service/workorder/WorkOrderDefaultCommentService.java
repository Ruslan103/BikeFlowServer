package com.example.demo.service.workorder;

import com.example.demo.dto.workorder.WorkOrderDefaultCommentRequest;
import com.example.demo.dto.workorder.WorkOrderDefaultCommentResponse;

public interface WorkOrderDefaultCommentService {
    WorkOrderDefaultCommentResponse find();
    WorkOrderDefaultCommentResponse create(WorkOrderDefaultCommentRequest request);
    WorkOrderDefaultCommentResponse update(WorkOrderDefaultCommentRequest request);
    void delete();
}
