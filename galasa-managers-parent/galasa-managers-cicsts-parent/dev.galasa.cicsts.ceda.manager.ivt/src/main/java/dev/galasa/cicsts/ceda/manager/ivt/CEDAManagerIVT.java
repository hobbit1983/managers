/*
 * Licensed Materials - Property of IBM
 * 
 * (c) Copyright IBM Corp. 2020.
 */
package dev.galasa.cicsts.ceda.manager.ivt;

import static org.assertj.core.api.Assertions.*;

import org.apache.commons.logging.Log;
import org.assertj.core.api.Fail;

import dev.galasa.Before;
import dev.galasa.BeforeClass;
import dev.galasa.Test;
import dev.galasa.cicsts.CicsRegion;
import dev.galasa.cicsts.CicsTerminal;
import dev.galasa.cicsts.ICicsRegion;
import dev.galasa.cicsts.ICicsTerminal;
import dev.galasa.cicsts.ceda.CEDA;
import dev.galasa.cicsts.ceda.CEDAException;
import dev.galasa.cicsts.ceda.ICEDA;
import dev.galasa.cicsts.cemt.CEMT;
import dev.galasa.cicsts.cemt.CEMTException;
import dev.galasa.cicsts.cemt.ICEMT;
import dev.galasa.core.manager.Logger;
import dev.galasa.zos3270.ErrorTextFoundException;
import dev.galasa.zos3270.FieldNotFoundException;
import dev.galasa.zos3270.KeyboardLockedException;
import dev.galasa.zos3270.TerminalInterruptedException;
import dev.galasa.zos3270.TextNotFoundException;
import dev.galasa.zos3270.TimeoutException;
import dev.galasa.zos3270.Zos3270Exception;
import dev.galasa.zos3270.spi.NetworkException;

@Test
public class CEDAManagerIVT {

	@CicsRegion()
	public ICicsRegion cics;

	@CicsTerminal()
	public ICicsTerminal cedaTerminal;

	@CicsTerminal()
	public ICicsTerminal cemtTerminal;

	@CicsTerminal()
	public ICicsTerminal terminal;

	@CEMT
	public ICEMT cemt;
	@CEDA
	public ICEDA ceda;

	@Logger
	public Log logger;

	@BeforeClass
	public void login() throws InterruptedException, Zos3270Exception {
		// Logon to the CICS Region


		cedaTerminal.clear();
		cedaTerminal.waitForKeyboard();
		cemtTerminal.clear();
		cemtTerminal.waitForKeyboard();
	}
	@Before
	public void before() throws TimeoutException, KeyboardLockedException, TerminalInterruptedException, NetworkException, FieldNotFoundException {
		// making sure that elements that will be used in the tests do not exist in the managers
		terminal.clear().waitForKeyboard();
		terminal.type("CEDA DELETE GROUP(Test) ALL").enter().waitForKeyboard();
		terminal.pf3().waitForKeyboard().clear().waitForKeyboard();
		terminal.type("CEDA DELETE GROUP(IVT) ALL").enter().waitForKeyboard();
		terminal.pf3().waitForKeyboard().clear().waitForKeyboard();
		terminal.type("CEDA DELETE GROUP(noIVT) ALL").enter().waitForKeyboard();
		terminal.pf3().waitForKeyboard().clear().waitForKeyboard();
		terminal.type("CEMT DISCARD prog(Program,prg1,prg2,prg3,prg4)").enter().waitForKeyboard();
		terminal.pf3().waitForKeyboard().clear().waitForKeyboard();
		terminal.type("CEMT DISCARD transaction(trx1)").enter().waitForKeyboard();
		terminal.pf3().waitForKeyboard().clear().waitForKeyboard();
		terminal.type("CEMT DISCARD LIBRARY(lib1)").enter().waitForKeyboard();
		terminal.pf3().waitForKeyboard().clear().waitForKeyboard();
	}

	@Test
	public void checkCECINotNull() {
		assertThat(ceda).isNotNull();
		assertThat(cics).isNotNull();
		assertThat(cemt).isNotNull();
		assertThat(cemtTerminal).isNotNull();
		assertThat(cedaTerminal).isNotNull();
	}


	@Test
	public void testResourceProgram() throws TextNotFoundException, ErrorTextFoundException, Zos3270Exception, InterruptedException, CEDAException {
		String resourceType = "PROGRAM";
		String resourceName = "Program";
		String groupName = "Test";
		String resourceParameters = null;
		boolean response = false;
		try {
			// testing create and install resource by creating it, installing it and then checking if it appeared on CEMT
			ceda.createResource(cedaTerminal, resourceType, resourceName, groupName, resourceParameters);

			ceda.installResource(cedaTerminal, resourceType, resourceName, groupName);

			if(cemt.inquireResource(cemtTerminal, resourceType, resourceName)!=null) {
				response = true;
			}
			assertThat(response);
			// if resource was installed successfully, then tests the delete method by discarding resource from CEMT, deleting and then trying to install and checking if the resource appeared on CEMT
			if (response) {
				response=false;
				cemt.discardResource(cemtTerminal, resourceType, resourceName, "RESPONSE: NORMAL");

				if(cemt.inquireResource(cemtTerminal, resourceType, resourceName)==null) {

					ceda.deleteResource(cedaTerminal, resourceType, resourceName, groupName);

					ceda.installResource(cedaTerminal, resourceType, resourceName, groupName);

					if(cemt.inquireResource(cemtTerminal, resourceType, resourceName)==null) {
						response = true;
					}
					assertThat(response);

				}else Fail.fail("Failed to discard resource");

			}else Fail.fail("Failed to intsall / delete resource");

		} catch (CEDAException | CEMTException e) {
			e.printStackTrace();
		}


	}

	@Test
	public void testResourceTransaction() throws TextNotFoundException, ErrorTextFoundException, Zos3270Exception, InterruptedException, CEDAException {
		String resourceType = "TRANSACTION";
		String resourceName = "trx1";
		String groupName = "Test";
		String resourceParameters = "PROGRAM(PRG1)";
		boolean response = false;
		try {

			// testing create and install resource by creating it, installing it and then checking if it appeared on CEMT

			ceda.createResource(cedaTerminal, resourceType, resourceName, groupName, resourceParameters);

			ceda.installResource(cedaTerminal, resourceType, resourceName, groupName);

			if(cemt.inquireResource(cemtTerminal, resourceType, resourceName)!=null) {
				response = true;
			}
			assertThat(response);

			// if resource was installed successfully, then tests the delete method by discarding resource from CEMT, deleting and then trying to install and checking if the resource appeared on CEMT

			if (response) {
				response=false;
				cemt.discardResource(cemtTerminal, resourceType, resourceName, "RESPONSE: NORMAL");

				if(cemt.inquireResource(cemtTerminal, resourceType, resourceName)==null) {

					ceda.deleteResource(cedaTerminal, resourceType, resourceName, groupName);

					ceda.installResource(cedaTerminal, resourceType, resourceName, groupName);

					if(cemt.inquireResource(cemtTerminal, resourceType, resourceName)==null) {
						response = true;
					}
					assertThat(response);

				}else Fail.fail("Failed to discard resource");

			}else Fail.fail("Failed to intsall / delete resource");

		} catch (CEDAException | CEMTException e) {
			e.printStackTrace();
		}


	}

	@Test
	public void testResourceLibrary() throws TextNotFoundException, ErrorTextFoundException, Zos3270Exception, InterruptedException, CEDAException {
		String resourceType = "LIBRARY";
		String resourceName = "lib1";
		String groupName = "Test";
		String resourceParameters = "DSNAME01(CTS.USER.APPL1.CICS.LOAD)";
		boolean response = false;
		try {

			// testing create and install resource by creating it, installing it and then checking if it appeared on CEMT

			ceda.createResource(cedaTerminal, resourceType, resourceName, groupName, resourceParameters);

			ceda.installResource(cedaTerminal, resourceType, resourceName, groupName);

			if(cemt.inquireResource(cemtTerminal, resourceType, resourceName)!=null) {
				response = true;
			}
			assertThat(response);
			
			// if resource was installed successfully, then tests the delete method by discarding resource from CEMT, deleting and then trying to install and checking if the resource appeared on CEMT

			if (response) {
				response=false;
				cemt.discardResource(cemtTerminal, resourceType, resourceName, "RESPONSE: NORMAL");

				if(cemt.inquireResource(cemtTerminal, resourceType, resourceName)==null) {

					ceda.deleteResource(cedaTerminal, resourceType, resourceName, groupName);

					ceda.installResource(cedaTerminal, resourceType, resourceName, groupName);

					if(cemt.inquireResource(cemtTerminal, resourceType, resourceName)==null) {
						response = true;
					}
					assertThat(response);

				}else Fail.fail("Failed to discard resource");

			}else Fail.fail("Failed to intsall / delete resource");

		} catch (CEDAException | CEMTException e) {
			e.printStackTrace();
		}


	}


	@Test
	public void testGroup() throws TextNotFoundException, ErrorTextFoundException, Zos3270Exception, CEMTException, InterruptedException, CEDAException {
		String resourceType = "prog";
		String resourceName = "prg1";
		String resourceName2 = "prg2";
		String resourceName3 = "prg3";
		String resourceName4 = "prg4";
		String groupName = "IVT";
		String groupName2 = "noIVT";
		boolean result =false;
		// creating different resources in two different groups
		ceda.createResource(cedaTerminal, resourceType, resourceName, groupName, null);
		ceda.createResource(cedaTerminal, resourceType, resourceName2, groupName, null);
		ceda.createResource(cedaTerminal, resourceType, resourceName3, groupName, null);
		/** different group**/
		ceda.createResource(cedaTerminal, resourceType, resourceName4, groupName2, null);
		// installing only one group and check if installed group appeared in CEMT and not installed one did not
		ceda.installGroup(cedaTerminal, groupName);
		if (cemt.inquireResource(cemtTerminal,resourceType, resourceName).containsValue(resourceName.toUpperCase())&&cemt.inquireResource(cemtTerminal,resourceType, resourceName2).containsValue(resourceName2.toUpperCase())&&cemt.inquireResource(cemtTerminal,resourceType, resourceName3).containsValue(resourceName3.toUpperCase())&&cemt.inquireResource(cemtTerminal,resourceType, resourceName4)==null) {
			result =true;
		}
		assertThat(result);

		//Checking if group delete works by discarding elements from CEMT and deleting group from CEDA, checking by installing group
		if(result) {
			result=false;
			ceda.deleteGroup(cedaTerminal, groupName);
			cemt.discardResource(cemtTerminal, resourceType, resourceName, "RESPONSE: NORMAL");
			cemt.discardResource(cemtTerminal, resourceType, resourceName2, "RESPONSE: NORMAL");
			cemt.discardResource(cemtTerminal, resourceType, resourceName3, "RESPONSE: NORMAL");
			assertThatThrownBy(() ->{
				ceda.installGroup(cedaTerminal, groupName);
			}).isInstanceOf(CEDAException.class).hasMessageContaining("Problem determining the result from the CEDA command");


		}else Fail.fail("CEDA Group Install/Delete failed");
	}


}
