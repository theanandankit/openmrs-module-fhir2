/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.fhir2;

import static org.openmrs.module.fhir2.FhirConstants.FHIR2_MODULE_ID;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import ca.uhn.fhir.rest.server.IResourceProvider;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.openmrs.GlobalProperty;
import org.openmrs.api.GlobalPropertyListener;
import org.openmrs.api.context.Context;
import org.openmrs.module.BaseModuleActivator;
import org.openmrs.module.Module;
import org.openmrs.module.ModuleException;
import org.openmrs.module.ModuleFactory;
import org.openmrs.module.fhir2.api.FhirService;
import org.openmrs.module.fhir2.api.dao.FhirDao;
import org.openmrs.module.fhir2.api.spi.ServiceClassLoader;
import org.openmrs.module.fhir2.api.translators.FhirTranslator;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.stereotype.Component;

/**
 * This class contains the logic that is run every time this module is either started or shutdown
 */
@Slf4j
@Component
@SuppressWarnings("unused")
public class FhirActivator extends BaseModuleActivator implements ApplicationContextAware {
	
	private static ApplicationContext applicationContext;
	
	private final Map<String, Set<Class<?>>> services = new HashMap<>();

	private boolean started = false;

	// this is volatile so that writes and reads are ordered, as we use it's presence or absence to determine how we
	// respond to certain events
	private volatile AnnotationConfigApplicationContext childApplicationContext;
	
	@Override
	public void started() {
		if (applicationContext == null) {
			throw new ModuleException("Cannot load FHIR2 module as the main application context is not available");
		}
		
		// we use a child application context to avoid polluting the main application context with any registered
		// ResourceProviders, etc.
		childApplicationContext = new AnnotationConfigApplicationContext();
		childApplicationContext.setParent(applicationContext);
		
		loadModules();
		
		childApplicationContext.start();

		started = true;

		log.info("Started FHIR");
	}
	
	@Override
	public void contextRefreshed() {
		if (!started) {
			return;
		}

		loadModules();
		reloadContext();
	}
	
	@Override
	public void stopped() {
		childApplicationContext.stop();
		childApplicationContext = null;

		started = false;

		log.info("Shutdown FHIR");
	}
	
	public ConfigurableApplicationContext getApplicationContext() {
		if (childApplicationContext == null) {
			throw new IllegalStateException("This method cannot be called before the module is started");
		}
		
		return childApplicationContext;
	}
	
	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		FhirActivator.applicationContext = applicationContext;
	}
	
	protected void loadModules() {
		ModuleFactory.getLoadedModules().stream()
		        // only try to load services from modules that have the FHIR2 module available on their ModuleClasspath
		        .filter(m -> m.getRequiredModuleVersion(FHIR2_MODULE_ID) != null
		                || m.getAwareOfModuleVersion(FHIR2_MODULE_ID) != null)
		        .forEach(this::loadModuleInternal);
		reloadContext();
	}
	
	protected void loadModule(Module module) {
		if (!services.containsKey(module.getName())) {
			loadModuleInternal(module);
			reloadContext();
		}
	}
	
	protected void unloadModule(String moduleName) {
		if (services.containsKey(moduleName)) {
			services.remove(moduleName);
			reloadContext();
		}
	}
	
	protected void reloadContext() {
		if (childApplicationContext != null) {
			childApplicationContext.refresh();
			registerBeans();
		}
	}
	
	protected void registerBeans() {
		if (childApplicationContext != null) {
			childApplicationContext.register(services.values().stream().reduce(new HashSet<>(), (s, v) -> {
				s.addAll(v);
				return s;
			}).toArray(new Class<?>[0]));
		}
	}
	
	private void loadModuleInternal(Module module) {
		ClassLoader cl = ModuleFactory.getModuleClassLoader(module);
		
		Set<Class<?>> moduleServices = services.computeIfAbsent(module.getName(), k -> new HashSet<>());
		Stream.of(FhirDao.class, FhirTranslator.class, FhirService.class, IResourceProvider.class)
		        .flatMap(c -> new ServiceClassLoader<>(c, cl).load().stream()).filter(c -> {
			        boolean result;
			        try {
				        result = c.getAnnotation(Component.class) != null;
			        }
			        catch (NullPointerException e) {
				        result = false;
			        }
			        
			        if (!result) {
				        log.warn("Skipping {} as it is not an annotated Spring Component", c);
			        }
			        
			        return result;
		        }).forEach(moduleServices::add);
	}
}
