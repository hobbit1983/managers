/*
 * Licensed Materials - Property of IBM
 * 
 * (c) Copyright IBM Corp. 2020.
 */
package dev.galasa.zosprogram.internal.properties;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import dev.galasa.framework.spi.ConfigurationPropertyStoreException;
import dev.galasa.framework.spi.IConfigurationPropertyStoreService;
import dev.galasa.framework.spi.cps.CpsProperties;
import dev.galasa.zosprogram.ZosProgramManagerException;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ZosProgramPropertiesSingleton.class, CpsProperties.class})
public class TestLanguageEnvironmentDatasetPrefix {
    
    @Mock
    private IConfigurationPropertyStoreService configurationPropertyStoreServiceMock;
    
    @Rule
    public ExpectedException exceptionRule = ExpectedException.none();
    
    private static final String IMAGE_ID = "IMAGE";
    private static final String PREFIX = "PREFIX";
    private static final List<String> PREFIX_LIST = Arrays.asList(PREFIX);
    
    @Test
    public void testConstructor() {
        LanguageEnvironmentDatasetPrefix languageEnvironmentDatasetPrefix = new LanguageEnvironmentDatasetPrefix();
        Assert.assertNotNull("Object was not created", languageEnvironmentDatasetPrefix);
    }
    
    @Test
    public void testEmpty() throws Exception {
        exceptionRule.expect(ZosProgramManagerException.class);
        exceptionRule.expectMessage("Required property zosprogram.le.[imageid].dataset.prefix not supplied");
        
        getProperty(Collections.emptyList());
    }
    
    @Test
    public void testValid() throws Exception {
        Assert.assertEquals("Unexpected value returned from LanguageEnvironmentDatasetPrefix.get()", PREFIX_LIST, getProperty(PREFIX_LIST));
    }
    
    @Test
    public void testException() throws Exception {
        exceptionRule.expect(ZosProgramManagerException.class);
        exceptionRule.expectMessage("Problem asking the CPS for the zOS LanguageExtended Environment dataset prefix for zOS image " + IMAGE_ID);
        
        getProperty(Arrays.asList("ANY"), true);
    }

    private List<String> getProperty(List<String> prefixList) throws Exception {
        return getProperty(prefixList, false);
    }
    
    private List<String> getProperty(List<String> prefixList, boolean exception) throws Exception {
        PowerMockito.spy(ZosProgramPropertiesSingleton.class);
        PowerMockito.doReturn(configurationPropertyStoreServiceMock).when(ZosProgramPropertiesSingleton.class, "cps");
        PowerMockito.spy(CpsProperties.class);
        
        if (!exception) {
            PowerMockito.doReturn(prefixList).when(CpsProperties.class, "getStringList", Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());            
        } else {
            PowerMockito.doThrow(new ConfigurationPropertyStoreException()).when(CpsProperties.class, "getStringList", Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
        }
        
        return LanguageEnvironmentDatasetPrefix.get(IMAGE_ID);
    }
}