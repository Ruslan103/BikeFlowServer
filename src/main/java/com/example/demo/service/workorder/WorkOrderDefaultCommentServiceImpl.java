package com.example.demo.service.workorder;

import com.example.demo.dto.workorder.WorkOrderDefaultCommentRequest;
import com.example.demo.dto.workorder.WorkOrderDefaultCommentResponse;
import com.example.demo.model.workorder.WorkOrderDefaultComment;
import com.example.demo.repository.WorkOrderDefaultCommentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class WorkOrderDefaultCommentServiceImpl implements WorkOrderDefaultCommentService {
    private final WorkOrderDefaultCommentRepository comments;

    @Override
    public WorkOrderDefaultCommentResponse find() {
        log.info("Получен запрос на чтение комментария к заказу по умолчанию");
        return toResponse(requireComment());
    }

    @Override
    @Transactional
    public WorkOrderDefaultCommentResponse create(WorkOrderDefaultCommentRequest request) {
        log.info("Получен запрос на создание комментария к заказу по умолчанию");
        if (comments.existsById(WorkOrderDefaultComment.SINGLETON_ID)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Комментарий к заказу по умолчанию уже создан");
        }
        WorkOrderDefaultComment comment = new WorkOrderDefaultComment();
        comment.setId(WorkOrderDefaultComment.SINGLETON_ID);
        comment.setComment(request.comment().strip());
        return toResponse(comments.save(comment));
    }

    @Override
    @Transactional
    public WorkOrderDefaultCommentResponse update(WorkOrderDefaultCommentRequest request) {
        log.info("Получен запрос на изменение комментария к заказу по умолчанию");
        WorkOrderDefaultComment comment = requireComment();
        comment.setComment(request.comment().strip());
        return toResponse(comment);
    }

    @Override
    @Transactional
    public void delete() {
        log.info("Получен запрос на удаление комментария к заказу по умолчанию");
        comments.delete(requireComment());
    }

    private WorkOrderDefaultComment requireComment() {
        return comments.findById(WorkOrderDefaultComment.SINGLETON_ID).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Комментарий к заказу по умолчанию не настроен"));
    }

    private WorkOrderDefaultCommentResponse toResponse(WorkOrderDefaultComment comment) {
        return new WorkOrderDefaultCommentResponse(comment.getComment());
    }
}
