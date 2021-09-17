/*
 *  This file is part of OpenTSDB.
 *  Copyright (C) 2021 Yahoo.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package net.opentsdb.horizon.alerting.corona.processor.emitter.opsgenie;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

import net.opentsdb.horizon.alerting.corona.model.alert.Alert;
import net.opentsdb.horizon.alerting.corona.model.alertgroup.AlertGroup;
import net.opentsdb.horizon.alerting.corona.model.alertgroup.GroupKey;
import net.opentsdb.horizon.alerting.corona.model.contact.Contact;
import net.opentsdb.horizon.alerting.corona.model.contact.impl.OpsGenieContact;
import net.opentsdb.horizon.alerting.corona.model.messagekit.MessageKit;
import net.opentsdb.horizon.alerting.corona.model.messagekit.meta.OpsGenieMeta;
import net.opentsdb.horizon.alerting.corona.monitoring.AppMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.opentsdb.horizon.alerting.corona.processor.Processor;
import net.opentsdb.horizon.alerting.corona.processor.emitter.Formatter;

/**
 * Emits OpsGenie notifications.
 */
public class OpsGenieEmitter implements Processor<MessageKit> {

    /* ------------ Constants ------------ */

    private static final Logger LOG =
            LoggerFactory.getLogger(OpsGenieEmitter.class);

    /* ------------ Fields ------------ */

    private final OpsGenieClient client;

    private final Formatter<MessageKit, OpsGenieAlert> formatter;

    private final int maxSendAttempts;

    /** Encryptor for OpsGenie API keys, for debugging. */
    private final Function<String, String> apiKeyEncryptor;

    /* ------------ Constructor ------------ */

    public OpsGenieEmitter(final Builder builder)
    {
        Objects.requireNonNull(builder, "builder cannot be null");
        Objects.requireNonNull(builder.client, "client cannot be null");
        Objects.requireNonNull(builder.formatter, "client cannot be null");
        if (builder.maxSendAttempts <= 0) {
            throw new IllegalArgumentException(
                    "maxSendAttempts has to be > 0. Given: " +
                            builder.maxSendAttempts);
        }
        this.client = builder.client;
        this.formatter = builder.formatter;
        this.maxSendAttempts = builder.maxSendAttempts;
        this.apiKeyEncryptor = builder.apiKeyEncryptor;
    }

    /* ------------ Methods ------------ */

    private boolean doWithRetry(
            final BiFunction<String, OpsGenieAlert, Boolean> fn,
            final String apiKey,
            final OpsGenieAlert alert)

    {
        for (int i = 0; i < maxSendAttempts; i++) {
            if (fn.apply(apiKey, alert)) {
                return true;
            }
        }
        return false;
    }

    private enum OpsGenieAction {
        CREATE,
        CLOSE,
        ADD_NOTE,
    }

    private void sendOne(final OpsGenieAlert alert,
                         final OpsGenieContact contact,
                         final String namespace,
                         final long alertId)
    {
        final String apiKey = contact.getApiKey();
        final Optional<Boolean> active = client.isActive(apiKey, alert);
        LOG.debug("Current alert status: alert_id={}, alias={}, active={}, contact={}",
                alertId, alert.getAlias(), active, contact);

        final OpsGenieAction action;
        if (active.isPresent() && active.get()) {
            if (alert.isClosable()) {
                action = OpsGenieAction.CLOSE;
            } else {
                // No API error and still active, just update the open alert.
                //
                // Note: Create API is actually called and we rely on the
                // OpsGenie de-duplication mechanism:
                // https://docs.opsgenie.com/docs/alert-deduplication
                //
                // We want to append the recovery alerts, as they are
                // continuation of already open alert.
                action = OpsGenieAction.ADD_NOTE;
            }
        } else {
            if (alert.isClosable()) {
                // The initial alert is closed. Do not send a recovery.
                LOG.info("Alert is closed, not sending recovery message: alert_id={}, alias={}, contact={}",
                        alertId, alert.getAlias(), contact);
                return;
            }
            // In case of API error or closed alert.
            //
            // Recovery note should not be appended because there is
            // no related context.
            action = OpsGenieAction.CREATE;
        }

        final boolean ok;
        switch (action) {
            case CLOSE:
                ok = doWithRetry(client::close, apiKey, alert);
                break;
            case ADD_NOTE:
                ok = doWithRetry(client::addNote, apiKey, alert.addRecoveryNote());
                break;
            case CREATE:
                ok = doWithRetry(client::create, apiKey, alert.removeRecoveryNote());
                break;
            default:
                throw new RuntimeException("Unknown OpsGenieAction: " + action);
        }

        {
            // Log information about the alert.
            String encryptedApiKey;
            if (apiKeyEncryptor == null) {
                encryptedApiKey = "<encryptor is not initialized>";
            } else {
                try {
                    encryptedApiKey = apiKeyEncryptor.apply(apiKey);
                } catch (Exception e) {
                    LOG.error("Error encrypting API key.", e);
                    encryptedApiKey = "<encryptor error>";
                }
            }
            LOG.info("Send alert result: alert_id={}, action={}, ok={}, namespace={}, contact={}, " +
                            "encrypted_apikey={}, alias={}, message={}, description=<<{}>>.",
                    alertId, action, ok, alert.getNamespace(), contact, encryptedApiKey,
                    alert.getAlias(), alert.getMessage(), alert.getDescription()
            );
        }

        if (!ok) {
            AppMonitor.get().countAlertSendFailed(namespace);
        } else {
            AppMonitor.get().countAlertSendSuccess(namespace);
        }
    }

    private void send(final OpsGenieAlert alert,
                      final List<OpsGenieContact> contacts,
                      final String namespace,
                      final long alertId)
    {
        for (OpsGenieContact contact : contacts) {
            try {
                sendOne(alert, contact, namespace, alertId);
            } catch (Exception e) {
                LOG.error("Sending alert failed: alias={},  alert={}, contact={}",
                        alert.getAlias(), alert, contact, e);
            }
        }
    }

    @Override
    public void process(final MessageKit messageKit)
    {
        if (messageKit.getType() != Contact.Type.OPSGENIE) {
            LOG.error("Unexpected MessageKit type: alert_id={}, message_kit={}",
                    messageKit.getAlertId(), messageKit);
            return;
        }

        final String namespace = messageKit.getNamespace();

        // Collect statistics on how many alerts are sent to OpsGenie.
        {
            AppMonitor.get().countEmitterAlertGroupSize(
                    messageKit.getAlertGroup().getAlerts().size(),
                    "opsgenie",
                    namespace,
                    messageKit.getAlertId()
            );
        }

        final boolean autoCloseEnabled = ((OpsGenieMeta) messageKit.getMeta()).isOpsGenieAutoClose();
        final List<OpsGenieAlert> alerts = formatAlerts(namespace, messageKit, autoCloseEnabled);
        LOG.info("Formatted alerts: alert_id={}, autoclose_enabled={}, alerts_to_send_count={}",
                messageKit.getAlertId(), autoCloseEnabled, alerts.size());

        @SuppressWarnings("unchecked")
        final List<OpsGenieContact> contacts =
                (List<OpsGenieContact>) messageKit.getContacts();

        for (OpsGenieAlert alert: alerts) {
            send(alert, contacts, namespace, messageKit.getAlertId());
        }
    }

    // Format message kit, possibly splitting one message kit into many.
    //
    // If OpsGenieAutoClose is enabled, then a single message kit is split
    // into many message kits with one alert each.
    private List<OpsGenieAlert> formatAlerts(final String namespace,
                                             final MessageKit messageKit,
                                             final boolean autoCloseEnabled)
    {
        final GroupKey originalGroupKey = messageKit.getAlertGroup().getGroupKey();

        if (autoCloseEnabled) {
            final List<Alert> allAlerts = messageKit.getAlertGroup().getAlerts();
            final List<OpsGenieAlert> opsGenieAlerts = new ArrayList<>(allAlerts.size());

            for (Alert singleAlert: allAlerts) {
                final Map<String, String> tags = singleAlert.getTags();
                final String[] keys = new String[tags.size()];
                final String[] vals = new String[tags.size()];

                int i = 0;
                for (Map.Entry<String, String> entry: tags.entrySet()) {
                    keys[i] = entry.getKey();
                    vals[i] = entry.getValue();
                    i++;
                }

                final GroupKey singleGroupKey = GroupKey.builder()
                        .setAlertId(originalGroupKey.getAlertId())
                        .setAlertType(originalGroupKey.getAlertType())
                        .setNamespace(originalGroupKey.getNamespace())
                        .setKeys(keys)
                        .setValues(vals)
                        .build();

                final AlertGroup singleAlertGroup = AlertGroup.builder()
                        .setGroupKey(singleGroupKey)
                        .setAlerts(Collections.singletonList(singleAlert))
                        .build();

                final MessageKit singleAlertMessageKit = MessageKit.builder()
                        .setAlertGroup(singleAlertGroup)
                        .setContacts(messageKit.getContacts())
                        .setMeta(messageKit.getMeta())
                        .setType(messageKit.getType())
                        .build();

                final Optional<OpsGenieAlert> maybeAlert =
                        formatSingleAlert(namespace, singleAlertMessageKit);
                maybeAlert.ifPresent(opsGenieAlerts::add);
            }
            return opsGenieAlerts;
        } else {
            final Optional<OpsGenieAlert> maybeAlert = formatSingleAlert(namespace, messageKit);
            return maybeAlert.map(Collections::singletonList).orElse(Collections.emptyList());
        }
    }

    // Format given message kit without any splits.
    private Optional<OpsGenieAlert> formatSingleAlert(final String namespace,
                                                      final MessageKit messageKit)
    {
        try {
            final OpsGenieAlert alert = formatter.format(messageKit);
            return Optional.of(alert);
        } catch (Exception e) {
            AppMonitor.get().countAlertFormatFailed(namespace);
            LOG.error("Formatting failed: alert_id={}, message_kit={}",
                    messageKit.getAlertId(), messageKit, e);
        }
        return Optional.empty();
    }

    /* ------------ Builder ------------ */

    public static class Builder {

        private OpsGenieClient client;

        private Formatter<MessageKit, OpsGenieAlert> formatter;

        private int maxSendAttempts;

        private Function<String, String> apiKeyEncryptor;

        private Builder() { }

        public Builder setClient(final OpsGenieClient client)
        {
            this.client = client;
            return this;
        }

        public Builder setFormatter(
                final Formatter<MessageKit, OpsGenieAlert> formatter)
        {
            this.formatter = formatter;
            return this;
        }

        public Builder setMaxSendAttempts(final int maxSendAttempts)
        {
            this.maxSendAttempts = maxSendAttempts;
            return this;
        }

        public Builder setApiKeyEncryptor(final Function<String, String> apiKeyEncryptor)
        {
            this.apiKeyEncryptor = apiKeyEncryptor;
            return this;
        }

        public OpsGenieEmitter build()
        {
            return new OpsGenieEmitter(this);
        }
    }

    public static Builder builder()
    {
        return new Builder();
    }
}
