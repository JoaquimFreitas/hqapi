package org.hyperic.hq.hqapi1.test;

import org.hyperic.hq.hqapi1.AlertDefinitionApi;
import org.hyperic.hq.hqapi1.AlertDefinitionBuilder;
import org.hyperic.hq.hqapi1.AlertDefinitionBuilder.AlertPriority;
import org.hyperic.hq.hqapi1.EscalationApi;
import org.hyperic.hq.hqapi1.HQApi;
import org.hyperic.hq.hqapi1.types.AlertCondition;
import org.hyperic.hq.hqapi1.types.AlertDefinition;
import org.hyperic.hq.hqapi1.types.AlertDefinitionsResponse;
import org.hyperic.hq.hqapi1.types.Escalation;
import org.hyperic.hq.hqapi1.types.Resource;
import org.hyperic.hq.hqapi1.types.ResourcePrototype;
import org.hyperic.hq.hqapi1.types.EscalationResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class AlertDefinitionSync_test extends AlertDefinitionTestBase {

    public AlertDefinitionSync_test(String name) {
        super(name);
    }

    // Generic AlertDefinition tests
    
    private AlertDefinition createTestDefinition() {
        AlertDefinition d = new AlertDefinition();

        Random r = new Random();
        d.setName("Test Alert Definition" + r.nextInt());
        d.setDescription("Test Alert Description");
        d.setPriority(AlertPriority.MEDIUM.getPriority());
        d.setEnabled(true);
        return d;
    }

    public void testSyncNoResource() throws Exception {

        AlertDefinitionApi api = getApi().getAlertDefinitionApi();

        AlertDefinition d = createTestDefinition();
        d.getAlertCondition().add(AlertDefinitionBuilder.createPropertyCondition(true, "myProp"));
        List<AlertDefinition> definitions = new ArrayList<AlertDefinition>();
        definitions.add(d);

        AlertDefinitionsResponse response = api.syncAlertDefinitions(definitions);
        hqAssertFailureInvalidParameters(response);
    }

    public void testSyncInvalidAlertId() throws Exception {

        AlertDefinitionApi api = getApi().getAlertDefinitionApi();
        Resource platform = getLocalPlatformResource(false, false);

        AlertDefinition d = createTestDefinition();
        d.setId(Integer.MAX_VALUE);
        d.setResourcePrototype(platform.getResourcePrototype());
        d.getAlertCondition().add(AlertDefinitionBuilder.createPropertyCondition(true, "myProp"));
        List<AlertDefinition> definitions = new ArrayList<AlertDefinition>();
        definitions.add(d);

        AlertDefinitionsResponse response = api.syncAlertDefinitions(definitions);
        hqAssertFailureObjectNotFound(response);
    }

    public void testSyncResourceAndResourcePrototype() throws Exception {

        AlertDefinitionApi api = getApi().getAlertDefinitionApi();
        Resource platform = getLocalPlatformResource(false, false);

        AlertDefinition d = createTestDefinition();
        d.setResourcePrototype(platform.getResourcePrototype());
        d.setResource(platform); // Can't have Resource & ResourcePrototype
        d.getAlertCondition().add(AlertDefinitionBuilder.createPropertyCondition(true, "myProp"));
        List<AlertDefinition> definitions = new ArrayList<AlertDefinition>();
        definitions.add(d);

        AlertDefinitionsResponse response = api.syncAlertDefinitions(definitions);
        hqAssertFailureInvalidParameters(response);
    }

    public void testSyncInvalidResource() throws Exception {

        AlertDefinitionApi api = getApi().getAlertDefinitionApi();
        Resource platform = new Resource();
        platform.setId(Integer.MAX_VALUE);
        platform.setName("Invalid Platform Resource");

        AlertDefinition d = createTestDefinition();
        d.setResource(platform); // Invalid Resource
        d.getAlertCondition().add(AlertDefinitionBuilder.createPropertyCondition(true, "myProp"));
        List<AlertDefinition> definitions = new ArrayList<AlertDefinition>();
        definitions.add(d);

        AlertDefinitionsResponse response = api.syncAlertDefinitions(definitions);
        hqAssertFailureObjectNotFound(response);
    }

    public void testSyncInvalidResourcePrototype() throws Exception {

        AlertDefinitionApi api = getApi().getAlertDefinitionApi();
        ResourcePrototype proto = new ResourcePrototype();
        proto.setName("Invalid Prototype");

        AlertDefinition d = createTestDefinition();
        d.setResourcePrototype(proto);
        d.getAlertCondition().add(AlertDefinitionBuilder.createPropertyCondition(true, "myProp"));
        List<AlertDefinition> definitions = new ArrayList<AlertDefinition>();
        definitions.add(d);

        AlertDefinitionsResponse response = api.syncAlertDefinitions(definitions);
        hqAssertFailureObjectNotFound(response);
    }

    public void testSyncInvalidPriority() throws Exception {
        AlertDefinitionApi api = getApi().getAlertDefinitionApi();
        Resource platform = getLocalPlatformResource(false, false);

        AlertDefinition d = createTestDefinition();
        d.setResourcePrototype(platform.getResourcePrototype());
        d.getAlertCondition().add(AlertDefinitionBuilder.createPropertyCondition(true, "myProp"));
        d.setPriority(4);
        List<AlertDefinition> definitions = new ArrayList<AlertDefinition>();
        definitions.add(d);

        AlertDefinitionsResponse response = api.syncAlertDefinitions(definitions);
        hqAssertFailureInvalidParameters(response);
    }

    public void testSyncBasicDefinition() throws Exception {
        AlertDefinitionApi api = getApi().getAlertDefinitionApi();
        Resource platform = getLocalPlatformResource(false, false);

        AlertDefinition d = createTestDefinition();
        d.setResourcePrototype(platform.getResourcePrototype());
        d.getAlertCondition().add(AlertDefinitionBuilder.createPropertyCondition(true, "myProp"));
        List<AlertDefinition> definitions = new ArrayList<AlertDefinition>();
        definitions.add(d);

        AlertDefinitionsResponse response = api.syncAlertDefinitions(definitions);
        hqAssertSuccess(response);
        assertEquals(response.getAlertDefinition().size(), 1);        
        for (AlertDefinition def : response.getAlertDefinition()) {
            validateDefinition(def);
        }

        // Cleanup
        cleanup(response.getAlertDefinition());
    }

    public void testSyncCountAndRange() throws Exception {
        AlertDefinitionApi api = getApi().getAlertDefinitionApi();
        Resource platform = getLocalPlatformResource(false, false);

        AlertDefinition d = createTestDefinition();
        d.setResourcePrototype(platform.getResourcePrototype());
        d.getAlertCondition().add(AlertDefinitionBuilder.createPropertyCondition(true, "myProp"));
        d.setCount(3);
        d.setRange(1800);
        List<AlertDefinition> definitions = new ArrayList<AlertDefinition>();
        definitions.add(d);

        AlertDefinitionsResponse response = api.syncAlertDefinitions(definitions);
        hqAssertSuccess(response);
        assertEquals(response.getAlertDefinition().size(), 1);
        for (AlertDefinition def : response.getAlertDefinition()) {
            validateDefinition(def);
        }

        // Cleanup
        cleanup(response.getAlertDefinition());
    }

    public void testSyncMulti() throws Exception {
        AlertDefinitionApi api = getApi().getAlertDefinitionApi();
        Resource platform = getLocalPlatformResource(false, false);

        final int NUM_DEFS = 10;
        List<AlertDefinition> definitions = new ArrayList<AlertDefinition>();
        for (int i = 0; i < NUM_DEFS; i++) {
            AlertDefinition d = createTestDefinition();
            d.setResourcePrototype(platform.getResourcePrototype());
            d.getAlertCondition().add(AlertDefinitionBuilder.createPropertyCondition(true, "myProp"));
            definitions.add(d);
        }

        AlertDefinitionsResponse response = api.syncAlertDefinitions(definitions);
        hqAssertSuccess(response);
        assertEquals(response.getAlertDefinition().size(), NUM_DEFS);        
        for (AlertDefinition def : response.getAlertDefinition()) {
            validateDefinition(def);
        }

        // Cleanup
        cleanup(response.getAlertDefinition());
    }

    // Escalation tests

    public void testSyncInvalidEscalation() throws Exception {
        AlertDefinitionApi api = getApi().getAlertDefinitionApi();
        Resource platform = getLocalPlatformResource(false, false);

        AlertDefinition d = createTestDefinition();
        d.setResourcePrototype(platform.getResourcePrototype());
        d.getAlertCondition().add(AlertDefinitionBuilder.createPropertyCondition(true, "myProp"));
        Escalation e = new Escalation();
        e.setName("Invalid Escalation");
        d.setEscalation(e);
        List<AlertDefinition> definitions = new ArrayList<AlertDefinition>();
        definitions.add(d);

        AlertDefinitionsResponse response = api.syncAlertDefinitions(definitions);
        hqAssertFailureObjectNotFound(response);
    }

    public void testSyncEmptyEscalation() throws Exception {
        AlertDefinitionApi api = getApi().getAlertDefinitionApi();
        Resource platform = getLocalPlatformResource(false, false);

        AlertDefinition d = createTestDefinition();
        d.setResourcePrototype(platform.getResourcePrototype());
        d.getAlertCondition().add(AlertDefinitionBuilder.createPropertyCondition(true, "myProp"));
        Escalation e = new Escalation();
        d.setEscalation(e);
        List<AlertDefinition> definitions = new ArrayList<AlertDefinition>();
        definitions.add(d);

        AlertDefinitionsResponse response = api.syncAlertDefinitions(definitions);
        hqAssertFailureObjectNotFound(response);
    }

    public void testSyncEscalation() throws Exception {
        HQApi api = getApi();
        AlertDefinitionApi defApi = api.getAlertDefinitionApi();
        EscalationApi escApi = api.getEscalationApi();

        Resource platform = getLocalPlatformResource(false, false);

        Random r = new Random();
        Escalation e = new Escalation();
        e.setName("Test Escalation" + r.nextInt());
        EscalationResponse escalationResponse =
                escApi.createEscalation(e);
        hqAssertSuccess(escalationResponse);

        AlertDefinition d = createTestDefinition();
        d.setResourcePrototype(platform.getResourcePrototype());
        d.getAlertCondition().add(AlertDefinitionBuilder.createPropertyCondition(true, "myProp"));
        d.setEscalation(e);
        List<AlertDefinition> definitions = new ArrayList<AlertDefinition>();
        definitions.add(d);

        AlertDefinitionsResponse response = defApi.syncAlertDefinitions(definitions);
        hqAssertSuccess(response);
        assertEquals(response.getAlertDefinition().size(), 1);
        for (AlertDefinition def : response.getAlertDefinition()) {
            validateDefinition(def);
            assertEquals(def.getEscalation().getId(),
                         escalationResponse.getEscalation().getId());
        }

        // Cleanup
        cleanup(response.getAlertDefinition());
        escApi.deleteEscalation(escalationResponse.getEscalation().getId());
    }

    // AlertCondition tests

    public void testSyncNoConditions() throws Exception {

        AlertDefinitionApi api = getApi().getAlertDefinitionApi();
        Resource platform = getLocalPlatformResource(false, false);

        AlertDefinition d = createTestDefinition();
        d.setResourcePrototype(platform.getResourcePrototype());
        List<AlertDefinition> definitions = new ArrayList<AlertDefinition>();
        definitions.add(d);

        AlertDefinitionsResponse response = api.syncAlertDefinitions(definitions);
        hqAssertFailureInvalidParameters(response);
    }

    public void testInvalidAlertConditionType() throws Exception {

        AlertDefinitionApi api = getApi().getAlertDefinitionApi();
        Resource platform = getLocalPlatformResource(false, false);

        AlertDefinition d = createTestDefinition();
        d.setResourcePrototype(platform.getResourcePrototype());

        AlertCondition cond = new AlertCondition();
        cond.setRequired(true);
        cond.setType(10); // Types range from 1-8.. See EventsConstants

        d.getAlertCondition().add(cond);
        List<AlertDefinition> definitions = new ArrayList<AlertDefinition>();
        definitions.add(d);

        AlertDefinitionsResponse response = api.syncAlertDefinitions(definitions);
        hqAssertFailureInvalidParameters(response);
    }
}
