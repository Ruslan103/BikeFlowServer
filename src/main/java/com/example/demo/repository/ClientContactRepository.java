package com.example.demo.repository;

import com.example.demo.model.client.ClientContact;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClientContactRepository extends JpaRepository<ClientContact, Long> {
    void deleteAllByClientId(Long clientId);
    List<ClientContact> findByClientIdOrderByIdAsc(Long clientId);

    @org.springframework.data.jpa.repository.Query(value = """
            select exists(select 1 from client_contacts c join contact_types t on t.id=c.contact_type
                where c.client <> :clientId and upper(t.code)=upper(:code) and
                (case when upper(t.code)='PHONE' then regexp_replace(c.contact_value,'[^0-9]','','g')
                    else lower(c.contact_value) end) =
                (case when upper(:code)='PHONE' then regexp_replace(:value,'[^0-9]','','g') else lower(:value) end))
            """, nativeQuery = true)
    boolean existsMatchingOtherClient(@org.springframework.data.repository.query.Param("code") String code,
            @org.springframework.data.repository.query.Param("value") String value,
            @org.springframework.data.repository.query.Param("clientId") Long clientId);
    @EntityGraph(attributePaths = "type")
    List<ClientContact> findByClientIdInOrderByIdAsc(Collection<Long> clientIds);
}
