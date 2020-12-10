package org.openmrs.module.fhir2.api.translators.impl;

import org.hl7.fhir.r4.model.Timing;
import org.openmrs.DrugOrder;
import org.openmrs.module.fhir2.api.mappings.DurationUnitMap;
import org.openmrs.module.fhir2.api.translators.DurationUnitTranslator;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.Nonnull;

public class DurationUnitTranslatorImpl implements DurationUnitTranslator {

	@Autowired
	private DurationUnitMap durationUnitMap;

	@Override
	public Timing.UnitsOfTime toFhirResource(@Nonnull DrugOrder drugOrder) {

		if (drugOrder.getDurationUnits().getUuid() == null) {
			return null;
		}

		return durationUnitMap.getDurationUnit(drugOrder.getDurationUnits().getUuid());

	}
}
