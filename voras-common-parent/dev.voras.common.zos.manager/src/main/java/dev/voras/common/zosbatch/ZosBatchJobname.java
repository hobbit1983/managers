package dev.voras.common.zosbatch;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import dev.voras.framework.spi.ValidAnnotatedFields;

/**
 * Requests a unique provisioned Jobname for running Jobs or STCs on a zOS Image.
 * 
 * <p>Used to populate a {@link IZosBatchJobname} field</p>
 * 
 * @author Michael Baylis
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
@ZosBatchField
@ValidAnnotatedFields({ IZosBatchJobname.class })
public @interface ZosBatchJobname {
	
	/**
	 * The tag of the zOS Image this variable is to be populated with
	 */
	String imageTag() default "primary";
}
