package com.example.demo.repository;

import com.example.demo.model.client.Client;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ClientRepository extends JpaRepository<Client, Long> {
    @Query(value = """
            select client.*
            from clients client
            left join lateral (
                select contact.contact_value
                from client_contacts contact
                join contact_types contact_type on contact_type.id = contact.contact_type
                where contact.client = client.id and btrim(contact.contact_value) <> ''
                order by case when upper(contact_type.code) = 'PHONE' then 0 else 1 end, contact.id
                limit 1
            ) selected_contact on true
            cross join lateral (
                select coalesce(
                    nullif(btrim(client.last_name), ''),
                    nullif(btrim(client.first_name), ''),
                    nullif(btrim(client.middle_name), ''),
                    nullif(btrim(client.organization_name), ''),
                    selected_contact.contact_value,
                    ''
                ) as sort_value
            ) client_sort
            order by
                case when client_sort.sort_value ~ '[A-Za-zА-Яа-яЁё]' then 0 else 1 end,
                translate(lower(client_sort.sort_value), 'ё', 'е'),
                client.id
            """, countQuery = "select count(*) from clients", nativeQuery = true)
    Page<Client> findAllAlphabetically(Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select client from Client client where client.id = :id")
    Optional<Client> findByIdForUpdate(@Param("id") Long id);

    @Query(value = """
            select c.* from clients c where exists(select 1 from client_contacts x
                join contact_types t on t.id=x.contact_type where x.client=c.id and upper(t.code)='PHONE'
                and regexp_replace(x.contact_value,'[^0-9]','','g')=:phone) order by c.id
            """, nativeQuery = true)
    List<Client> findByExactPhone(@Param("phone") String phone);

    @Query("""
            select c from Client c where locate(lower(:query),lower(coalesce(c.firstName,'')))>0
                or locate(lower(:query),lower(coalesce(c.lastName,'')))>0
                or locate(lower(:query),lower(coalesce(c.middleName,'')))>0
                or locate(lower(:query),lower(coalesce(c.organizationName,'')))>0
                or locate(lower(:query),lower(coalesce(c.representative,'')))>0
                or exists(select x.id from ClientContact x where x.client=c and locate(lower(:query),lower(x.value))>0)
            order by c.id
            """)
    List<Client> searchText(@Param("query") String query);

    @Query(value = """
            select client.*
            from clients client
            where lower(:query) <% lower(coalesce(client.first_name, ''))
               or lower(:query) <% lower(coalesce(client.last_name, ''))
               or lower(:query) <% lower(coalesce(client.middle_name, ''))
               or lower(:query) <% lower(coalesce(client.organization_name, ''))
               or lower(:query) <% lower(coalesce(client.representative, ''))
               or lower(:query) <% lower(coalesce(client.last_name, '') || ' '
                    || coalesce(client.first_name, '') || ' ' || coalesce(client.middle_name, ''))
               or exists (
                    select 1 from client_contacts contact
                    where contact.client = client.id
                      and lower(:query) <% lower(coalesce(contact.contact_value, ''))
               )
            order by greatest(
                word_similarity(lower(:query), lower(coalesce(client.first_name, ''))),
                word_similarity(lower(:query), lower(coalesce(client.last_name, ''))),
                word_similarity(lower(:query), lower(coalesce(client.middle_name, ''))),
                word_similarity(lower(:query), lower(coalesce(client.organization_name, ''))),
                word_similarity(lower(:query), lower(coalesce(client.representative, ''))),
                word_similarity(lower(:query), lower(coalesce(client.last_name, '') || ' '
                    || coalesce(client.first_name, '') || ' ' || coalesce(client.middle_name, ''))),
                coalesce((
                    select max(word_similarity(lower(:query), lower(contact.contact_value)))
                    from client_contacts contact where contact.client = client.id
                ), 0)
            ) desc, client.id
            """, nativeQuery = true)
    List<Client> searchTextWithTypos(@Param("query") String query);

    @Query(value = """
            select distinct client.*
            from clients client
            join client_contacts contact on contact.client = client.id
            join contact_types contact_type on contact_type.id = contact.contact_type
            where upper(contact_type.code) = 'PHONE'
              and :phoneSuffix <% substring(regexp_replace(contact.contact_value, '[^0-9]', '', 'g') from 4)
            order by client.id
            """, nativeQuery = true)
    List<Client> findBySimilarPhoneSuffix(@Param("phoneSuffix") String phoneSuffix);
    @Query(value = "select nextval('client_public_id_seq')", nativeQuery = true)
    Long nextPublicId();
    @Query(value = """
            select client.* from clients client
            where exists (
                select 1 from client_contacts contact
                join contact_types type on type.id = contact.contact_type
                where contact.client = client.id and upper(type.code) = 'PHONE'
                    and substring(regexp_replace(contact.contact_value, '[^0-9]', '', 'g') from 4) = :suffix
            )
            order by client.last_name, client.first_name, client.middle_name, client.id
            """, nativeQuery = true)
    List<Client> findByPhoneSuffix(@Param("suffix") String suffix);
}
