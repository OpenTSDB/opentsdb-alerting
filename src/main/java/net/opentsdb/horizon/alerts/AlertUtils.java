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

package net.opentsdb.horizon.alerts;

import javax.xml.bind.DatatypeConverter;
import java.io.IOException;
import java.math.BigDecimal;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;

import net.openhft.hashing.LongHashFunction;
import net.opentsdb.horizon.alerts.model.AlertEventBag;
import net.opentsdb.horizon.alerts.config.AlertConfig;
import net.opentsdb.horizon.alerts.config.AlertConfigFactory;
import net.opentsdb.horizon.alerts.config.AlertConfigFields;
import net.opentsdb.horizon.alerts.config.NotificationConfig;
import net.opentsdb.horizon.alerts.config.impl.DefaultTransitionConfig;
import net.opentsdb.horizon.alerts.config.impl.HealthCheckConfig;
import net.opentsdb.horizon.alerts.config.impl.HealthCheckConfigFields;
import net.opentsdb.horizon.alerts.config.impl.MetricAlertConfig;
import net.opentsdb.horizon.alerts.config.impl.MetricAlertConfigFields;
import net.opentsdb.horizon.alerts.enums.AlertState;
import net.opentsdb.horizon.alerts.enums.ComparatorType;
import net.opentsdb.horizon.alerts.enums.SummaryType;
import net.opentsdb.horizon.alerts.enums.WindowSampler;
import net.opentsdb.horizon.alerts.model.AlertEvent;
import net.opentsdb.horizon.alerts.model.HealthCheckAlertEvent;
import net.opentsdb.horizon.alerts.model.Recipient;
import net.opentsdb.horizon.alerts.model.SingleMetricAlertEvent;
import net.opentsdb.horizon.alerts.model.SummarySingleMetricAlertEvent;
import net.opentsdb.horizon.alerts.model.tsdb.Datum;
import net.opentsdb.horizon.alerts.model.tsdb.IMetric;
import net.opentsdb.horizon.alerts.model.tsdb.Metric;
import net.opentsdb.horizon.alerts.model.tsdb.Tags;
import net.opentsdb.horizon.alerts.model.tsdb.YmsStatusEvent;
import net.opentsdb.horizon.alerts.processor.impl.StatusWriter;
import net.opentsdb.horizon.alerts.query.tsdb.TSDV3Constants;
import net.opentsdb.horizon.alerts.state.AlertStateChange;
import net.opentsdb.horizon.alerts.state.AlertStateStore;
import org.apache.logging.log4j.util.BiConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class AlertUtils {

    public static final ObjectMapper jsonMapper = new ObjectMapper();
    public static final String ALERTS_APPLICATION = "ALERTS";
    private static final double RECOVERY_THRESHOLD_FACTOR = 0.000001;
    private static final String ABOVE = "above";
    private static final String BELOW = "below";
    private static final String EQUAL = "equal";
    private static final String EQUALS = "equals";
    private static final String TO_FORMAT = "%s or %s";
    private static final String ABOVE_OR_EQUALS = String.format(TO_FORMAT,ABOVE,EQUALS);
    private static final String BELOW_OR_EQUALS = String.format(TO_FORMAT,BELOW,EQUALS);

    private static final String GREATER_THAN = ">";
    private static final String LESS_THAN = "<";
    private static final String SYMBOL_EQUALS = "=";
    private static final String GREATER_THAN_OR_EQUALS = ">=";
    private static final String LESS_THAN_OR_EQUALS = "<=";

    public static final String STATUS_OK = "status_ok";
    public static final String STATUS_BAD = "status_bad";
    public static final String STATUS_WARN= "status_warn";
    public static final String STATUS_UNK = "status_unk";
    public static final String STATUS_MISSING = "status_missing";

    public static final String SINK_KAFKA = "kafka";

    public static int dataFrequencyInSecs = 60;

    public static int defaultEvaluationDelayInMins = 2;

    public static int DO_NOT_NAG = -1;

    private static LongHashFunction hashFunction = LongHashFunction.xx();

    private static final Logger LOG = LoggerFactory.getLogger(AlertUtils.class);

    private static final int metricRoundoff = 2;

    private static final int metricRoundoffScale = 4;

    private static final String SUB_TYPE_EVENT_ALERT = "eventAlert";

    private static final int queryIndex = 0;

    private static final String EVENT_ALERT_MESSAGE = "hits %s (observed) %s %s (threshold)";

    public static final long runTimeReset = 3600*24*365;


    public static long getBatchTime(long time, int runFreq) {
        return (time - (time % runFreq));
    }

    public static long getDefaultLastRunTimeSecs(int runFrequencyInSecs) {

        return (getBatchTime(
                System.currentTimeMillis()/1000,
                runFrequencyInSecs) - runFrequencyInSecs);

    }
    
    public static long getDefaultLastRunTimeSecs(){
        return getDefaultLastRunTimeSecs(dataFrequencyInSecs);
    }

    public static boolean isValidRunTimeSecs(long runTimeSecs) {
        final long currTimeSecs = System.currentTimeMillis() / 1000;

        if(runTimeSecs < (currTimeSecs - runTimeReset)) {
            return false;
        }
        return true;
    }

    public static AlertConfig loadConfig(final String json) throws IOException {

        final JsonNode root = parseJsonTree(json);

        final int alertid = root.get(AlertConfigFields.ALERT_ID).asInt();

        final String name = root.get(AlertConfigFields.NAME).asText();

        final String namespace = root.get(AlertConfigFields.NAMESPACE).asText();

        final long last_modified = root.get(AlertConfigFields.UPDATED_TIME).asLong();

        final String type = root.get(AlertConfigFields.TYPE).asText();

        final JsonNode thresholdNode = root.get(AlertConfigFields.THRESHOLD);

        final String ttype;
        if (thresholdNode.hasNonNull(AlertConfigFields.SUB_TYPE)) {
            ttype = thresholdNode.get(AlertConfigFields.SUB_TYPE).asText();
        } else if (thresholdNode.hasNonNull(HealthCheckConfigFields.HEALTHCHECK)) {
            ttype = HealthCheckConfigFields.HEALTHCHECK;
        } else if (thresholdNode.hasNonNull(MetricAlertConfigFields.SINGLE_METRIC)) {
            ttype = MetricAlertConfigFields.SINGLE_METRIC;
        } else if (thresholdNode.hasNonNull(SUB_TYPE_EVENT_ALERT)) {
            ttype = SUB_TYPE_EVENT_ALERT;
        }
        else {
            throw new RuntimeException("Unable to infer subtype");
        }

        final boolean enabled = root.get(AlertConfigFields.ENABLED).asBoolean();

        final List<String> labelList;
        if (root.hasNonNull(AlertConfigFields.LABELS)) {
            labelList = new ArrayList<>();
            Iterator<JsonNode> labels = root.get(AlertConfigFields.LABELS).elements();
            while (labels.hasNext()) {
                labelList.add(labels.next().asText());
            }
        } else {
            labelList = Collections.emptyList();
        }

        /**
         * This node is type specific
         */
        final JsonNode thresholdPropertiesNode = thresholdNode.get(ttype);

        // Main query
        final String queryType = thresholdPropertiesNode.get(AlertConfigFields.QUERY_TYPE).asText();
        final JsonNode queryJson = root.get(AlertConfigFields.QUERIES).get(queryType).get(queryIndex);
        
        String md5BaseQueryString = null;
        try {
            md5BaseQueryString = AlertUtils.getMD5StringOfString(
                    root.get(AlertConfigFields.QUERIES).get(queryType).get(queryIndex).toString());
        } catch (NoSuchAlgorithmException | IOException e) {
            LOG.error("Error calculating MD5: ",e);
        }

        final NotificationConfig notificationConfig = parseNotificationConfig(namespace,alertid, name,root);

        AlertConfig alertConfig = AlertConfigFactory.getAlertConfig(namespace,type,ttype,alertid,last_modified);
        alertConfig.setAlertName(name);
        alertConfig.setLast_modified(last_modified);
        alertConfig.setEnabled(enabled);
        alertConfig.setLabels(labelList);
        //alertConfig.setMiscFields(miscFields);
        alertConfig.setNotificationConfig(notificationConfig);
        // Alert specific config
        alertConfig.setQueryJson(queryJson);
        alertConfig.setMd5BaseQueryString(md5BaseQueryString);

        if(thresholdPropertiesNode.has(AlertConfigFields.BAD_THRESHOLD)) {
            final JsonNode badNode = thresholdPropertiesNode.get(AlertConfigFields.BAD_THRESHOLD);
            final String s = badNode.asText();

            if(!AlertUtils.isEmpty(s) && !badNode.isNull()) {
                alertConfig.setHasBadThreshold(true);
                alertConfig.setBadThreshold(thresholdPropertiesNode.get(AlertConfigFields.BAD_THRESHOLD).asDouble());
            }
        }

        if(thresholdPropertiesNode.has(AlertConfigFields.WARN_THRESHOLD)) {
            final JsonNode warnNode = thresholdPropertiesNode.get(AlertConfigFields.WARN_THRESHOLD);
            final String s = warnNode.asText();

            if(!AlertUtils.isEmpty(s) && !warnNode.isNull()) {
                alertConfig.setHasWarnThreshold(true);
                alertConfig.setWarnThreshold(warnNode.asDouble());
            }
        }
        String s = null;
        if(thresholdPropertiesNode.has(AlertConfigFields.RECOVERY_THRESHOLD)) {
            final JsonNode recoveryNode = thresholdPropertiesNode.get(AlertConfigFields.RECOVERY_THRESHOLD);
            s = recoveryNode.asText();

            if(!AlertUtils.isEmpty(s) && !recoveryNode.isNull()) {

                alertConfig.setHasRecoveryThreshold(true);
                alertConfig.setRecoveryThreshold(recoveryNode.asDouble());
            }
        }

        if (thresholdNode.has(AlertConfigFields.IS_NAG_ENABLED)
                && thresholdNode.get(AlertConfigFields.IS_NAG_ENABLED).asBoolean(false)) {
            alertConfig.setNagIntervalInSecs(
                    thresholdNode.get(AlertConfigFields.NAG_INTERVAL).asInt()
            );
        } else {
            alertConfig.setNagIntervalInSecs(DO_NOT_NAG);
        }

        final boolean notifyOnMissing;
        if (thresholdNode.has(AlertConfigFields.NOTIFY_ON_MISSING)) {
            notifyOnMissing = thresholdNode.get(AlertConfigFields.NOTIFY_ON_MISSING).asBoolean(false);
        } else {
            notifyOnMissing = false;
        }
        alertConfig.setMissingEnabled(notifyOnMissing);

        if (thresholdNode.has(AlertConfigFields.DELAY_EVALUATION)) {
            alertConfig.setEvaluationDelayInMins(
                    thresholdNode.get(AlertConfigFields.DELAY_EVALUATION).asInt(defaultEvaluationDelayInMins));
        }

        final JsonNode notificationNode = root.get(AlertConfigFields.NOTIFICATION);

        final JsonNode transitionsToNotifyNode = notificationNode.get(AlertConfigFields.TRANSITIONS_TO_NOTIFY);

        alertConfig.setTransitionConfig(new DefaultTransitionConfig(transitionsToNotifyNode.elements(),
                        notifyOnMissing));

        alertConfig.parseAlertSpecific(root);
        
        return alertConfig;

    }

    public static JsonNode parseJsonTree(String json) throws IOException {
        return jsonMapper.readTree(json);
    }

    public static NotificationConfig parseNotificationConfig(String namespace, int alertid, String name, final JsonNode root) {


        final JsonNode notificationNode = root.get(AlertConfigFields.NOTIFICATION);

        final String subject = notificationNode.get(AlertConfigFields.SUBJECT).asText();
        final String body = notificationNode.get(AlertConfigFields.BODY).asText();

        final List<Recipient> recipientList = new ArrayList<>();

        final JsonNode recipients = notificationNode.get(AlertConfigFields.RECIPIENTS);

        final Iterator<String> stringIterator = recipients.fieldNames();

        //Recipient list
        while(stringIterator.hasNext()) {

            String type = stringIterator.next();

            final JsonNode typeList = recipients.get(type);

            final Iterator<JsonNode> typeRecipients = typeList.elements();

            while(typeRecipients.hasNext()) {
                Recipient recipient = new Recipient(type, typeRecipients.next().get(AlertConfigFields.RECIPIENTS_NAME).asText());
                recipientList.add(recipient);
            }
        }

        //Grouping rules

        final List<String> groupingRules = new ArrayList<>();

        final JsonNode groupingNode = root.get(AlertConfigFields.ALERT_GROUPING_RULES);


        final Iterator<JsonNode> elements = groupingNode.elements();

        while(elements.hasNext()) {
            groupingRules.add(elements.next().asText());
        }


        NotificationConfig notificationConfig = new NotificationConfig(namespace,alertid,name);
        notificationConfig.setSubject(subject);
        notificationConfig.setBody(body);
        notificationConfig.setRecipients(recipientList);

        notificationConfig.setGroupingRules(groupingRules);

        return notificationConfig;

    }

    public static long getXXHash(long... values) {
        return hashFunction.hashLongs(values);
    }


    public static long getHashForNAMT(String namespace, long alertId, SortedMap<String, String> tags) {

        StringBuilder build = new StringBuilder();
        build.append(namespace);
        for(String tag : tags.keySet()){
            build.append(tag);
            build.append(tags.get(tag));
        }

        return getXXHash(alertId,782738273l,hashFunction.hashChars(build));

    }

    public static List<String> getNamespacesFromResponse(String response) throws IOException {

        List<String> namespaces = new ArrayList<>();
        JsonNode root = jsonMapper.readTree(response);
        root.elements()
                .forEachRemaining(element -> namespaces.add(element.get("name").asText()));
        return namespaces;
    }

    public static boolean isEmpty(String property) {

        if(property != null && !property.isEmpty()) {
            return false;
        }
        return true;

    }

    public static String prefixHttps(String host) {
        String urlPath = host;
        if(!host.startsWith("http://")) {
            if(!host.startsWith("https://")) {
                urlPath = "https://" + host;

            }
        }
        

        return urlPath;
    }

    public static String suffixPath(String tsdbHost, String queryPath) {

        if(tsdbHost.endsWith("/")) {
            if(queryPath.startsWith("/")) {
                return tsdbHost + queryPath.substring(1);
            } else {
                return tsdbHost + queryPath;
            }
        } else {
            if(queryPath.startsWith("/")) {
                return tsdbHost + queryPath;
            } else {
                return tsdbHost + "/" +queryPath;
            }
        }

    }

    public static String getURL(String tsdbHost, String tsdbQueryPath) {


        String prefixHttps = AlertUtils.prefixHttps(tsdbHost);

        return AlertUtils.suffixPath(prefixHttps,tsdbQueryPath);
    }

    public static String getMD5StringOfString(String query) throws NoSuchAlgorithmException, IOException {

        MessageDigest md = MessageDigest.getInstance("MD5");
        md.update(query.getBytes());
        byte[] digest = md.digest();
        return DatatypeConverter
                .printHexBinary(digest).toUpperCase();

    }

    public static AlertEvent createAlertEvent(final long hashForNAMT,
                                              final AlertConfig metricAlertConfig,
                                              final AlertState alertState,
                                              final String tsField,
                                              final AlertStateStore alertStateStore,
                                              final SortedMap<String, String> tagMap) {

        AlertEvent alertEvent = null;
        final AlertStateChange stageChange = alertStateStore.raiseAlert(metricAlertConfig.getNamespace()
                , metricAlertConfig.getAlertId(), tagMap, alertState);
        if (stageChange.raiseAlert()) {
            alertEvent = AlertUtils.createAlertEvent(hashForNAMT,
                    tsField, tagMap, alertState,
                    metricAlertConfig);
            alertEvent.setOriginSignal(stageChange.getPreviousState());
            alertEvent.setNag(stageChange.isNag());
            LOG.info("id: {} Alert event: {} hash {} have put in current map",
                    metricAlertConfig.getAlertId(), alertEvent.toString(),
                    hashForNAMT);
        }
        return alertEvent;

    }

    public static AlertEvent createAlertEvent(final long alertHash,
                                              final String tsField,
                                              final SortedMap<String, String> tags,
                                              final AlertState alertType,
                                              final AlertConfig alertConfig) {

        final AlertEvent alertEvent = alertConfig.createAlertEvent(alertHash,
                                                    tsField,
                                                    tags,alertType);
        alertEvent.setAlertId(alertConfig.getAlertId());
        if(alertEvent.getAlertRaisedTimestamp() == 0) {
            alertEvent.setAlertRaisedTimestamp(Long.parseLong(tsField));
        }
        alertEvent.setCurrentSignal(alertType);
        alertEvent.setNamespace(alertConfig.getNamespace());
        alertEvent.setTags(tags);
        alertEvent.setAlertHash(alertHash);

        //alertEvent.setAlertDetails(String.format("%s %s %s (threshold = %s)"), nam,comparator,String.valueOf(bad_threshold));

        return alertEvent;
    }

    public static SingleMetricAlertEvent createSingleMetricAlertEvent(final String tsField,
                                                                      final SortedMap<String, String> tags,
                                                                      final AlertState signal,
                                                                      final MetricAlertConfig metricAlertConfig) {

        long alertRaisedTime = Long.parseLong(tsField) + metricAlertConfig.getSlidingWindowInSecs();

        final SingleMetricAlertEvent alertEvent;
        if(metricAlertConfig.getWindowSampler() != WindowSampler.SUMMARY) {
            alertEvent = new SingleMetricAlertEvent();
        } else {
            alertEvent = new SummarySingleMetricAlertEvent();
            ((SummarySingleMetricAlertEvent) alertEvent).
                    setSummaryType(metricAlertConfig.getSummarizer());
        }
        alertEvent.setComparator(ComparatorType.getComparatorTypeFromOperator
                (metricAlertConfig.getComparisonOperator()));
        alertEvent.setWindowSampler(metricAlertConfig.getWindowSampler());
        alertEvent.setAlertRaisedTimestamp(alertRaisedTime);

        double thresholdToSet = 0d;
        switch (signal) {

            case BAD:
                thresholdToSet = metricAlertConfig.getBadThreshold();
                break;
            case WARN:
                thresholdToSet = metricAlertConfig.getWarnThreshold();
                break;
            case GOOD:
                thresholdToSet = metricAlertConfig.getRecoveryThreshold();
                alertEvent.setComparator(ComparatorType.getComparatorTypeFromOperator
                        (metricAlertConfig.getFlippedComparisionOperator()));
                alertEvent.setWindowSampler(metricAlertConfig.getWindowSampler().flip());
                break;
            case MISSING:
                thresholdToSet = Double.NaN;
                break; //Not needed
        }
        alertEvent.setThreshold(thresholdToSet);
        alertEvent.setWindowSize((int)metricAlertConfig.getSlidingWindowInSecs());

        return alertEvent;
    }

    public static Double soothMetricValue(final Double metricValue) {
        try {
            DecimalFormat df = new DecimalFormat("0", DecimalFormatSymbols.getInstance(Locale.ENGLISH));
            df.setMaximumFractionDigits(340);

            String valueStr = df.format(metricValue.doubleValue());

            if (valueStr.split("\\.").length > 1) {
                String sl = valueStr.split("\\.")[1];
                int index = 0;
                if (sl.startsWith("0")) {
                    StringBuilder finalMetricVal = new StringBuilder();
                    finalMetricVal.append(valueStr.split("\\.")[0]);
                    finalMetricVal.append(".");

                    while (String.valueOf(sl.charAt(index++)).equals("0")) {
                        finalMetricVal.append("0");
                    }
                    int roundOff = metricRoundoff;
                    if (index == 2) {
                        roundOff++;
                    }
                    String roundoffString = sl.substring(index - 1, sl.length());
                    if (roundoffString.length() > roundOff) {
                        roundoffString = roundoffString.substring(0, roundOff);
                    }
                    finalMetricVal.append(roundoffString);
                    Double doub = Double.valueOf(finalMetricVal.toString());
                    return doub;
                } else {
                    Double metricValueRounded =
                            new BigDecimal(metricValue).setScale(metricRoundoffScale, BigDecimal.ROUND_HALF_EVEN)
                                    .doubleValue();
                    return metricValueRounded;
                }
            }
            return metricValue;
        } catch (Exception e) {
            LOG.error("Error while soothing metric, returning original metric value");
            return metricValue;
        }
    }

    public static String stripTrailingZeros(Double value) {
        try {
            return new BigDecimal(value.toString()).stripTrailingZeros().toPlainString();
        } catch (Throwable t) {
            LOG.error("Unable to strip trailing zeros for " + value);
            return String.valueOf(value);
        }
    }

    public static String getWordFromComparator(String comparator) {
        String strToFormat = null;
        switch (comparator) {
            case LESS_THAN:
                strToFormat = BELOW;
                break;
            case GREATER_THAN:
                strToFormat = ABOVE;
                break;
            case LESS_THAN_OR_EQUALS:
                strToFormat = BELOW_OR_EQUALS;
                break;
            case GREATER_THAN_OR_EQUALS:
                strToFormat = ABOVE_OR_EQUALS;
                break;
        }

        return strToFormat;
    }

    public static String getComparatorFromWord(String comparisonString) {
        String comparison_operator = null;
        if(comparisonString.contains(ABOVE)) {
            comparison_operator = GREATER_THAN;
        } else if(comparisonString.contains(BELOW)) {
            comparison_operator = LESS_THAN;
        }

        if(comparisonString.contains(EQUAL)) {
            if(comparison_operator == null) {
                comparison_operator = SYMBOL_EQUALS;
            } else {
                comparison_operator += SYMBOL_EQUALS;
            }
        }

        return comparison_operator;
    }

    public static String flipOperator(String comparison_operator) {

        switch (comparison_operator) {

            case GREATER_THAN:
                return LESS_THAN_OR_EQUALS;
            case LESS_THAN:
                return GREATER_THAN_OR_EQUALS;
            case GREATER_THAN_OR_EQUALS:
                return LESS_THAN;
            case LESS_THAN_OR_EQUALS:
                return GREATER_THAN;

        }
        throw new AlertRuntmeException("Unsupported operator: "+ comparison_operator);
    }

    public static double calculateRecoveryThreshold(double warnThreshold, String comparisonString) {

        if (comparisonString.contains(ABOVE)) {
            return warnThreshold - warnThreshold * RECOVERY_THRESHOLD_FACTOR;
        } else {
            if(comparisonString.contains(BELOW)) {
                return warnThreshold + warnThreshold * RECOVERY_THRESHOLD_FACTOR;
            }
        }
        throw new AlertRuntmeException("Unsupported operator: "+ comparisonString);
    }

    public static int getIntervalAsInt(String interval, final long start, final long end) throws AlertException {
        final String intervalFmt = interval.toLowerCase();
        try {
            if (intervalFmt.endsWith("m")) {
                return getInterval(intervalFmt, "m", TSDV3Constants.SECS_IN_MIN);
            } else if (intervalFmt.endsWith("h")) {
                return getInterval(intervalFmt, "h", TSDV3Constants.SECS_IN_HOUR);
            } else if (intervalFmt.endsWith("d")) {
                return getInterval(intervalFmt, "d", TSDV3Constants.SECS_IN_DAY);
            } else if (intervalFmt.endsWith("s")) {
                return getInterval(intervalFmt, "s", 1);
            } else if (intervalFmt.endsWith("min")) {
                return getInterval(intervalFmt, "min", TSDV3Constants.SECS_IN_MIN);
            } else if (intervalFmt.endsWith("secs")) {
                return getInterval(intervalFmt, "secs", 1);
            } else if (intervalFmt.endsWith("hr")) {
                return getInterval(intervalFmt, "hr", TSDV3Constants.SECS_IN_HOUR);
            } else if (intervalFmt.endsWith("day")) {
                return getInterval(intervalFmt, "day", TSDV3Constants.SECS_IN_DAY);
            } else if (intervalFmt.endsWith("all")) {
                return getInterval(start, end);
            } else {
                throw new AlertException(String.format("Error parsing interval %s", interval));
            }

        } catch (Exception e) {
            LOG.error("Error parsing {}", interval, e);
            throw new AlertException(String.format("Error parsing interval %s", interval), e);
        }
    }

    private static int getInterval(String toSplit, String split, int factor) {
        return Integer.parseInt(toSplit.split(split)[0]) * factor;
    }
    
    private static int getInterval(long start, long end) {
        return (int) (end - start);
    }

    public static long[] getTimestampsFromInterval(final long startTime, final long endTime, final long intervalInSecs,
                                                   final int sizeOfValues) {

        int size = (int)(((endTime - startTime)/intervalInSecs));

        final long endTimeToUse;

        if(sizeOfValues > size) {
            //start inclusive
            size = sizeOfValues;
            endTimeToUse = endTime;
        } else {

            endTimeToUse = endTime - intervalInSecs;
        }

        if(size > 0) {
            long[] vals = new long[size];

            long val_to_val = endTime;
            int index = size - 1;
            while (index >= 0 ) {
                vals[index] = val_to_val;
                val_to_val -= intervalInSecs;
                index--;
            }
            LOG.debug("Returning vals: {} for {} {} {} {}",Arrays.toString(vals),startTime,endTime,intervalInSecs, size);
            return vals;
        }
        return new long[0];
    }

    public static void updateAlertValues(List<Double> valueForTheTimeseries, AlertEvent alertEvent, boolean isSummary) {
        final double[] doublesFromCurrent = valueForTheTimeseries.stream().
                mapToDouble(Double::valueOf).toArray();
        if(isSummary) {
            if(alertEvent instanceof SummarySingleMetricAlertEvent) {
                ((SummarySingleMetricAlertEvent) alertEvent).setSummaryValues(doublesFromCurrent);
            }
        } else if (alertEvent instanceof SingleMetricAlertEvent){
            ((SingleMetricAlertEvent) alertEvent).absorbValues(doublesFromCurrent);
        }
    }

    public static double[] slidingWindow(double[] fullValues, SummaryType summarizer,
                                         long summaryInterval, long baseInterval) {

        final int indicesToSummarize = (int)(summaryInterval/baseInterval);
        LOG.info("Indices to summarize: "+ indicesToSummarize + " full values: "+ fullValues.length);
        final double[] returnVal;
        if(fullValues.length >= indicesToSummarize) {
            returnVal = new double[(fullValues.length - indicesToSummarize)+1];
        } else if (fullValues.length == 0) {
            return new double[0];
        } else {
            returnVal = new double[1];
        }

        for(int i = (fullValues.length -1), j = (fullValues.length - indicesToSummarize)
            ; (i - indicesToSummarize) >= 0 ; i--,j--  ) {

            returnVal[j] = summarize(summarizer,fullValues, i - indicesToSummarize,i);

        }

        return returnVal;
    }

    private static double summarize(SummaryType summaryType, double[] fullValues, int start, int end) {

        double sum = 0;
        int count = 0;
        for(int j = start; j <= end ; j++) {
            if(fullValues[j] != Double.NaN) {
                sum += fullValues[j];
                count++;
            }
        }

        if(count == 0) {
            return 0d;
        }

        if(summaryType == SummaryType.AVG) {
            return (sum/count);
        } else {
            return sum;
        }



    }

    public static void setMetricName(AlertEvent alertEvent, String metricName) {
        if (alertEvent instanceof SingleMetricAlertEvent) {
            ((SingleMetricAlertEvent) alertEvent).setMetricName(metricName);
        }
    }

    public static AlertEvent createHealthCheckAlertEvent(String tsField, SortedMap<String, String> tags,
                                                         AlertState signal, HealthCheckConfig healthCheckConfig) {

        return new HealthCheckAlertEvent();
    }

    public static String getMessageForMissing(long storedLastSeenTime) {
        final LocalDateTime formattedDateTime = Instant.ofEpochSecond
                (storedLastSeenTime).atZone(ZoneId.of("UTC")).toLocalDateTime();
        return "No data received since "+ formattedDateTime.toString();
    }

    public static String getMessageForMissingRecovery(long storedLastSeenTime) {
        final LocalDateTime formattedDateTime = Instant.ofEpochSecond
                (storedLastSeenTime).atZone(ZoneId.of("UTC")).toLocalDateTime();
        return "Recovered from missing data at "+ formattedDateTime.toString();
    }

    public static String getMessageForAutoRecovery(AlertState prevState, long storedLastSeenTime) {
        final LocalDateTime formattedDateTime = Instant.ofEpochSecond
                (storedLastSeenTime).atZone(ZoneId.of("UTC")).toLocalDateTime();
        return "Auto Recovered from " +prevState.name()+" state last seen at"+ formattedDateTime.toString();
    }

    public static void writeStatus(final StatusWriter statusWriter,
                                   final AlertConfig alertConfig,
                                   final long timestamp,
                                   final String namespace,
                                   final AlertState alertState,
                                   final long alertId,
                                   final SortedMap<String, String> tagMap,
                                   final String input_status_msg) {
        writeStatus(statusWriter,
                alertConfig.getAlertName(),
                alertConfig.getAlertType().getString(),
                timestamp,
                namespace,
                alertState,
                alertId,
                tagMap,
                input_status_msg);

    }

    public static void writeStatus(final StatusWriter statusWriter,
                                   final String alertName,
                                   final String alertType,
                                   final long timestamp,
                                   final String namespace,
                                   final AlertState alertState,
                                   final long alertId,
                                   final SortedMap<String, String> tagMap,
                                   final String input_status_msg) {

        final Map<String, IMetric> statusMap = getStatusMap(alertState);

        final TreeMap<String, String> stringStringTreeMap = new TreeMap<>();
        stringStringTreeMap.putAll(tagMap);

        removeNsAndAppFromMap(stringStringTreeMap);

        final String status_msg;

        if(input_status_msg != null) {
            status_msg = input_status_msg;
        } else {
            status_msg = "alert " + alertId + " in state " + alertState.name();
        }

        addSystemTags(
                stringStringTreeMap,
                alertId,
                alertName,
                alertType)
        ;

        final Datum datum = Datum.newBuilder()
                .withCluster(namespace)
                .withApplication(ALERTS_APPLICATION)
                .withTags(new Tags(Collections.unmodifiableMap(stringStringTreeMap)))
                .withMetrics(statusMap)
                .withStatus_code(null)
                .withStatus_msg(status_msg)
                .withTimestamp(timestamp)
                .build();
        YmsStatusEvent ymsStatusEvent = new YmsStatusEvent();
        ymsStatusEvent.setData(datum);
        ymsStatusEvent.setAdditionalProperty("ALERT_SOURCE","alert");
        statusWriter.process(ymsStatusEvent);

    }


    public static final String ALERT_ID_TAG = "_alert_id";
    public static final String ALERT_NAME_TAG = "_alert_name";
    public static final String ALERT_TYPE_TAG = "_alert_type";
    public static final String HORIZON_ALERT_TAG = "horizon_alert_id";

    public static void addSystemTags(final Map<String, String> stringStringTreeMap,
                                     final long alertId,
                                     final String alertName,
                                     final String alertType) {
        stringStringTreeMap.put(ALERT_ID_TAG, String.valueOf(alertId));
        stringStringTreeMap.put(HORIZON_ALERT_TAG, String.valueOf(alertId));
        if(Objects.nonNull(alertName)) {
            stringStringTreeMap.put(ALERT_NAME_TAG, alertName);
        }
        if(Objects.nonNull(alertType)) {
            stringStringTreeMap.put(ALERT_TYPE_TAG, alertType);
        }
    }

    private static void removeSystemTags(Map<String, String> stringStringTreeMap) {
        stringStringTreeMap.remove(ALERT_ID_TAG);
        stringStringTreeMap.remove(ALERT_NAME_TAG);
        stringStringTreeMap.remove(ALERT_TYPE_TAG);
        stringStringTreeMap.remove(HORIZON_ALERT_TAG);
    }

    public static void removeSystemTags(final AlertEvent alertEvent) {
        removeSystemTags(alertEvent.getTags());
    }

    public static void removeNsAndAppFromMap(Map<String,String> stringStringMap) {
        stringStringMap.remove(HealthCheckConfigFields.APPLICATION_FOR_DATA);
        stringStringMap.remove(HealthCheckConfigFields.NAMESPACE_FOR_DATA);
    }

    public static Map<String, IMetric> getStatusMap(AlertState alertState) {
        Map<String, IMetric> stringIMetricMap = new HashMap<>();

        switch (alertState) {
            case BAD:
                stringIMetricMap.put(STATUS_BAD, new Metric(1));
                break;
            case WARN:
                stringIMetricMap.put(STATUS_WARN, new Metric(1));
                break;
            case MISSING:
                stringIMetricMap.put(STATUS_MISSING, new Metric(1));
                break;
            case UNKNOWN:
                stringIMetricMap.put(STATUS_UNK, new Metric(1));
                break;
            case GOOD:
                stringIMetricMap.put(STATUS_OK, new Metric(1));
                break;
        }
        stringIMetricMap.putIfAbsent(STATUS_BAD,new Metric(0));
        stringIMetricMap.putIfAbsent(STATUS_OK,new Metric(0));
        stringIMetricMap.putIfAbsent(STATUS_WARN,new Metric(0));
        stringIMetricMap.putIfAbsent(STATUS_UNK,new Metric(0));
        stringIMetricMap.putIfAbsent(STATUS_MISSING,new Metric(0));

        return stringIMetricMap;
    }

    public static String getEventStatusMessage(int hits, int threshold, AlertState newState) {

        switch (newState) {

            case BAD:
                return String.format(EVENT_ALERT_MESSAGE,hits,GREATER_THAN_OR_EQUALS,threshold);
            case GOOD:
                return String.format(EVENT_ALERT_MESSAGE,hits,LESS_THAN,threshold);

        }

        return null;
    }

    public static BiConsumer<Long, Long> getPurgeErrorConsumer(
            final Logger log,
            final long alertId) {
        return (hash, lastSeen) -> log.error(
                "id: {} bad purge for {} {}",
                alertId,
                hash,
                lastSeen
        );
    }

    public static class Interval {

        private int _m_interval = TSDV3Constants.DEFAULT_INTERVAL;

        public Interval(int interval) {
            _m_interval = interval;
        }

        public int getInterval() {
            return _m_interval;
        }

        public void setInterval(int interval) {
            _m_interval = interval;
        }

    }

    public static long[] getStartEndAndIntervalFromTimeSpec(JsonNode root) throws AlertException {
        final JsonNode node = root.get(TSDV3Constants.TIME_SPECIFICATION);

        long[] arrayOfValues = new long[3];

        //in secs

        if(node != null) {
            long start = node.get(TSDV3Constants.TIME_START).asLong();
            long end = node.get(TSDV3Constants.TIME_END).asLong();
            arrayOfValues[0] = start;
            arrayOfValues[1] = end;
            arrayOfValues[2] = AlertUtils.getIntervalAsInt(node.get(TSDV3Constants.INTERVAL).asText(), start, end);
            return arrayOfValues;
        }

        return null;

    }

    public static void reportAlertStats(final AlertEventBag alertBag,
                                        final AlertConfig alertConfig) {
        final List<AlertEvent> alertEvents = alertBag.getAlertEvents();
        if (Objects.nonNull(alertEvents) && !alertEvents.isEmpty()) {
            final Map<AlertState, Long> metrics = alertEvents
                    .stream()
                    .collect(Collectors
                            .groupingBy
                                    (AlertEvent::getSignal, Collectors.counting()));
            metrics.entrySet()
                    .stream()
                    .forEach(e -> {
                        Monitoring.get()
                                .reportAlertsRaised(
                                        e.getValue(),
                                        alertConfig.getNamespace(),
                                        alertConfig.getAlertId(),
                                        e.getKey());
                    });
        }
    }

    public static long getPurgeDate(long purgeInterval) {
        final long currTimeInSecs = System.currentTimeMillis() / 1000;

        return (currTimeInSecs - purgeInterval);

    }
}
