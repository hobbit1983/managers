/*
 * Licensed Materials - Property of IBM
 * 
 * (c) Copyright IBM Corp. 2019.
 */
package dev.galasa.zosbatch.zosmf.internal;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import dev.galasa.zos.IZosImage;
import dev.galasa.zosbatch.IZosBatchJobname;
import dev.galasa.zosbatch.ZosBatchException;
import dev.galasa.zosbatch.ZosBatchManagerException;
import dev.galasa.zosbatch.zosmf.internal.properties.JobnamePrefix;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ZosBatchImpl.class, JobnamePrefix.class})
public class TestZosBatchImpl {
	
	private ZosBatchImpl zosBatch;
	
	private ZosBatchImpl zosBatchSpy;

	@Mock
	private IZosImage zosImageMock;

	@Mock
	private ZosBatchJobnameImpl zosJobnameMock;
	
	private static final String FIXED_JOBNAME = "GAL45678";
	
	@Mock
	private ZosBatchJobImpl zosBatchJobMock;
	
	@Before
	public void setup() throws ZosBatchManagerException {
		// Mock the getImageID() method
		Mockito.when(zosImageMock.getImageID()).thenReturn("image");
		
		// Mock ZosBatchImpl#newZosBatchJob() to return the mocked ZosBatchJobImpl
		zosBatch = new ZosBatchImpl(zosImageMock);
		zosBatchSpy = Mockito.spy(zosBatch);
		
		// Mock the mocked ZosBatchJobImpl#submitJob() to return the mocked ZosBatchJobImpl
		Mockito.when(zosBatchJobMock.submitJob()).thenReturn(zosBatchJobMock);
		
		// Mock JobnamePrefix.get() to return a fixed value
		PowerMockito.mockStatic(JobnamePrefix.class);
		Mockito.when(JobnamePrefix.get(Mockito.anyString())).thenReturn(FIXED_JOBNAME);
	}
	
	@Test
	public void testSubmitJobandCleanup() throws ZosBatchException {
		Mockito.doReturn(zosBatchJobMock).when(zosBatchSpy).newZosBatchJob(Mockito.anyString(), Mockito.any());
		Assert.assertEquals("submitJob should return mocked batch job", zosBatchSpy.submitJob("JCL", null), zosBatchJobMock);

		Mockito.when(zosBatchJobMock.submitted()).thenReturn(false);
		zosBatch.cleanup();
		
		Assert.assertEquals("submitJob should return mocked batch job", zosBatchSpy.submitJob("JCL", null), zosBatchJobMock);

		Mockito.when(zosBatchJobMock.submitted()).thenReturn(true);
		Mockito.when(zosBatchJobMock.isArchived()).thenReturn(false);
		Mockito.when(zosBatchJobMock.isPurged()).thenReturn(false);
		zosBatch.cleanup();		
		
		Assert.assertEquals("submitJob should return mocked batch job", zosBatchSpy.submitJob("JCL", zosJobnameMock), zosBatchJobMock);

		Mockito.when(zosBatchJobMock.isArchived()).thenReturn(true);
		Mockito.when(zosBatchJobMock.isPurged()).thenReturn(true);
		zosBatch.cleanup();
	}
	
	@Test
	public void testNewZosBatchJob() throws Exception {
		PowerMockito.whenNew(ZosBatchJobImpl.class).withArguments(Mockito.any(IZosImage.class), Mockito.any(IZosBatchJobname.class), Mockito.anyString()).thenReturn(zosBatchJobMock);
		Whitebox.setInternalState(zosBatchSpy, "image", zosImageMock);
		Assert.assertEquals("Should return the mocked batch job", zosBatchSpy.newZosBatchJob("JCL", zosJobnameMock), zosBatchJobMock);
	}
}