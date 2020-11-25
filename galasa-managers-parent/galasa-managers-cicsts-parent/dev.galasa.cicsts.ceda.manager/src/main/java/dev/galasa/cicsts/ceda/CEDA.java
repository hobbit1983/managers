/*
 * Licensed Materials - Property of IBM
 * 
 * (c) Copyright IBM Corp. 2020.
 */
package dev.galasa.cicsts.ceda;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import dev.galasa.framework.spi.ValidAnnotatedFields;
import dev.galasa.zos3270.ITerminal;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
@CEDAManagerField
@ValidAnnotatedFields({ ICEDA.class })
public @interface CEDA {
    
}