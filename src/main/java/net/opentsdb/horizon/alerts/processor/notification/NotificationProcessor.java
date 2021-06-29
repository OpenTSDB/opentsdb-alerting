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

import net.opentsdb.horizon.alerts.AlertUtils;
import net.opentsdb.horizon.alerts.EnvironmentConfig;
import net.opentsdb.horizon.alerts.config.NotificationConfig;
import net.opentsdb.horizon.alerts.config.impl.MetricAlertConfig;
import net.opentsdb.horizon.alerts.enums.AlertState;
import net.opentsdb.horizon.alerts.model.AlertEvent;
import net.opentsdb.horizon.alerts.model.AlertEventBag;
import net.opentsdb.horizon.alerts.model.EventAlertEvent;
import net.opentsdb.horizon.alerts.model.HealthCheckAlertEvent;
import net.opentsdb.horizon.alerts.model.PeriodOverPeriodAlertEvent;
import net.opentsdb.horizon.alerts.model.Recipient;
import net.opentsdb.horizon.alerts.processor.ChainableProcessor;
import net.opentsdb.horizon.alerts.processor.impl.EnrichmentProcessor;
import org.apache.commons.mail.EmailException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class NotificationProcessor extends EnrichmentProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(NotificationProcessor.class);

    private ChainableProcessor<AlertEventBag> nextProcessor;

    private volatile NotificationConfig config;

    private EnvironmentConfig environmentConfig = new EnvironmentConfig();

    private Map<String,EmailClient> clients = new HashMap<>();

    private final String ORIGIN_EMAIL_ADDRESS;

    /**
     * TODO: Should not be here
     */
    private MetricAlertConfig metricAlertConfig = null;

    private String comparatorString = null;
    private String flipComparatorString = null;
    private int lastValueInMins = 0;

    public NotificationProcessor(NotificationConfig config) {
        this.config = config;

        this.ORIGIN_EMAIL_ADDRESS ="%s-" + environmentConfig.getMailAddressSuffix();
    }

    public void setConfig(NotificationConfig config) {
        this.config = config;
    }

    @Override
    public boolean process(AlertEventBag e) {

        if (e == null) {
            return true;
        } else if (e.getAlertEvents().size() > 0) {
            final AlertEvent sampleEvent = e.getAlertEvents().get(0);
            if (sampleEvent instanceof HealthCheckAlertEvent
                    || sampleEvent instanceof EventAlertEvent
                    || sampleEvent instanceof PeriodOverPeriodAlertEvent) {
                LOG.info("id: {} Received {} in notification: {} ",
                        e.getId(),
                        sampleEvent.getClass().getSimpleName(),
                        e.getAlertEvents().size());

                if (nextProcessor != null) {
                    return nextProcessor.process(e);
                } else {
                    return true;
                }
            }
        } else {
            LOG.info("id: {} Received alerts in notification: {} ",e.getId(), e.getAlertEvents());
            return true;
        }

        try {

            LOG.info("id: {} Received alerts in notification: {} ",e.getId(), e.getAlertEvents());

            //Notify
            /**
             * TODO: Remove Remove Remove
             */
            metricAlertConfig = (MetricAlertConfig) getAlertConfig();
            config = metricAlertConfig.getNotificationConfig();
            comparatorString = AlertUtils.getWordFromComparator(metricAlertConfig.getComparisonOperator());
            flipComparatorString = AlertUtils.getWordFromComparator(metricAlertConfig.getFlippedComparisionOperator());
            lastValueInMins = (int)(metricAlertConfig.getSlidingWindowInSecs()/ AlertUtils.dataFrequencyInSecs);


            if(e.getAlertEvents().isEmpty()) {
                return true;
            }

            //For now send the email grouped at alert level.

            Map<AlertState, List<AlertEvent>> eventsByType = getEventsByType(e);

            final List<AlertEvent> badAlertEvents = eventsByType.get(AlertState.BAD);
            final List<AlertEvent> warnAlertEvents = eventsByType.get(AlertState.WARN);

            if(badAlertEvents != null || warnAlertEvents != null) {
                int count = 0;
                if(eventsByType.containsKey(AlertState.BAD)) {
                    count += badAlertEvents.size();
                }
                if(eventsByType.containsKey(AlertState.WARN)) {
                    count += warnAlertEvents.size();
                }
                if(count != 0) {

                    List<AlertEvent> badPlusWarnList = new ArrayList<>();

                    Map<String, StringBuilder> badGroupToBody  = NotificationUtils.
                            getGroupToBody(badAlertEvents,
                                    config,
                                    NotificationConstants.compareLine,
                                    comparatorString,
                                    metricAlertConfig.getBadThreshold(),
                                    lastValueInMins,
                                    AlertState.BAD,
                                    badPlusWarnList);

                    Map<String,StringBuilder> warnGroupToBody = NotificationUtils.
                            getGroupToBody(warnAlertEvents,
                                    config,
                                    NotificationConstants.compareLine,
                                    comparatorString,
                                    metricAlertConfig.getWarnThreshold(),
                                    lastValueInMins,
                                    AlertState.WARN,
                                    badPlusWarnList);

                    final Map<String, String> subjectList = NotificationUtils.getSubjectList(NotificationConstants.otherSubject,
                            badPlusWarnList,config);

                    subjectList.keySet()
                            .forEach(
                                    group -> {

                                        final String subject = subjectList.get(group);
                                        final StringBuilder bodyBuilder = new StringBuilder();
                                        if(badGroupToBody != null) {
                                            if(badGroupToBody.get(group) != null) {
                                                bodyBuilder.append(badGroupToBody.get(group).toString());
                                            }
                                        }
                                        if(warnGroupToBody != null) {
                                            if(warnGroupToBody.get(group) != null) {
                                                bodyBuilder.append(warnGroupToBody.get(group).toString());
                                            }
                                        }
                                        LOG.info("Sending mail for Bad {} and Warn {} : {},{}",badAlertEvents,warnAlertEvents,
                                                subject,bodyBuilder.toString());
                                        //sendMails(subject,bodyBuilder.toString());

                                    }
                            );
                }
            }

            //Send recovery mails
            final List<AlertEvent> recoveryAlertEvents = eventsByType.get(AlertState.GOOD);

            Map<String,StringBuilder> recoveryGroupToBody = NotificationUtils.
                    getGroupToBody(recoveryAlertEvents,
                            config,
                            NotificationConstants.compareLine,
                            flipComparatorString,
                            metricAlertConfig.getRecoveryThreshold(),
                            lastValueInMins,
                            AlertState.GOOD,
                            null);
            LOG.info("id: {} RecoveryGroupToBody: {}",e.getId(),recoveryGroupToBody);
            if(recoveryGroupToBody != null && !recoveryGroupToBody.isEmpty()) {

                final Map<String, String> subjectList = NotificationUtils.getSubjectList(NotificationConstants.recoverySubject,
                        recoveryAlertEvents, config);
                LOG.info("id: {} RecoverySubjectList: {}",e.getId(),subjectList);
                if (subjectList != null) {
                    subjectList.keySet()
                            .forEach(
                                    group -> {

                                        final String subject = subjectList.get(group);
                                        final StringBuilder bodyBuilder = recoveryGroupToBody.get(group);
                                        LOG.info("Sending mail for recovery {}: {},{}",recoveryAlertEvents,
                                                subject,bodyBuilder.toString());
                                        //sendMails(subject, bodyBuilder.toString());

                                    }
                            );
                }
            }


        } catch (Exception e1) {
            LOG.error("Errors processing notifications: {}", e,e1);
        }

        if(nextProcessor != null) {
            return nextProcessor.process(e);
        } else {
            return true;
        }

    }


    private Map<AlertState, List<AlertEvent>> getEventsByType(AlertEventBag e) {

        return e.getAlertEvents()
                .stream()
                .collect(Collectors.groupingBy(AlertEvent::getSignal));

    }

    private void sendMails(String subject, String body) {

        LOG.info("Email To fire: subject {} body {} {} {} {}",
                subject,body,ORIGIN_EMAIL_ADDRESS,environmentConfig.getEmailRelayServer(), NotificationConstants.EMAIL_ALIAS);


        final String namespace = config.getNamespace();

        if(clients.get(namespace) == null) {

            clients.put(namespace, new EmailClient(environmentConfig.getEmailRelayServer(),
                     String.format(ORIGIN_EMAIL_ADDRESS,namespace,namespace),
                     String.format(NotificationConstants.EMAIL_ALIAS,namespace)));
        }


        EmailClient client = clients.get(namespace);


        try {
            if(environmentConfig.fireEmails()) {
                final List<Recipient> recipients = config.getRecipients();

                for (Recipient recipient : recipients) {
                    if (recipient.getType().equalsIgnoreCase("email") &&
                            !recipient.getName().equalsIgnoreCase("bob@opentsdb.net.com")) {
                        try {
                            client.send(subject, body, recipient.getName());
                        } catch (EmailException e) {
                            LOG.error("Error sending emails for namespace: " +
                                    "{} and mail: {} ", namespace, recipient.getName(), e);
                        }
                    }
                }
            }
            client.send(suffixMailIdentifier(subject), body,
                    "bob@opentsdb.net.com",
                    "bob@opentsdb.net.com");

            return;
        } catch (Exception e) {
            LOG.error("Error sending emails for namespace: {}",namespace,e);
            return;
        }
    }

    private String suffixMailIdentifier(String subject) {
        return subject + String.format(" [Horizon to me %s]",environmentConfig.getEnv());
    }

    @Override
    public void setNextProcessor(ChainableProcessor<AlertEventBag> nextProcessor) {
        this.nextProcessor = nextProcessor;
    }
}
