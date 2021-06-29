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

package net.opentsdb.horizon.alerts.processor.notification;

import net.opentsdb.horizon.alerts.config.NotificationConfig;
import net.opentsdb.horizon.alerts.model.AlertEvent;
import net.opentsdb.horizon.alerts.enums.AlertState;
import net.opentsdb.horizon.alerts.AlertUtils;
import net.opentsdb.horizon.alerts.query.QueryConstants;
import joptsimple.internal.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class NotificationUtils {

    private static final Logger LOG = LoggerFactory.getLogger(NotificationUtils.class);

    public static Map<String, List<AlertEvent>> groupByRules(List<String> groupingRules, List<AlertEvent> alertEvents) {

        if(alertEvents == null) {
           return null;
        }
        if(groupingRules == null || groupingRules.isEmpty()) {
            Map<String,List<AlertEvent>> grouMap = new HashMap<>();
            grouMap.put(NotificationConstants.DEFAULT_GROUP,alertEvents);
            return grouMap;
        }

        final Map<String, List<AlertEvent>> groupedAlerts = alertEvents.stream()
                .map(alertEvent -> {

                    final List<String> gValues = groupingRules.stream()
                            .map(str -> alertEvent.getTags().get(str))
                            .collect(Collectors.toList());
                    if (!gValues.isEmpty()) {
                        GroupAnnotatedEvent groupAnnotatedEvent
                                = new GroupAnnotatedEvent(new Group(gValues), alertEvent);
                        return groupAnnotatedEvent;
                    } else {
                        return null;
                    }

                })
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(ev -> Strings.join(ev.getGroup().groupValues, " "),
                        Collectors.mapping(GroupAnnotatedEvent::getAlertEvent, Collectors.toList())));

        return groupedAlerts;
    }

    public static Map<String, StringBuilder> getGroupToBody(final List<AlertEvent> alertEvents,
                                                            final NotificationConfig config,
                                                            final String compareLine,
                                                            final String comparatorString,
                                                            final double threshold,
                                                            final long lastValueInMins,
                                                            AlertState alertEventType,
                                                            final List<AlertEvent> collector) {

        if(alertEvents != null && !alertEvents.isEmpty()) {

            if(collector != null) {
                collector.addAll(alertEvents);
            }

            final Map<String, List<AlertEvent>> stringListMap = NotificationUtils.
                    groupByRules(config.getGroupingRules(), alertEvents);

            return stringListMap
                    .keySet()
                    .stream()
                    .map(group -> {
                        final StringBuilder bodyBuilder = new StringBuilder();
                        bodyBuilder.append(deriveSubstituedStringForGroupedEvents(config.getBody(),
                                stringListMap.get(group), config.getGroupingRules()));
                        addDoubleBreaks(bodyBuilder);
                        addAlertBody(stringListMap.get(group),
                                alertEventType,
                                bodyBuilder,
                                compareLine,
                                comparatorString,
                                threshold,
                                String.valueOf(lastValueInMins));
                        return new BuilderWrapper(group, bodyBuilder);

                    }).collect(Collectors.toMap(BuilderWrapper::getGroup,
                    BuilderWrapper::getBuilder));
        }
        return null;
    }

    private static class BuilderWrapper {

        private StringBuilder builder;
        private String group;

        public BuilderWrapper(String group, StringBuilder builder) {
            this.builder = builder;
            this.group = group;
        }

        public StringBuilder getBuilder() {
            return builder;
        }

        public String getGroup() {
            return group;
        }
    }

    private static class GroupAnnotatedEvent {
        private Group group;
        private AlertEvent alertEvent;

        public GroupAnnotatedEvent(Group group, AlertEvent alertEvent) {
            this.group = group;
            this.alertEvent = alertEvent;
        }

        public Group getGroup() {
            return group;
        }

        public AlertEvent getAlertEvent() {
            return alertEvent;
        }
    }

    private static class Group {
        private List<String> groupValues;

        public Group(List<String> groupValues) {
            this.groupValues = groupValues;
        }

        public List<String> getGroupValues() {
            return groupValues;
        }

        @Override
        public int hashCode() {
            return groupValues.hashCode();
        }

        @Override
        public boolean equals(Object obj) {

            if(obj instanceof Group) {
                Group other = (Group) obj;
                if(other.groupValues.equals(this.groupValues)) {
                    return true;
                }
            }

            return false;
        }
    }

    private static class KeyValueHolder {

        private String key;
        private String value;

        public KeyValueHolder(String key, String value) {
            this.key = key;

            this.value = value;
        }

        public String getKey() {
            return key;
        }

        public String getValue() {
            return value;
        }
    }

    public static String deriveSubstituedStringForGroupedEvents(final String subject,
                                                final List<AlertEvent> alertEvents,
                                                final List<String> groupingRules) {

        final String[] starts = subject.split(NotificationConstants.SUBSTITUTE_START);

        String subjectCopy = new String(subject);
        LOG.info("Starts length: {}, stats: {}, {} ",starts.length,starts,subject);
        if(starts.length > 1 &&
                (alertEvents != null && !alertEvents.isEmpty())) {
            List<String> tags = new ArrayList<>();
            for(String tag : starts) {

                if(tag.contains(NotificationConstants.COMPARE_SUBSTITUTE_END)) {
                    final String[] split = tag.split(NotificationConstants.SUBSTITUTE_END);
                    tags.add(split[0]);
                } else {
                    final String[] split = tag.split(NotificationConstants.SUBSTITUTE_END);
                    LOG.error("Malformed subject: {} , {} , {} ,{}",subject,split,tag.contains(NotificationConstants.COMPARE_SUBSTITUTE_END), NotificationConstants.SUBSTITUTE_END);
                    continue;
                }
            }

            List<String> tagsTobeCounted = new ArrayList<>();
            List<String> grbyIntersection = new ArrayList<>();

            for(String tag : tags) {
                if(groupingRules != null && groupingRules.contains(tag)) {
                    grbyIntersection.add(tag);
                } else {
                    tagsTobeCounted.add(tag);
                }
            }

            //Form group by tag
            String tempCopy = subjectCopy;
            for(String tag : grbyIntersection) {

                tempCopy = tempCopy.replaceAll(getSubStr(tag),
                        alertEvents.get(0).getTags().get(tag));

            }

            //Count the others
            if(tagsTobeCounted.size() > 0) {
                final Map<String, Map<String, String>> keyToUniqueValues = new HashMap<>();
                tagsTobeCounted.forEach(tag -> keyToUniqueValues.put(tag,new HashMap<>()));
                alertEvents.forEach(alertEvent -> {
                            final Map<String, String> tags1 = alertEvent.getTags();
                            tagsTobeCounted
                                    .stream()
                                    .forEach(tagKey -> {
                                        final Map<String, String> stringStringMap = keyToUniqueValues.get(tagKey);
                                        if(tags1.get(tagKey) != null) {
                                            stringStringMap.put(tags1.get(tagKey),tags1.get(tagKey));
                                        }

                                    });
                        });


                for(String tagKey : keyToUniqueValues.keySet()) {
                    tempCopy = tempCopy.replaceAll(getSubStr(tagKey),
                            getStrKeyForNonGrps(keyToUniqueValues.get(tagKey).size(),tagKey));
                }

            }

            subjectCopy = new String(tempCopy);

        }



        return subjectCopy;
    }

    private static String getStrKeyForNonGrps(int size,String key) {
        return String.format(NotificationConstants.NON_GRP_REPLACE,size,key);
    }

    private static String getSubStr(String grby) {
        return NotificationConstants.SUBSTITUTE_START +grby+ NotificationConstants.SUBSTITUTE_END;
    }

    public static Map<String,String> getSubjectList(final String subjectTemplate,
                                                    final List<AlertEvent> badAlertEvents,
                                                    final NotificationConfig config) {
        final Map<String, List<AlertEvent>> stringListMap = groupByRules(config.getGroupingRules(), badAlertEvents);

        final String subjectT = config.getSubject();
        final List<String> groupingRulesT = config.getGroupingRules();

        if(stringListMap != null) {
            return stringListMap.keySet()
                    .stream()
                    .map(key -> {
                        final String subjecto;
                        final List<AlertEvent> alertEvents = stringListMap.get(key);
                        if(key.equalsIgnoreCase(NotificationConstants.DEFAULT_GROUP)) {
                            subjecto = String.format(subjectTemplate,
                                    alertEvents.size(),
                                    deriveSubstituedStringForGroupedEvents(subjectT,
                                            stringListMap.get(key),
                                            groupingRulesT), NotificationConstants.DEFAULT_GROUP_SUFFIX);
                        } else {
                            subjecto = String.format(subjectTemplate,
                                    alertEvents.size(),
                                    deriveSubstituedStringForGroupedEvents(subjectT,
                                            stringListMap.get(key),
                                            groupingRulesT), key);
                        }

                        return new KeyValueHolder(key,subjecto);
                    }).collect(Collectors.toMap(KeyValueHolder::getKey,KeyValueHolder::getValue));
        }

        return null;
    }

    /**
     * Only used for metric alerts.
     * @param alertEventList
     * @param alertEventType
     * @param bodyBuilder
     * @param compareLine
     * @param comparatorString
     * @param threshold
     * @param lastValueInMins
     */
    public static void addAlertBody(List<AlertEvent> alertEventList,
                                    AlertState alertEventType,
                                    StringBuilder bodyBuilder,
                                    String compareLine,
                                    String comparatorString,
                                    double threshold,
                                    String lastValueInMins) {
        if(alertEventList != null && !alertEventList.isEmpty()) {
            Date date = new Date(alertEventList.get(0).getAlertRaisedTimestamp()*1_000l);
            bodyBuilder.append(String.format(compareLine,
                    comparatorString,
                    AlertUtils.stripTrailingZeros(AlertUtils.soothMetricValue(threshold)),
                    String.valueOf(lastValueInMins),
                    date.toString(),
                    alertEventType.name()));
            final String tagEntities = getTagEntitiesromEvents(alertEventList);

            bodyBuilder.append(tagEntities);
        }

        addDoubleBreaks(bodyBuilder);
    }

    public static String getTagEntitiesromEvents(List<AlertEvent> warnAlertEventList) {
        return warnAlertEventList.stream()
                .map(event -> {

                    StringBuilder builder = new StringBuilder();
                    builder.append("<br>");
                    event.getTags().keySet()
                            .forEach(key ->{
                                if(key.equalsIgnoreCase(QueryConstants.GROUP_BY_ALL)) {
                                    builder.append("Grouped by everything, ");
                                } else {
                                    builder.append(String.format("%s : %s, ", key, event.getTags().get(key)));
                                }
                            });
                    builder.append(event.getAlertDetails());
                    addDoubleBreaks(builder);
                    return builder.toString();
                }).collect(Collectors.joining(""));
    }

    public static void addDoubleBreaks(StringBuilder bodyBuilder) {
        bodyBuilder.append("<br>");
        bodyBuilder.append("<br>");
    }
}
