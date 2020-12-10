package org.openmrs.module.fhir2.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.search.annotations.Field;
import org.hl7.fhir.r4.model.Timing;
import org.openmrs.Auditable;
import org.openmrs.Concept;
import org.openmrs.Retireable;
import org.openmrs.User;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.util.Date;
import java.util.UUID;

@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
@Entity
@Table(name = "fhir_duration_unit_map")
public class FhirDurationUnitMap implements Auditable, Retireable {

	@EqualsAndHashCode.Include
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "duration_unit_map_id")
	private Integer id;

	@ManyToOne(optional = false)
	@JoinColumn(nullable = false, name = "concept_id")
	private Concept concept;

	@Column(nullable = false, name = "unit_of_time")
	private Timing.UnitsOfTime unitsOfTime;

	@ManyToOne(optional = false)
	@JoinColumn(name = "creator", updatable = false)
	protected User creator;

	@Column(name = "date_created", nullable = false, updatable = false)
	private Date dateCreated;

	@ManyToOne
	@JoinColumn(name = "changed_by")
	private User changedBy;

	@Column(name = "date_changed")
	private Date dateChanged;

	@Column(name = "retired", nullable = false)
	@Field
	private Boolean retired = Boolean.FALSE;

	@Column(name = "date_retired")
	private Date dateRetired;

	@ManyToOne
	@JoinColumn(name = "retired_by")
	private User retiredBy;

	@Column(name = "retire_reason")
	private String retireReason;

	@Column(name = "uuid", unique = true, nullable = false, length = 36)
	private String uuid = UUID.randomUUID().toString();

	@Override
	public Boolean isRetired() {
		return retired;
	}
}
