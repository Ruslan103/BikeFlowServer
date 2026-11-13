package com.example.demo.repository;

import com.example.demo.model.workorder.WorkOrderDefaultComment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkOrderDefaultCommentRepository extends JpaRepository<WorkOrderDefaultComment, Integer> {
}
