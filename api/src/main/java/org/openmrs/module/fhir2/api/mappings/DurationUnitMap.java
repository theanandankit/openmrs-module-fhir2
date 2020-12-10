package org.openmrs.module.fhir2.api.mappings;

import lombok.extern.slf4j.Slf4j;
import org.hibernate.HibernateException;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Projections;
import org.hl7.fhir.r4.model.Timing;
import org.openmrs.module.fhir2.model.FhirDurationUnitMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;

import static org.hibernate.criterion.Restrictions.eq;

@Component
@Slf4j
public class DurationUnitMap {

	@Autowired
	@Qualifier("sessionFactory")
	private SessionFactory sessionFactory;

	public Timing.UnitsOfTime getDurationUnit(@Nonnull String conceptUuid) {

		try {
			return (Timing.UnitsOfTime) sessionFactory.getCurrentSession().createCriteria(FhirDurationUnitMap.class)
					.createAlias("concept", "c").add(eq("c.uuid", conceptUuid))
					.setProjection(Projections.property("unitOfTime")).uniqueResult();
		}
		catch (HibernateException e) {
			log.error("Exception caught while trying to load DurationUnit for concept '{}'", conceptUuid, e);
		}

		return null;
	}

}
