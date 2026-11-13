package com.example.demo.repository;

import com.example.demo.model.workorder.WorkOrder;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

public interface WorkOrderRepository extends JpaRepository<WorkOrder, Long> {
    boolean existsByBlankNumberAndIdNot(String blankNumber, Long id);
    boolean existsByBlankNumber(String blankNumber);

    @Query(value = "SELECT nextval('work_order_number_seq')", nativeQuery = true)
    Long nextBlankNumberSequenceValue();

    Optional<WorkOrder> findByBlankNumber(String blankNumber);

    @Query("select workOrder from WorkOrder workOrder where workOrder.client.id = :clientId "
            + "order by workOrder.createdAt desc, workOrder.id desc")
    List<WorkOrder> findAllByClientId(@Param("clientId") Long clientId);

    @Query(value = "SELECT * FROM work_orders WHERE CASE "
            + "WHEN blank_number ~ '^[0-9]+$' "
            + "THEN COALESCE(NULLIF(regexp_replace(blank_number, '^0+', ''), ''), '0') "
            + "ELSE blank_number END = :number ORDER BY created_at DESC, id DESC",
            nativeQuery = true)
    List<WorkOrder> findByNormalizedBlankNumber(@Param("number") String number);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select workOrder from WorkOrder workOrder where workOrder.blankNumber = :number")
    Optional<WorkOrder> findByNumberForUpdate(@Param("number") String number);

    boolean existsByTrelloCardAndIdNot(String cardId, Long id);
    boolean existsByClientBicycleId(Long bicycleId);
    boolean existsByClientId(Long clientId);

    @Modifying
    @Query(value = """
            update work_orders
            set client_contact = case when client_contact in (:contactIds) then null else client_contact end,
                phone_contact = case when phone_contact in (:contactIds) then null else phone_contact end,
                other_contact = case when other_contact in (:contactIds) then null else other_contact end
            where client_contact in (:contactIds)
               or phone_contact in (:contactIds)
               or other_contact in (:contactIds)
            """, nativeQuery = true)
    int clearContactReferences(@Param("contactIds") Collection<Long> contactIds);
}
