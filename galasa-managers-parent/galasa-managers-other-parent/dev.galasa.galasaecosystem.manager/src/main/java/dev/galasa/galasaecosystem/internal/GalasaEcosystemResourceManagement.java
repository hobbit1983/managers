/*
 * Licensed Materials - Property of IBM
 * 
 * (c) Copyright IBM Corp. 2020.
 */
package dev.galasa.galasaecosystem.internal;

import java.util.concurrent.TimeUnit;

import org.osgi.service.component.annotations.Component;

import dev.galasa.framework.spi.IConfigurationPropertyStoreService;
import dev.galasa.framework.spi.IDynamicStatusStoreService;
import dev.galasa.framework.spi.IFramework;
import dev.galasa.framework.spi.IResourceManagement;
import dev.galasa.framework.spi.IResourceManagementProvider;
import dev.galasa.framework.spi.ResourceManagerException;
import dev.galasa.galasaecosystem.internal.properties.GalasaEcosystemPropertiesSingleton;

/**
 * Initialise the Resource Management routines for the Galasa Ecosystem Manager 
 * 
 * @author Michael Baylis
 *
 */
@Component(service= {IResourceManagementProvider.class})
public class GalasaEcosystemResourceManagement implements IResourceManagementProvider {

	private IFramework                         framework;
	private IResourceManagement                resourceManagement;
	private IDynamicStatusStoreService         dss;
	private IConfigurationPropertyStoreService cps;
	
	private RunResourceMonitor runResourceMonitor;
	
	@Override
	public boolean initialise(IFramework framework, IResourceManagement resourceManagement) throws ResourceManagerException {
		this.framework = framework;
		this.resourceManagement = resourceManagement;
		try {
			this.dss = this.framework.getDynamicStatusStoreService(GalasaEcosystemManagerImpl.NAMESPACE);
			this.cps = this.framework.getConfigurationPropertyService(GalasaEcosystemManagerImpl.NAMESPACE);
			GalasaEcosystemPropertiesSingleton.setCps(cps);
		} catch (Exception e) {
			throw new ResourceManagerException("Unable to initialise Galasa Ecosystem resource monitor", e);
		}
		
		runResourceMonitor = new RunResourceMonitor(framework, resourceManagement, dss, this, cps);
		
		return true;
	}

	@Override
	public void start() {
		this.resourceManagement.getScheduledExecutorService().scheduleWithFixedDelay(runResourceMonitor, 
				this.framework.getRandom().nextInt(20),
				60, 
				TimeUnit.SECONDS);
	}

	@Override
	public void shutdown() {
	}

	@Override
	public void runFinishedOrDeleted(String runName) {
		this.runResourceMonitor.runFinishedOrDeleted(runName);
	}

}
