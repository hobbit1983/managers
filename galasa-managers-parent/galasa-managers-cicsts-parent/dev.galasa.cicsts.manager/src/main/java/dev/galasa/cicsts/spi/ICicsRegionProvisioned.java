/*
 * Licensed Materials - Property of IBM
 * 
 * (c) Copyright IBM Corp. 2020.
 */
package dev.galasa.cicsts.spi;

import dev.galasa.cicsts.ICicsRegion;

public interface ICicsRegionProvisioned extends ICicsRegion {

    String getNextTerminalId();

}
