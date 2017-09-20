package org.bitbucket.openkilda.atdd;

import static org.bitbucket.openkilda.DefaultParameters.trafficEndpoint;
import static org.bitbucket.openkilda.flow.FlowUtils.isTrafficTestsEnabled;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.bitbucket.openkilda.flow.FlowUtils;
import org.bitbucket.openkilda.messaging.model.Flow;
import org.bitbucket.openkilda.messaging.payload.flow.FlowEndpointPayload;
import org.bitbucket.openkilda.messaging.payload.flow.FlowPayload;
import org.bitbucket.openkilda.messaging.payload.flow.FlowState;
import org.bitbucket.openkilda.topo.TopologyHelp;

import cucumber.api.java.en.Given;
import cucumber.api.java.en.When;
import org.glassfish.jersey.client.ClientConfig;

import java.io.File;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;

public class FlowFFRTest {
    private static final String fileName = "topologies/barebones-topology.json";
    private static int numberOfPathes = 2;
    private static final List<List<String>> failebleLinks = new LinkedList<List<String>>(Arrays.asList(
            new LinkedList<String>(Arrays.asList("s3", "1")), new LinkedList<String>(Arrays.asList("s4", "1"))));

    private static final long FLOW_COOKIE = 1L;
    private static final String flowId = "1";
    private static final String sourceSwitch = "00:00:00:00:00:00:00:02";
    private static final String destinationSwitch = "00:00:00:00:00:00:00:06";
    private static final Integer sourcePort = 1;
    private static final Integer destinationPort = 1;
    private static final Integer sourceVlan = 1000;
    private static final Integer destinationVlan = 1000;
    private static final int bandwidth = 1000;

    private FlowPayload flowPayload;
    private Flow flow;
    private List<List<String>> failedLinks = new LinkedList<List<String>>();

    private void failLink() throws Throwable {
        // This method is designed to work with barebones topology.
        // It might need refactoring in the future when other topologies
        // are considered
        List<String> linkToFail = failebleLinks.remove(0);
        failedLinks.add(linkToFail);
        String switchName = linkToFail.get(0);
        String portNum = linkToFail.get(1);
        Client client = ClientBuilder.newClient(new ClientConfig());
        Response result = client
                .target(trafficEndpoint)
                .path("/set_link_state")
                .queryParam("switch", switchName)
                .queryParam("port", portNum)
                .queryParam("newstate", "down")
                .request()
                .post(Entity.json(""));
        assertEquals(200, result.getStatus());
    }

    private void failISL() throws Throwable {
        List<String> linkToFail = failebleLinks.remove(0);
        failedLinks.add(linkToFail);
        String switchName = linkToFail.get(0);
        String portNum = linkToFail.get(1);
        Client client = ClientBuilder.newClient(new ClientConfig());
        Response result = client
                .target(trafficEndpoint)
                .path("/cutlink")
                .queryParam("switch", switchName)
                .queryParam("port", portNum)
                .request()
                .post(Entity.json(""));
        assertEquals(200, result.getStatus());
    }

    private void failSwitch() throws Throwable {
        List<String> linkToFail = failebleLinks.remove(0);
        failedLinks.add(linkToFail);
        String switchName = linkToFail.get(0);
        String portNum = linkToFail.get(1);
        Client client = ClientBuilder.newClient(new ClientConfig());
        Response result = client
                .target(trafficEndpoint)
                .path("/knockoutswitch")
                .queryParam("switch", switchName)
                .request()
                .post(Entity.json(""));
        assertEquals(200, result.getStatus());
    }

    private void resurrectLink() throws Throwable {
        List<String> linkToRestore = failedLinks.remove(1);
        failedLinks.add(linkToRestore);
        String switchName = linkToRestore.get(0);
        String portNum = linkToRestore.get(1);
        Client client = ClientBuilder.newClient(new ClientConfig());
        Response result = client
                .target(trafficEndpoint)
                .path("/set_link_state")
                .queryParam("switch", switchName)
                .queryParam("port", portNum)
                .queryParam("newstate", "up")
                .request()
                .post(Entity.json(""));
        assertEquals(200, result.getStatus());
    }

    private void resurrectISL() throws Throwable {
        List<String> linkToRestore = failedLinks.remove(1);
        failedLinks.add(linkToRestore);
        String switchName = linkToRestore.get(0);
        String portNum = linkToRestore.get(1);
        Client client = ClientBuilder.newClient(new ClientConfig());
        Response result = client
                .target(trafficEndpoint)
                .path("/restorelink")
                .queryParam("switch", switchName)
                .request()
                .post(Entity.json(""));
        assertEquals(200, result.getStatus());
    }

    private void resurrectSwitch() throws Throwable {
        List<String> linkToRestore = failedLinks.remove(1);
        failedLinks.add(linkToRestore);
        String switchName = linkToRestore.get(0);
        String portNum = linkToRestore.get(1);
        Client client = ClientBuilder.newClient(new ClientConfig());
        Response result = client
                .target(trafficEndpoint)
                .path("/reviveswitch")
                .queryParam("switch", switchName)
                .queryParam("controller", "tcp:172.19.0.6:6653")
                .request()
                .post(Entity.json(""));
        assertEquals(200, result.getStatus());
    }

    private boolean trafficIsOk(boolean expectedResult) throws Throwable {
        if (isTrafficTestsEnabled()) {
            TimeUnit.SECONDS.sleep(1);
            Client client = ClientBuilder.newClient(new ClientConfig());
            Response result = client
                    .target(trafficEndpoint)
                    .path("/checkflowtraffic")
                    .queryParam("srcswitch", "s1")
                    .queryParam("dstswitch", "s6")
                    .queryParam("srcport", "1")
                    .queryParam("dstport", "1")
                    .queryParam("srcvlan", "1000")
                    .queryParam("dstvlan", "1000")
                    .request()
                    .get();
            return result.getStatus() == 200;
        } else {
            return expectedResult;
        }
    }

    @Given("^basic multi-path topology$")
    public void a_multi_path_topology() throws Throwable {
        ClassLoader classLoader = getClass().getClassLoader();
        File file = new File(classLoader.getResource(fileName).getFile());
        String json = new String(Files.readAllBytes(file.toPath()));
        assert TopologyHelp.CreateMininetTopology(json);
    }

    @When("^a flow is successfully created$")
    public void successfulFlowCreation() throws Throwable {
        flowPayload = new FlowPayload(FlowUtils.getFlowName(flowId),
                new FlowEndpointPayload(sourceSwitch, sourcePort, sourceVlan),
                new FlowEndpointPayload(destinationSwitch, destinationPort, destinationVlan),
                bandwidth, flowId, null);
        flow = new Flow(FlowUtils.getFlowName(flowId), bandwidth, FLOW_COOKIE, flowId, null, sourceSwitch,
                destinationSwitch, sourcePort, destinationPort, sourceVlan, destinationVlan, 0, 0, null, FlowState.UP);

        FlowPayload response = FlowUtils.putFlow(flowPayload);
        assertNotNull(response);
        response.setLastUpdated(null);

        assertEquals(flowPayload, response);
        flowPayload = response;
        System.out.println(response.toString());
        TimeUnit.SECONDS.sleep(5);
    }

    @When("^traffic flows through this flow$")
    public void trafficFlows() throws Throwable {
        assertTrue(trafficIsOk(true));
    }

    @When("^traffic does not flow through this flow$")
    public void trafficNotFlows() throws Throwable {
        assertFalse(trafficIsOk(false));
    }


    @When("^a route in use fails$")
    public void routeInUseFails() throws Throwable {
        failLink();
        numberOfPathes--;
    }

    @When("^an ISL in use fails$")
    public void ISLInUseFails() throws Throwable {
        failISL();
        numberOfPathes--;
    }

    @When("^there is an alternative route$")
    public void alternativeRouteExists() throws Throwable {
        assertNotEquals(numberOfPathes, 0);
    }

    @When("^there is no alternative route$")
    public void noRoutesInFlow() throws Throwable {
        assertEquals(numberOfPathes, 0);
    }

    @When("^a failed route comes back up$")
    public void resurrectRoute() throws Throwable {
        resurrectLink();
        numberOfPathes++;
    }

    @When("^a failed ISL comes back up$")
    public void restoreISL() throws Throwable {
        resurrectISL();
        numberOfPathes++;
    }

    @When("^a switch in use fails$")
    public void switchInUseFails() throws Throwable {
        failSwitch();
        numberOfPathes--;
    }

    @When("^a failed switch comes back up$")
    public void restoreSwitch() throws Throwable {
        resurrectSwitch();
        numberOfPathes++;
    }
}
