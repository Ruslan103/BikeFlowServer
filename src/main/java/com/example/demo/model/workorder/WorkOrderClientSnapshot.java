package com.example.demo.model.workorder;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public record WorkOrderClientSnapshot(
        @Column(name = "snapshot_client_type") String clientType,
        @Column(name = "snapshot_display_name", length = 400) String displayName,
        @Column(name = "snapshot_first_name") String firstName,
        @Column(name = "snapshot_last_name") String lastName,
        @Column(name = "snapshot_middle_name") String middleName,
        @Column(name = "snapshot_organization_name", length = 250) String organizationName,
        @Column(name = "snapshot_representative", length = 250) String representative,
        @Column(name = "snapshot_contact_type") String contactTypeCode,
        @Column(name = "snapshot_contact_value", length = 255) String contactValue,
        @Column(name = "snapshot_bicycle_brand") String bicycleBrand,
        @Column(name = "snapshot_bicycle_comment", columnDefinition = "TEXT") String bicycleComment,
        @Column(name = "snapshot_client_comment", columnDefinition = "TEXT") String clientComment) { }
