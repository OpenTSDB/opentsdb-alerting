/*
 * This file is part of OpenTSDB.
 * Copyright (C) 2021 Yahoo.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.opentsdb.horizon.alerts.state;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import net.opentsdb.horizon.alerts.AlertUtils;
import net.opentsdb.horizon.alerts.config.TransitionConfig;
import net.opentsdb.horizon.alerts.config.impl.DefaultTransitionConfig;
import net.opentsdb.horizon.alerts.enums.AlertState;

import org.junit.Assert;
import org.testng.annotations.Test;

import java.io.ByteArrayOutputStream;
import java.util.Iterator;
import java.util.TreeMap;

import static net.opentsdb.horizon.alerts.AlertUtils.DO_NOT_NAG;

public class AlertStateStoreImplTest {


    @Test
    public void testRaiseAlert() {
        ByteArrayOutputStream s = new ByteArrayOutputStream();
        s.reset();
        ObjectMapper objectMapper = new ObjectMapper();

        final ArrayNode arrayNode = objectMapper.createArrayNode();

        arrayNode.add("goodToBad");
        arrayNode.add("goodToWarn");
        arrayNode.add("badToGood");

        final Iterator<JsonNode> iterator = arrayNode.iterator();

        TransitionConfig transitionConfig = new DefaultTransitionConfig(iterator, false);
        AlertStateStore alertStateStore = AlertStateStores.withTransitions("id",
                300,transitionConfig);

        final String ns = "namespace";
        final TreeMap<String,String> tags = new TreeMap<String,String>() {{
            put("host","host1");
            put("user","user1");
        }};
        final long alertId = 1l;

        final AlertStateChange bad
                = alertStateStore.raiseAlert(ns, alertId, tags, AlertState.BAD);

        Assert.assertTrue(bad.raiseAlert());
    }

    @Test
    public void testRaisingMultipleAlertsInARun() {

        ObjectMapper objectMapper = new ObjectMapper();

        final ArrayNode arrayNode = objectMapper.createArrayNode();

        arrayNode.add("goodToBad");
        arrayNode.add("goodToWarn");
        arrayNode.add("badToGood");

        final Iterator<JsonNode> iterator = arrayNode.iterator();

        TransitionConfig transitionConfig = new DefaultTransitionConfig(iterator, false);
        AlertStateStore alertStateStore = AlertStateStores.withTransitions("id",
                300,transitionConfig);

        final String ns = "namespace";
        final TreeMap<String,String> tags = new TreeMap<String,String>() {{
            put("host","host1");
            put("user","user1");
        }};
        final long alertId = 1l;

        final AlertStateChange bad
                = alertStateStore.raiseAlert(ns, alertId, tags, AlertState.BAD);

        Assert.assertTrue(bad.raiseAlert());

        final AlertStateChange warn
                = alertStateStore.raiseAlert(ns, alertId, tags, AlertState.WARN);

        Assert.assertFalse(warn.raiseAlert());

        final AlertStateChange good
                = alertStateStore.raiseAlert(ns, alertId, tags, AlertState.GOOD);

        Assert.assertFalse(good.raiseAlert());

        Assert.assertEquals(AlertState.BAD,alertStateStore.
                getCurrentState(AlertUtils.getHashForNAMT(ns,alertId, tags)));
    }

    @Test
    public void testDontNag() {
        ObjectMapper objectMapper = new ObjectMapper();

        final ArrayNode arrayNode = objectMapper.createArrayNode();

        arrayNode.add("goodToBad");
        arrayNode.add("goodToWarn");
        arrayNode.add("badToGood");

        final Iterator<JsonNode> iterator = arrayNode.iterator();

        TransitionConfig transitionConfig = new DefaultTransitionConfig(iterator, false);
        AlertStateStore alertStateStore = AlertStateStores.withTransitions("id",
                300,transitionConfig);

        final String ns = "namespace";
        final TreeMap<String,String> tags = new TreeMap<String,String>() {{
            put("host","host1");
            put("user","user1");
        }};
        final long alertId = 1l;

        final AlertStateChange bad
                = alertStateStore.raiseAlert(ns, alertId, tags, AlertState.BAD);

        Assert.assertTrue(bad.raiseAlert());

        alertStateStore.newRun();

        final AlertStateChange nag
                = alertStateStore.raiseAlert(ns, alertId, tags, AlertState.BAD);

        Assert.assertFalse(nag.raiseAlert());
    }

    @Test
    public void testNag() throws InterruptedException {
        ObjectMapper objectMapper = new ObjectMapper();

        final ArrayNode arrayNode = objectMapper.createArrayNode();

        arrayNode.add("goodToBad");
        arrayNode.add("goodToWarn");
        arrayNode.add("badToGood");

        final Iterator<JsonNode> iterator = arrayNode.iterator();

        TransitionConfig transitionConfig = new DefaultTransitionConfig(iterator, false);
        AlertStateStore alertStateStore = AlertStateStores.withTransitions("id",
                1,transitionConfig);

        final String ns = "namespace";
        final TreeMap<String,String> tags = new TreeMap<String,String>() {{
            put("host","host1");
            put("user","user1");
        }};
        final long alertId = 1l;

        final AlertStateChange bad
                = alertStateStore.raiseAlert(ns, alertId, tags, AlertState.BAD);

        Assert.assertTrue(bad.raiseAlert());

        Thread.sleep(2100);

        alertStateStore.newRun();

        final AlertStateChange nag
                = alertStateStore.raiseAlert(ns, alertId, tags, AlertState.BAD);

        Assert.assertTrue(nag.raiseAlert());
    }

    @Test
    public void testTransition() throws InterruptedException {
        ObjectMapper objectMapper = new ObjectMapper();

        final ArrayNode arrayNode = objectMapper.createArrayNode();

        arrayNode.add("goodToBad");
        arrayNode.add("goodToWarn");
        arrayNode.add("badToWarn");
        arrayNode.add("warnToGood");

        final Iterator<JsonNode> iterator = arrayNode.iterator();

        TransitionConfig transitionConfig = new DefaultTransitionConfig(iterator, false);
        AlertStateStore alertStateStore = AlertStateStores.withTransitions("id",
                300,transitionConfig);

        final String ns = "namespace";
        final TreeMap<String,String> tags = new TreeMap<String,String>() {{
            put("host","host1");
            put("user","user1");
        }};
        final long alertId = 1l;

        final AlertStateChange bad
                = alertStateStore.raiseAlert(ns, alertId, tags, AlertState.BAD);

        Assert.assertTrue(bad.raiseAlert());

        Assert.assertEquals(AlertState.BAD,alertStateStore.
                getCurrentState(AlertUtils.getHashForNAMT(ns,alertId, tags)));

        alertStateStore.newRun();

        final AlertStateChange warn
                = alertStateStore.raiseAlert(ns, alertId, tags, AlertState.WARN);

        Assert.assertTrue(warn.raiseAlert());

        Assert.assertEquals(AlertState.WARN,alertStateStore.
                getCurrentState(AlertUtils.getHashForNAMT(ns,alertId, tags)));

        alertStateStore.newRun();

        final AlertStateChange good
                = alertStateStore.raiseAlert(ns, alertId, tags, AlertState.GOOD);

        Assert.assertTrue(good.raiseAlert());

        Assert.assertEquals(AlertState.GOOD,alertStateStore.
                getCurrentState(AlertUtils.getHashForNAMT(ns,alertId, tags)));
    }

    @Test
    public void testRaiseBadAfterRecovery() throws InterruptedException {
        ObjectMapper objectMapper = new ObjectMapper();

        final ArrayNode arrayNode = objectMapper.createArrayNode();

        arrayNode.add("goodToBad");
        arrayNode.add("goodToWarn");
        arrayNode.add("badToGood");

        final Iterator<JsonNode> iterator = arrayNode.iterator();

        TransitionConfig transitionConfig = new DefaultTransitionConfig(iterator, false);
        AlertStateStore alertStateStore = AlertStateStores.withTransitions("id",
                300,transitionConfig);

        final String ns = "namespace";
        final TreeMap<String,String> tags = new TreeMap<String,String>() {{
            put("host","host1");
            put("user","user1");
        }};
        final long alertId = 1l;

        final AlertStateChange bad
                = alertStateStore.raiseAlert(ns, alertId, tags, AlertState.BAD);

        Assert.assertTrue(bad.raiseAlert());

        Assert.assertEquals(AlertState.BAD,alertStateStore.
                getCurrentState(AlertUtils.getHashForNAMT(ns,alertId, tags)));

        alertStateStore.newRun();

        final AlertStateChange good
                = alertStateStore.raiseAlert(ns, alertId, tags, AlertState.GOOD);

        Assert.assertTrue(good.raiseAlert());

        Assert.assertEquals(AlertState.GOOD,alertStateStore.
                getCurrentState(AlertUtils.getHashForNAMT(ns,alertId, tags)));

        Assert.assertEquals(AlertState.BAD,alertStateStore.
                getPreviousState(AlertUtils.getHashForNAMT(ns,alertId, tags)));

        alertStateStore.newRun();

        final AlertStateChange bad2
                = alertStateStore.raiseAlert(ns, alertId, tags, AlertState.BAD);

        Assert.assertTrue(bad2.raiseAlert());

        Assert.assertEquals(AlertState.BAD,alertStateStore.
                getCurrentState(AlertUtils.getHashForNAMT(ns,alertId, tags)));

        Assert.assertEquals(AlertState.GOOD,alertStateStore.
                getPreviousState(AlertUtils.getHashForNAMT(ns,alertId, tags)));
    }

    @Test
    public void testDoNotRaiseGoodAfterRecovery() throws InterruptedException {
        ObjectMapper objectMapper = new ObjectMapper();

        final ArrayNode arrayNode = objectMapper.createArrayNode();

        arrayNode.add("goodToBad");
        arrayNode.add("goodToWarn");
        arrayNode.add("badToGood");

        final Iterator<JsonNode> iterator = arrayNode.iterator();

        TransitionConfig transitionConfig = new DefaultTransitionConfig(iterator, false);
        AlertStateStore alertStateStore = AlertStateStores.withTransitions("id",
                1,transitionConfig);

        final String ns = "namespace";
        final TreeMap<String,String> tags = new TreeMap<String,String>() {{
            put("host","host1");
            put("user","user1");
        }};
        final long alertId = 1l;

        final AlertStateChange bad
                = alertStateStore.raiseAlert(ns, alertId, tags, AlertState.BAD);

        Assert.assertTrue(bad.raiseAlert());

        Assert.assertEquals(AlertState.BAD,alertStateStore.
                getCurrentState(AlertUtils.getHashForNAMT(ns,alertId, tags)));

        alertStateStore.newRun();

        final AlertStateChange good
                = alertStateStore.raiseAlert(ns, alertId, tags, AlertState.GOOD);

        Assert.assertTrue(good.raiseAlert());

        Assert.assertEquals(AlertState.GOOD,alertStateStore.
                getCurrentState(AlertUtils.getHashForNAMT(ns,alertId, tags)));

        Thread.sleep(2100);

        alertStateStore.newRun();

        final AlertStateChange good2
                = alertStateStore.raiseAlert(ns, alertId, tags, AlertState.GOOD);

        Assert.assertFalse(good2.raiseAlert());

        Assert.assertEquals(AlertState.GOOD,alertStateStore.
                getCurrentState(AlertUtils.getHashForNAMT(ns,alertId, tags)));

        Assert.assertEquals(AlertState.BAD,alertStateStore.
                getPreviousState(AlertUtils.getHashForNAMT(ns,alertId, tags)));
    }

    @Test
    public void testRaiseWarnAlert() {
        ObjectMapper objectMapper = new ObjectMapper();

        final ArrayNode arrayNode = objectMapper.createArrayNode();

        arrayNode.add("goodToBad");
        arrayNode.add("goodToWarn");
        arrayNode.add("badToGood");

        final Iterator<JsonNode> iterator = arrayNode.iterator();

        TransitionConfig transitionConfig = new DefaultTransitionConfig(iterator, false);
        AlertStateStore alertStateStore = AlertStateStores.withTransitions("id",
                300,transitionConfig);

        final String ns = "namespace";
        final TreeMap<String,String> tags = new TreeMap<String,String>() {{
            put("host","host1");
            put("user","user1");
        }};
        final long alertId = 1l;

        final AlertStateChange warn
                = alertStateStore.raiseAlert(ns, alertId, tags, AlertState.WARN);

        Assert.assertTrue(warn.raiseAlert());
    }

    @Test
    public void testRaisingMultipleAlertsInARunRepeatSameState() {

        ObjectMapper objectMapper = new ObjectMapper();

        final ArrayNode arrayNode = objectMapper.createArrayNode();

        arrayNode.add("goodToBad");
        arrayNode.add("goodToWarn");
        arrayNode.add("badToGood");

        final Iterator<JsonNode> iterator = arrayNode.iterator();

        TransitionConfig transitionConfig = new DefaultTransitionConfig(iterator, false);
        AlertStateStore alertStateStore = AlertStateStores.withTransitions("id",
                300,transitionConfig);

        final String ns = "namespace";
        final TreeMap<String,String> tags = new TreeMap<String,String>() {{
            put("host","host1");
            put("user","user1");
        }};
        final long alertId = 1l;

        final AlertStateChange bad
                = alertStateStore.raiseAlert(ns, alertId, tags, AlertState.BAD);

        Assert.assertTrue(bad.raiseAlert());

        final AlertStateChange bad1
                = alertStateStore.raiseAlert(ns, alertId, tags, AlertState.BAD);

        Assert.assertFalse(bad1.raiseAlert());

        final AlertStateChange warn
                = alertStateStore.raiseAlert(ns, alertId, tags, AlertState.WARN);

        Assert.assertFalse(warn.raiseAlert());

        final AlertStateChange good
                = alertStateStore.raiseAlert(ns, alertId, tags, AlertState.GOOD);

        Assert.assertFalse(good.raiseAlert());

        Assert.assertEquals(AlertState.BAD,alertStateStore.
                getCurrentState(AlertUtils.getHashForNAMT(ns,alertId, tags)));
    }

    @Test
    public void testDontNagForMultipleStates() {
        ObjectMapper objectMapper = new ObjectMapper();

        final ArrayNode arrayNode = objectMapper.createArrayNode();

        arrayNode.add("goodToBad");
        arrayNode.add("goodToWarn");
        arrayNode.add("badToWarn");

        final Iterator<JsonNode> iterator = arrayNode.iterator();

        TransitionConfig transitionConfig = new DefaultTransitionConfig(iterator, false);
        AlertStateStore alertStateStore = AlertStateStores.withTransitions("id",
                300,transitionConfig);

        final String ns = "namespace";
        final TreeMap<String,String> tags = new TreeMap<String,String>() {{
            put("host","host1");
            put("user","user1");
        }};
        final long alertId = 1l;

        final AlertStateChange bad
                = alertStateStore.raiseAlert(ns, alertId, tags, AlertState.BAD);

        Assert.assertTrue(bad.raiseAlert());

        alertStateStore.newRun();

        final AlertStateChange nag
                = alertStateStore.raiseAlert(ns, alertId, tags, AlertState.BAD);

        Assert.assertFalse(nag.raiseAlert());

        alertStateStore.newRun();

        final AlertStateChange warn
                = alertStateStore.raiseAlert(ns, alertId, tags, AlertState.WARN);

        Assert.assertTrue(bad.raiseAlert());

        alertStateStore.newRun();

        final AlertStateChange warnnag
                = alertStateStore.raiseAlert(ns, alertId, tags, AlertState.WARN);

        Assert.assertFalse(nag.raiseAlert());
    }

    @Test
    public void testProperNagForMultipleRuns() throws InterruptedException {
        ObjectMapper objectMapper = new ObjectMapper();

        final ArrayNode arrayNode = objectMapper.createArrayNode();

        arrayNode.add("goodToBad");
        arrayNode.add("goodToWarn");
        arrayNode.add("badToGood");

        final Iterator<JsonNode> iterator = arrayNode.iterator();

        TransitionConfig transitionConfig = new DefaultTransitionConfig(iterator, false);
        AlertStateStore alertStateStore = AlertStateStores.withTransitions("id",
                1,transitionConfig);

        final String ns = "namespace";
        final TreeMap<String,String> tags = new TreeMap<String,String>() {{
            put("host","host1");
            put("user","user1");
        }};
        final long alertId = 1l;

        final AlertStateChange bad
                = alertStateStore.raiseAlert(ns, alertId, tags, AlertState.BAD);

        Assert.assertTrue(bad.raiseAlert());

        Thread.sleep(2100);

        alertStateStore.newRun();

        final AlertStateChange nag
                = alertStateStore.raiseAlert(ns, alertId, tags, AlertState.BAD);

        Assert.assertTrue(nag.raiseAlert());
        alertStateStore.newRun();

        final AlertStateChange nag1
                = alertStateStore.raiseAlert(ns, alertId, tags, AlertState.BAD);

        Assert.assertFalse(nag1.raiseAlert());


        Thread.sleep(100);

        alertStateStore.newRun();

        final AlertStateChange nag2
                = alertStateStore.raiseAlert(ns, alertId, tags, AlertState.BAD);
        Assert.assertFalse(nag2.raiseAlert());

        Thread.sleep(2100);

        alertStateStore.newRun();

        final AlertStateChange nag3
                = alertStateStore.raiseAlert(ns, alertId, tags, AlertState.BAD);

        Assert.assertTrue(nag3.raiseAlert());

    }

    @Test
    public void testTransitionWithChange() throws InterruptedException {
        ObjectMapper objectMapper = new ObjectMapper();

        final ArrayNode arrayNode = objectMapper.createArrayNode();

        arrayNode.add("goodToBad");
        arrayNode.add("goodToWarn");
        arrayNode.add("badToWarn");
        arrayNode.add("warnToGood");

        final Iterator<JsonNode> iterator = arrayNode.iterator();

        TransitionConfig transitionConfig = new DefaultTransitionConfig(iterator, false);
        AlertStateStore alertStateStore = AlertStateStores.withTransitions("id",
                1,transitionConfig);

        final String ns = "namespace";
        final TreeMap<String,String> tags = new TreeMap<String,String>() {{
            put("host","host1");
            put("user","user1");
        }};
        final long alertId = 1l;

        final AlertStateChange bad
                = alertStateStore.raiseAlert(ns, alertId, tags, AlertState.BAD);

        Assert.assertTrue(bad.raiseAlert());

        Assert.assertEquals(AlertState.BAD,alertStateStore.
                getCurrentState(AlertUtils.getHashForNAMT(ns,alertId, tags)));

        alertStateStore.newRun();

        final AlertStateChange warn
                = alertStateStore.raiseAlert(ns, alertId, tags, AlertState.WARN);

        Assert.assertTrue(warn.raiseAlert());

        Assert.assertEquals(AlertState.WARN,alertStateStore.
                getCurrentState(AlertUtils.getHashForNAMT(ns,alertId, tags)));

        alertStateStore.newRun();

        final AlertStateChange good
                = alertStateStore.raiseAlert(ns, alertId, tags, AlertState.GOOD);

        Assert.assertTrue(good.raiseAlert());

        Assert.assertEquals(AlertState.GOOD,alertStateStore.
                getCurrentState(AlertUtils.getHashForNAMT(ns,alertId, tags)));



        final ArrayNode newArrayNode = objectMapper.createArrayNode();

        newArrayNode.add("goodToBad");
        newArrayNode.add("goodToWarn");
        newArrayNode.add("badToWarn");

        final Iterator<JsonNode> newIterator = newArrayNode.iterator();

        DefaultTransitionConfig newTransitionConfig = new DefaultTransitionConfig(newIterator, false);

        alertStateStore.setTransitionConfig(newTransitionConfig);

        alertStateStore.newRun();
        
        final AlertStateChange bad2
                = alertStateStore.raiseAlert(ns, alertId, tags, AlertState.BAD);
        System.out.println(bad2.getCurrentState() == AlertState.BAD);
        Assert.assertTrue(bad2.raiseAlert());

        Assert.assertEquals(AlertState.BAD,alertStateStore.
                getCurrentState(AlertUtils.getHashForNAMT(ns,alertId, tags)));

        alertStateStore.newRun();

        final AlertStateChange warn2
                = alertStateStore.raiseAlert(ns, alertId, tags, AlertState.WARN);

        Assert.assertTrue(warn2.raiseAlert());

        Assert.assertEquals(AlertState.WARN,alertStateStore.
                getCurrentState(AlertUtils.getHashForNAMT(ns,alertId, tags)));

        alertStateStore.newRun();

        final AlertStateChange good2
                = alertStateStore.raiseAlert(ns, alertId, tags, AlertState.GOOD);

        Assert.assertFalse(good2.raiseAlert());

        Assert.assertEquals(AlertState.GOOD,alertStateStore.
                getCurrentState(AlertUtils.getHashForNAMT(ns,alertId, tags)));


    }

    @Test
    public void testRaiseBadAfterRecoveryNotInTheSameRun() throws InterruptedException {
        ObjectMapper objectMapper = new ObjectMapper();

        final ArrayNode arrayNode = objectMapper.createArrayNode();

        arrayNode.add("goodToBad");
        arrayNode.add("goodToWarn");
        arrayNode.add("badToGood");

        final Iterator<JsonNode> iterator = arrayNode.iterator();

        TransitionConfig transitionConfig = new DefaultTransitionConfig(iterator, false);
        AlertStateStore alertStateStore = AlertStateStores.withTransitions("id",
                1,transitionConfig);

        final String ns = "namespace";
        final TreeMap<String,String> tags = new TreeMap<String,String>() {{
            put("host","host1");
            put("user","user1");
        }};
        final long alertId = 1l;

        final AlertStateChange bad
                = alertStateStore.raiseAlert(ns, alertId, tags, AlertState.BAD);

        Assert.assertTrue(bad.raiseAlert());
        Assert.assertEquals(AlertState.BAD,alertStateStore.
                getCurrentState(AlertUtils.getHashForNAMT(ns,alertId, tags)));

        alertStateStore.newRun();

        final AlertStateChange good
                = alertStateStore.raiseAlert(ns, alertId, tags, AlertState.GOOD);

        Assert.assertTrue(good.raiseAlert());
        Assert.assertEquals(AlertState.GOOD,alertStateStore.
                getCurrentState(AlertUtils.getHashForNAMT(ns,alertId, tags)));
        Assert.assertEquals(AlertState.BAD,alertStateStore.
                getPreviousState(AlertUtils.getHashForNAMT(ns,alertId, tags)));

        final AlertStateChange bad2
                = alertStateStore.raiseAlert(ns, alertId, tags, AlertState.BAD);

        Assert.assertFalse(bad2.raiseAlert());
        Assert.assertEquals(AlertState.GOOD,alertStateStore.
                getCurrentState(AlertUtils.getHashForNAMT(ns,alertId, tags)));
        Assert.assertEquals(AlertState.BAD,alertStateStore.
                getPreviousState(AlertUtils.getHashForNAMT(ns,alertId, tags)));

        alertStateStore.newRun();

        final AlertStateChange bad3
                = alertStateStore.raiseAlert(ns, alertId, tags, AlertState.BAD);

        Assert.assertTrue(bad3.raiseAlert());
        Assert.assertEquals(AlertState.BAD,alertStateStore.
                getCurrentState(AlertUtils.getHashForNAMT(ns,alertId, tags)));
        Assert.assertEquals(AlertState.GOOD,alertStateStore.
                getPreviousState(AlertUtils.getHashForNAMT(ns,alertId, tags)));
    }

    @Test
    public void testDoNotRaiseGoodFirstTime() throws InterruptedException {
        ObjectMapper objectMapper = new ObjectMapper();

        final ArrayNode arrayNode = objectMapper.createArrayNode();

        arrayNode.add("goodToBad");
        arrayNode.add("goodToWarn");
        arrayNode.add("badToGood");
        arrayNode.add("warnToGood");

        final Iterator<JsonNode> iterator = arrayNode.iterator();

        TransitionConfig transitionConfig = new DefaultTransitionConfig(iterator, false);
        AlertStateStore alertStateStore = AlertStateStores.withTransitions("id",
                1,transitionConfig);

        final String ns = "namespace";
        final TreeMap<String,String> tags = new TreeMap<String,String>() {{
            put("host","host1");
            put("user","user1");
        }};
        final long alertId = 1l;

        final AlertStateChange good
                = alertStateStore.raiseAlert(ns, alertId, tags, AlertState.GOOD);

        Assert.assertFalse(good.raiseAlert());

        Assert.assertEquals(AlertState.GOOD,alertStateStore.
                getCurrentState(AlertUtils.getHashForNAMT(ns,alertId, tags)));

        alertStateStore.newRun();

        final AlertStateChange bad
                = alertStateStore.raiseAlert(ns, alertId, tags, AlertState.BAD);

        Assert.assertTrue(bad.raiseAlert());

        Assert.assertEquals(AlertState.BAD,alertStateStore.
                getCurrentState(AlertUtils.getHashForNAMT(ns,alertId, tags)));

        Assert.assertEquals(AlertState.GOOD,alertStateStore.
                getPreviousState(AlertUtils.getHashForNAMT(ns,alertId, tags)));
    }

    @Test
    public void testAddNewTranistion() {
        ObjectMapper objectMapper = new ObjectMapper();

        final ArrayNode arrayNode = objectMapper.createArrayNode();

        arrayNode.add("goodToBad");
        arrayNode.add("goodToWarn");

        final Iterator<JsonNode> iterator = arrayNode.iterator();

        TransitionConfig transitionConfig = new DefaultTransitionConfig(iterator, false);
        AlertStateStore alertStateStore = AlertStateStores.withTransitions("id",
                300,transitionConfig);

        final String ns = "namespace";
        final TreeMap<String,String> tags = new TreeMap<String,String>() {{
            put("host","host1");
            put("user","user1");
        }};
        final long alertId = 1l;

        final AlertStateChange bad
                = alertStateStore.raiseAlert(ns, alertId, tags, AlertState.BAD);

        Assert.assertTrue(bad.raiseAlert());

        alertStateStore.newRun();

        final AlertStateChange good
                = alertStateStore.raiseAlert(ns, alertId, tags, AlertState.GOOD);

        Assert.assertFalse(good.raiseAlert());


        final ArrayNode changedNode = objectMapper.createArrayNode();

        changedNode.add("goodToBad");
        changedNode.add("goodToWarn");
        changedNode.add("badToGood");

        final Iterator<JsonNode> nodeIterator = changedNode.iterator();

        TransitionConfig changed = new DefaultTransitionConfig(nodeIterator, false);

        alertStateStore.setTransitionConfig(changed);

        alertStateStore.newRun();

        final AlertStateChange bad1
                = alertStateStore.raiseAlert(ns, alertId, tags, AlertState.BAD);

        Assert.assertTrue(bad1.raiseAlert());

        alertStateStore.newRun();

        final AlertStateChange good2
                = alertStateStore.raiseAlert(ns, alertId, tags, AlertState.GOOD);

        Assert.assertTrue(good2.raiseAlert());
    }

    @Test
    public void testMultipleTimeseries() {

        ObjectMapper objectMapper = new ObjectMapper();

        final ArrayNode arrayNode = objectMapper.createArrayNode();

        arrayNode.add("goodToBad");
        arrayNode.add("badToGood");

        final Iterator<JsonNode> iterator = arrayNode.iterator();

        TransitionConfig transitionConfig = new DefaultTransitionConfig(iterator, false);
        AlertStateStore alertStateStore = AlertStateStores.withTransitions("id",
                300,transitionConfig);

        final String ns = "namespace";
        final TreeMap<String,String> tags = new TreeMap<String,String>() {{
            put("host","host1");
            put("user","user1");
        }};
        final long alertId = 1l;

        final TreeMap<String,String> tags2 = new TreeMap<String,String>() {{
            put("host","host2");
            put("user","user1");
        }};

        final AlertStateChange bad
                = alertStateStore.raiseAlert(ns, alertId, tags, AlertState.BAD);

        Assert.assertTrue(bad.raiseAlert());

        final AlertStateChange good2
                = alertStateStore.raiseAlert(ns, alertId, tags2, AlertState.GOOD);

        Assert.assertFalse(good2.raiseAlert());

        alertStateStore.newRun();

        final AlertStateChange bad1
                = alertStateStore.raiseAlert(ns, alertId, tags, AlertState.BAD);

        Assert.assertFalse(bad1.raiseAlert());

        Assert.assertEquals(AlertState.BAD,alertStateStore.
                getCurrentState(AlertUtils.getHashForNAMT(ns,alertId, tags)));

        final AlertStateChange bad2
                = alertStateStore.raiseAlert(ns, alertId, tags2, AlertState.BAD);

        Assert.assertTrue(bad2.raiseAlert());

        Assert.assertEquals(AlertState.BAD,alertStateStore.
                getCurrentState(AlertUtils.getHashForNAMT(ns,alertId, tags2)));

    }

    @Test
    public void testNagWithConfigChange() throws InterruptedException {
        ObjectMapper objectMapper = new ObjectMapper();

        final ArrayNode arrayNode = objectMapper.createArrayNode();

        arrayNode.add("goodToBad");
        arrayNode.add("goodToWarn");
        arrayNode.add("badToGood");

        final Iterator<JsonNode> iterator = arrayNode.iterator();

        TransitionConfig transitionConfig = new DefaultTransitionConfig(iterator, false);
        AlertStateStore alertStateStore = AlertStateStores.withTransitions("id",
                300,transitionConfig);

        final String ns = "namespace";
        final TreeMap<String,String> tags = new TreeMap<String,String>() {{
            put("host","host1");
            put("user","user1");
        }};
        final long alertId = 1l;

        final AlertStateChange bad
                = alertStateStore.raiseAlert(ns, alertId, tags, AlertState.BAD);

        Assert.assertTrue(bad.raiseAlert());

        alertStateStore.newRun();

        final AlertStateChange nag
                = alertStateStore.raiseAlert(ns, alertId, tags, AlertState.BAD);

        Assert.assertFalse(nag.raiseAlert());


        alertStateStore.setNagIntervalInSecs(1);

        Thread.sleep(2100);

        alertStateStore.newRun();

        final AlertStateChange nag2
                = alertStateStore.raiseAlert(ns, alertId, tags, AlertState.BAD);

        Assert.assertTrue(nag2.raiseAlert());
    }

    @Test
    public void testAlertDetailContainerFields() throws InterruptedException {
        ObjectMapper objectMapper = new ObjectMapper();

        final ArrayNode arrayNode = objectMapper.createArrayNode();

        arrayNode.add("goodToBad");
        arrayNode.add("badToWarn");
        arrayNode.add("warnToGood");

        final Iterator<JsonNode> iterator = arrayNode.iterator();

        TransitionConfig transitionConfig = new DefaultTransitionConfig(iterator, false);
        AlertStateStore alertStateStore = AlertStateStores.withTransitions("id",
                300,transitionConfig);

        final String ns = "namespace";
        final TreeMap<String,String> tags = new TreeMap<String,String>() {{
            put("host","host1");
            put("user","user1");
        }};
        final long alertId = 1l;

        final AlertStateChange bad
                = alertStateStore.raiseAlert(ns, alertId, tags, AlertState.BAD);

        Assert.assertTrue(bad.raiseAlert());


        Assert.assertTrue(bad.getPreviousState() == AlertState.GOOD);
        Assert.assertTrue(bad.getCurrentState() == AlertState.BAD);

        alertStateStore.newRun();

        final AlertStateChange warn
                = alertStateStore.raiseAlert(ns, alertId, tags, AlertState.WARN);

        Assert.assertTrue(warn.raiseAlert());

        Assert.assertTrue(warn.getPreviousState() == AlertState.BAD);
        Assert.assertTrue(warn.getCurrentState() == AlertState.WARN);

        alertStateStore.newRun();

        final AlertStateChange good
                = alertStateStore.raiseAlert(ns, alertId, tags, AlertState.GOOD);

        Assert.assertTrue(good.raiseAlert());

        Assert.assertTrue(good.getPreviousState() == AlertState.WARN);
        Assert.assertTrue(good.getCurrentState() == AlertState.GOOD);
    }

    @Test
    public void testSkippedTransitions() throws InterruptedException {
        ObjectMapper objectMapper = new ObjectMapper();

        final ArrayNode arrayNode = objectMapper.createArrayNode();

        arrayNode.add("goodToBad");
        arrayNode.add("goodToWarn");
        arrayNode.add("warnToGood");

        final Iterator<JsonNode> iterator = arrayNode.iterator();

        TransitionConfig transitionConfig = new DefaultTransitionConfig(iterator, false);
        AlertStateStore alertStateStore = AlertStateStores.withTransitions("id",
                1,transitionConfig);

        final String ns = "namespace";
        final TreeMap<String,String> tags = new TreeMap<String,String>() {{
            put("host","host1");
            put("user","user1");
        }};
        final long alertId = 1l;

        final AlertStateChange bad
                = alertStateStore.raiseAlert(ns, alertId, tags, AlertState.BAD);

        Assert.assertTrue(bad.raiseAlert());

        Assert.assertEquals(AlertState.BAD,alertStateStore.
                getCurrentState(AlertUtils.getHashForNAMT(ns,alertId, tags)));

        alertStateStore.newRun();

        final AlertStateChange warn
                = alertStateStore.raiseAlert(ns, alertId, tags, AlertState.WARN);

        Assert.assertFalse(warn.raiseAlert());

        Assert.assertEquals(AlertState.WARN,alertStateStore.
                getCurrentState(AlertUtils.getHashForNAMT(ns,alertId, tags)));

        alertStateStore.newRun();

        final AlertStateChange good
                = alertStateStore.raiseAlert(ns, alertId, tags, AlertState.GOOD);

        Assert.assertTrue(good.raiseAlert());

        Assert.assertEquals(AlertState.GOOD,alertStateStore.
                getCurrentState(AlertUtils.getHashForNAMT(ns,alertId, tags)));
    }

    @Test
    public void testNagToNoNag() throws InterruptedException {
        ObjectMapper objectMapper = new ObjectMapper();

        final ArrayNode arrayNode = objectMapper.createArrayNode();

        arrayNode.add("goodToBad");
        arrayNode.add("goodToWarn");
        arrayNode.add("badToGood");

        final Iterator<JsonNode> iterator = arrayNode.iterator();

        TransitionConfig transitionConfig = new DefaultTransitionConfig(iterator, false);
        AlertStateStore alertStateStore = AlertStateStores.withTransitions("id",
                1,transitionConfig);

        final String ns = "namespace";
        final TreeMap<String,String> tags = new TreeMap<String,String>() {{
            put("host","host1");
            put("user","user1");
        }};
        final long alertId = 1l;

        final AlertStateChange bad
                = alertStateStore.raiseAlert(ns, alertId, tags, AlertState.BAD);

        Assert.assertTrue(bad.raiseAlert());

        Assert.assertEquals(AlertState.BAD,alertStateStore.
                getCurrentState(AlertUtils.getHashForNAMT(ns,alertId, tags)));

        Assert.assertEquals(AlertState.GOOD,alertStateStore.
                getPreviousState(AlertUtils.getHashForNAMT(ns,alertId, tags)));

        Thread.sleep(2100);

        alertStateStore.newRun();

        final AlertStateChange bad2
                = alertStateStore.raiseAlert(ns, alertId, tags, AlertState.BAD);

        Assert.assertTrue(bad2.raiseAlert());

        Assert.assertEquals(AlertState.BAD,alertStateStore.
                getCurrentState(AlertUtils.getHashForNAMT(ns,alertId, tags)));

        Assert.assertEquals(AlertState.GOOD,alertStateStore.
                getPreviousState(AlertUtils.getHashForNAMT(ns,alertId, tags)));

        alertStateStore.setNagIntervalInSecs(DO_NOT_NAG);

        alertStateStore.newRun();

        Thread.sleep(2100);

        final AlertStateChange bad3
                = alertStateStore.raiseAlert(ns, alertId, tags, AlertState.BAD);

        Assert.assertFalse(bad3.raiseAlert());

        Assert.assertEquals(AlertState.BAD,alertStateStore.
                getCurrentState(AlertUtils.getHashForNAMT(ns,alertId, tags)));

        Assert.assertEquals(AlertState.GOOD,alertStateStore.
                getPreviousState(AlertUtils.getHashForNAMT(ns,alertId, tags)));



    }

    @Test
    public void testNoNagToNag() throws InterruptedException {
        ObjectMapper objectMapper = new ObjectMapper();

        final ArrayNode arrayNode = objectMapper.createArrayNode();

        arrayNode.add("goodToBad");
        arrayNode.add("goodToWarn");
        arrayNode.add("badToGood");

        final Iterator<JsonNode> iterator = arrayNode.iterator();

        TransitionConfig transitionConfig = new DefaultTransitionConfig(iterator, false);
        AlertStateStore alertStateStore = AlertStateStores.withTransitions("id",
                DO_NOT_NAG,transitionConfig);

        final String ns = "namespace";
        final TreeMap<String,String> tags = new TreeMap<String,String>() {{
            put("host","host1");
            put("user","user1");
        }};
        final long alertId = 1l;

        final AlertStateChange bad
                = alertStateStore.raiseAlert(ns, alertId, tags, AlertState.BAD);

        Assert.assertTrue(bad.raiseAlert());

        Assert.assertEquals(AlertState.BAD,alertStateStore.
                getCurrentState(AlertUtils.getHashForNAMT(ns,alertId, tags)));

        Assert.assertEquals(AlertState.GOOD,alertStateStore.
                getPreviousState(AlertUtils.getHashForNAMT(ns,alertId, tags)));

        Thread.sleep(2100);

        alertStateStore.newRun();

        final AlertStateChange bad2
                = alertStateStore.raiseAlert(ns, alertId, tags, AlertState.BAD);

        Assert.assertFalse(bad2.raiseAlert());

        Assert.assertEquals(AlertState.BAD,alertStateStore.
                getCurrentState(AlertUtils.getHashForNAMT(ns,alertId, tags)));

        Assert.assertEquals(AlertState.GOOD,alertStateStore.
                getPreviousState(AlertUtils.getHashForNAMT(ns,alertId, tags)));

        alertStateStore.setNagIntervalInSecs(1);

        alertStateStore.newRun();

        Thread.sleep(2100);

        final AlertStateChange bad3
                = alertStateStore.raiseAlert(ns, alertId, tags, AlertState.BAD);

        Assert.assertTrue(bad3.raiseAlert());

        Assert.assertEquals(AlertState.BAD,alertStateStore.
                getCurrentState(AlertUtils.getHashForNAMT(ns,alertId, tags)));

        Assert.assertEquals(AlertState.GOOD,alertStateStore.
                getPreviousState(AlertUtils.getHashForNAMT(ns,alertId, tags)));
    }

}
