/*
 * Licensed Materials - Property of IBM
 * 
 * (c) Copyright IBM Corp. 2019.
 */
package dev.galasa.zosbatch;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import dev.galasa.framework.spi.ValidAnnotatedFields;

/**
 * Requests access to the zOS Batch Manager
 * 
 * <p>Used to populate a {@link IZosBatch} field</p>
 * 
 * @author Michael Baylis
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
@ZosBatchField
@ValidAnnotatedFields({ IZosBatch.class })
public @interface ZosBatch {
	
	/**
	 * The tag of the zOS Image this variable is to be populated with
	 */
	String imageTag() default "primary";
}