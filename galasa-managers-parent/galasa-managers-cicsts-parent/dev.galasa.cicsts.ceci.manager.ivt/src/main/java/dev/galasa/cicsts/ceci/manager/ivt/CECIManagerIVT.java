/*
 * Licensed Materials - Property of IBM
 * 
 * (c) Copyright IBM Corp. 2020.
 */
package dev.galasa.cicsts.ceci.manager.ivt;

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.commons.logging.Log;

import dev.galasa.Test;
import dev.galasa.BeforeClass;
import dev.galasa.cicsts.ceci.CECI;
import dev.galasa.cicsts.ceci.CECIException;
import dev.galasa.cicsts.ceci.ICECI;
import dev.galasa.cicsts.ceci.ICECIResponse;
import dev.galasa.core.manager.Logger;
import dev.galasa.zos.IZosImage;
import dev.galasa.zos.ZosImage;
import dev.galasa.zos3270.ITerminal;
import dev.galasa.zos3270.Zos3270Terminal;
import dev.galasa.zos3270.Zos3270Exception;

/**
 * IVT for the CICS CECI Manager
 * 
 * 
 * @author Will Yates
 *
 */
@Test
public class CECIManagerIVT {

    @ZosImage(imageTag="GENAPP")
    public IZosImage zosImage;

    @Zos3270Terminal(imageTag = "GENAPP")
    public ITerminal ceciTerminal;

    @Zos3270Terminal(imageTag = "GENAPP")
    public ITerminal terminal;

    @CECI
    public ICECI ceci;

    @Logger
    public Log logger;

    @BeforeClass
    public void login()throws InterruptedException, Zos3270Exception {
        //Logon to the CICS Region
        ceciTerminal.waitForKeyboard();
        ceciTerminal.type("logon applid(CICS01)");
        ceciTerminal.enter();
        ceciTerminal.waitForTextInField("This is the CICS MRO Terminal Owing Region");
        ceciTerminal.clear();
        ceciTerminal.waitForKeyboard();
        ceciTerminal.type("CECI").enter().waitForKeyboard();

        terminal.waitForKeyboard();
        terminal.type("logon applid(CICS01)");
        terminal.enter();
        terminal.waitForTextInField("This is the CICS MRO Terminal Owing Region");
        terminal.clear();
        terminal.waitForKeyboard();
    }

    /**
     * Ensure that we have an instance of the CECI Manager
     * 
     * @throws IvtException if there is a problem with the docker manager
     */
    @Test
    public void checkCECINotNull() {
        assertThat(ceci).isNotNull();
    }

    /**
     * Tests that variables defined with a long name are caught
     * @throws CECIException
     */
    @Test 
    public void testVariablesWithLONGNames() throws CECIException{
        String nineCharacterName = "ABCDEFGHI";
        String tenCharacterName    = "ABCDEFGHIJ";
        String variableContent = "THIS IS A TEXT STRING";

        int length = ceci.defineVariableText(ceciTerminal, nineCharacterName, variableContent);
        assertThat(length).isEqualTo(variableContent.length());
        try{
            ceci.defineVariableText(ceciTerminal, tenCharacterName, variableContent);
        }catch(CECIException ce){

        }
    }

    /**
     * Attempts to retrieve a variable that has been deleted
     * @throws CECIException
     */
    //@Test
    public void basicDeleteTest() throws CECIException{
        String variableContent = "THIS IS A TEXT STRING";
        String variableName = "TEST2";
        ceci.defineVariableText(ceciTerminal, variableName, variableContent);
        ceci.deleteVariable(ceciTerminal, variableName);
        try{
            ceci.retrieveVariableText(ceciTerminal, variableName);
        }catch(CECIException ce){

        }
    }

    /**
     * Defines, retrieves a variable and then attempts to delete it
     * Currently fails to delete the vairable after it has been retrieved
     * @throws CECIException
     */
    //@Test
    public void checkStoringVariableDefinition()throws CECIException{
        String variableContent = "THIS IS A TEXT STRING";
        String variableName = "TEST1";
        logger.info("Defining TEST1");
        ceci.defineVariableText(ceciTerminal, variableName, variableContent);
        logger.info("Retrieving TEST1 and checking content is accurate");
        assertThat(ceci.retrieveVariableText(ceciTerminal, variableName)).isEqualTo(variableContent);
        logger.info("deleting variable TEST1");
        ceci.deleteVariable(ceciTerminal, variableName);
        try{
            logger.info("attempting to retrieve the deleted variable");
            ceci.retrieveVariableText(ceciTerminal, variableName);
        }catch(CECIException ce){
            assertThat(ce.getMessage()).isEqualTo("Unable to find variable &TEST1");
        }
    }

    /**
     * Tests the execution of a basic command
     * currently causes the manager to go into a loop
     * @throws CECIException
     */
    //@Test
    public void basicCommandExecution()throws CECIException{
        String userVariable = "USERID";
        String command = "ASSIGN USERID(&" + userVariable + ")";

        ICECIResponse resp = ceci.issueCommand(ceciTerminal,command,false);
        String user = ceci.retrieveVariableText(ceciTerminal, userVariable);
        logger.info("Retrieved user was: " + user);
        logger.info("response from command was: " + resp.getResponse());
    }

    @Test
    public void documentationTestBasicCommand() throws CECIException {
        String ceciCommand = "EXEC CICS WRITE OPERATOR TEXT('About to execute Galasa Test...')";
        ICECIResponse resp = ceci.issueCommand(terminal, ceciCommand);
        assertThat(resp.isNormal()).isTrue();
    }

    @Test
    public void documentationTestLinkWithContainer() throws CECIException {
        String inputData = "My_Container_Data";
        ICECIResponse resp = ceci.putContainer(ceciTerminal, "MY-CHANNEL", "MY-CONTAINER-IN", inputData, null, null, null);
        assertThat(resp.isNormal()).isTrue();
        resp = ceci.linkProgramWithChannel(ceciTerminal, "MYPROG", "MY-CHANNEL", null, null, false);
        assertThat(resp.isNormal()).isTrue();
        resp = ceci.getContainer(ceciTerminal, "MY-CHANNEL", "MY-CONTAINER-OUT", "&DATAOUT", null, null);
        assertThat(resp.isNormal()).isTrue();
        String outputData = ceci.retrieveVariableText(ceciTerminal, "&DATAOUT");
        assertThat(outputData).isEqualTo(inputData);
    }

    private void checkqueue(String queueName, String checkData) throws InterruptedException, Zos3270Exception{
        terminal.type("CEBR " + queueName).enter().waitForKeyboard();
        assertThat(terminal.retrieveScreen().contains(checkData));
        terminal.pf3().waitForKeyboard().clear().waitForKeyboard();
    }

    /**
     * Writes data to a TSQ, checks that it was written and then cleans up the queue
     * @throws CECIException
     * @throws InterruptedException
     * @throws Zos3270Exception
     */
    //@Test
    public void writeToTSQTest()throws CECIException, InterruptedException, Zos3270Exception{
        String queueName = "queue1";
        String dataToWrite = "My name is Hobbit";
        String variableName = "TSQDATA";
        ceci.defineVariableText(ceciTerminal, variableName, dataToWrite);
        String command = "WRITEQ TS QUEUE('" + queueName +"') FROM(&" + variableName + ")";
        ceci.issueCommand(ceciTerminal, command);
        checkqueue(queueName, dataToWrite);
        command = "DELETEQ TS QUEUE('" + queueName + "')";
        ceci.issueCommand(ceciTerminal, command);
        checkqueue(queueName, "DOES NOT EXIST");
        
    }

    /**
     * Defines two variables of the same name and checks that the 
     * second definition takes effect 
     * @throws CECIException
     */
    //@Test
    public void canWeDefineTwoVariablesWithSameName() throws CECIException {
        String variableName = "NOTUNIQUE";
        String value1 = "value1";
        String value2 = "A longer value";
        int length = 0;
        length = ceci.defineVariableText(ceciTerminal, variableName, value1);
        assertThat(length).isEqualTo(value1.length());
        length = ceci.defineVariableText(ceciTerminal, variableName, value2);
        assertThat(length).isEqualTo(value2.length());
        assertThat(ceci.retrieveVariableText(ceciTerminal, variableName).equals(value2));
    }

    /**
     * Simple test that puts data to a queue and retrieves it again
     * currently failing
     * @throws CECIException
     */ 
    //@Test
    public void putAndGetDataToAContainer() throws CECIException{
        String containerName = "CONT1";
        String containerData = "THIS IS SOME CONTAINER DATA";
        String channelName   = "CHAN1";
        String variableName = "&OP";

        ceci.putContainer(ceciTerminal, channelName, containerName, containerData, "CHAR", null,null);
        ceci.getContainer(ceciTerminal, channelName, containerName, variableName, null,null);
        assertThat(ceci.retrieveVariableText(ceciTerminal, variableName).equals(containerData));
    }  
}