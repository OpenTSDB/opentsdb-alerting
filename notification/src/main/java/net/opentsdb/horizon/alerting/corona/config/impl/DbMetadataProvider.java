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

package net.opentsdb.horizon.alerting.corona.config.impl;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;
import java.util.stream.Collectors;

import net.opentsdb.horizon.alerting.corona.component.DaemonThreadFactory;
import net.opentsdb.horizon.alerting.corona.config.ConfigFetcher;
import net.opentsdb.horizon.alerting.corona.config.MetadataProvider;
import net.opentsdb.horizon.alerting.corona.model.alertconfig.Notification;
import net.opentsdb.horizon.alerting.corona.model.alertconfig.Recipient;
import net.opentsdb.horizon.alerting.corona.model.alertconfig.impl.NAlertConfig;
import net.opentsdb.horizon.alerting.corona.model.contact.Contact;
import net.opentsdb.horizon.alerting.corona.model.contact.Contact.Type;
import net.opentsdb.horizon.alerting.corona.model.contact.Contacts;
import net.opentsdb.horizon.alerting.corona.model.metadata.Metadata;
import net.opentsdb.horizon.alerting.corona.monitoring.AppMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.VisibleForTesting;

public class DbMetadataProvider implements MetadataProvider {

    /* ------------ Constants ------------ */

    private static final Logger LOG =
            LoggerFactory.getLogger(DbMetadataProvider.class);

    private static final long ZERO_DELAY = 0L;

    private static final String[] EMPTY_STRING_ARRAY = new String[0];

    /* ------------ Static Methods ------------ */

    public static DbMetadataProvider create(
            final ConfigFetcher<NAlertConfig> configFetcher)
    {
        return new DbMetadataProvider(configFetcher, 30L);
    }

    /* ------------ Fields ------------ */

    private final ConfigFetcher<NAlertConfig> configFetcher;

    private final long updateFrequencySec;

    private final AtomicReference<List<String>> namespacesRef;

    private final Map<Long, List<String>> alertToRules;

    private final Map<Long, Contacts> alertToContacts;

    private final Map<Long, Metadata> alertToMetadata;

    /**
     * Protect access to {@link #alertToRules}, {@link #alertToContacts},
     * {@link #alertToMetadata}.
     * <p>
     * Synchronizes {@link #updateAlertEntry(long, List, Contacts, Metadata)},
     * {@link #getGroupingRules(long)}, {@link #getContacts(long)},
     * {@link #getMetadata(long)}.
     */
    private final ReentrantReadWriteLock rwLock;

    private final ScheduledExecutorService executor;

    private final ExecutorService workerPool;

    /* ------------ Constructor ------------ */

    protected DbMetadataProvider(
            final ConfigFetcher<NAlertConfig> configFetcher,
            final long updateFrequencySec)
    {
        Objects.requireNonNull(configFetcher, "configFetcher cannot be null");
        if (updateFrequencySec <= 0) {
            throw new IllegalArgumentException(
                    "updateFrequencySec has to be <= 0. Given: " +
                            updateFrequencySec);
        }
        this.configFetcher = configFetcher;
        this.updateFrequencySec = updateFrequencySec;
        this.namespacesRef = new AtomicReference<>(Collections.emptyList());
        this.alertToRules = new ConcurrentHashMap<>();
        this.alertToContacts = new ConcurrentHashMap<>();
        this.alertToMetadata = new ConcurrentHashMap<>();
        this.rwLock = new ReentrantReadWriteLock();
        this.executor = Executors.newSingleThreadScheduledExecutor(DaemonThreadFactory.INSTANCE);
        this.workerPool = Executors.newWorkStealingPool();
    }

    /* ------------ Methods ------------ */

    /**
     * Update list of namespaces.
     *
     * @return true if update made, false otherwise.
     */
    private boolean updateNamespaces()
    {
        final Optional<List<String>> optional = configFetcher.getNamespaces();
        if (optional.isPresent()) {
            final List<String> namespaces =
                    Collections.unmodifiableList(optional.get());
            namespacesRef.set(namespaces);
            return true;
        }

        return false;
    }

    /**
     * Maps contacts to a map of Contact.Type -> ((id or name) -> instance).
     *
     * If the contact instance has non-zero id, then two entries will
     * be added to the second map, one with stringified id and one with name.
     * This is done to take care or the rolling contact update in the config
     * db.
     *
     * @param contacts contacts
     * @return a map Contact.Type -> ((id or name) -> instance).
     */
    private Map<Type, Map<String, Contact>> mapContactsByTypeAndName(
            final Contacts contacts)
    {
        final Map<Contact.Type, Map<String, Contact>> typeToNameToContact =
                new HashMap<>();

        contacts.forEach((type, contactList) -> {
            final Map<String, Contact> nameToContact;
            if (contactList == null || contactList.isEmpty()) {
                nameToContact = Collections.emptyMap();
            } else {
                nameToContact = new HashMap<>(contactList.size());
                for (final Contact contact: contactList) {
                    nameToContact.put(contact.getName(), contact);
                    final int contactId = contact.getId();
                    if (contactId != Contact.UNKNOWN_ID) {
                        nameToContact.put(String.valueOf(contactId), contact);
                    }
                }
            }
            typeToNameToContact.put(type, nameToContact);
        });
        return typeToNameToContact;
    }

    /**
     * Joins recipients for the alert with the contact information for the
     * namespace. The result contains all recipients, but with proper contact
     * instances.
     *
     * @param recipients   a map of Contact.ViewType -> List of Recipients
     * @param contactsBook map of Contact.ViewType -> list of contact instances
     * @return a map Contact.ViewType -> List of contact instances.
     */
    private Contacts join(
            final Map<Contact.Type, List<Recipient>> recipients,
            final Map<Contact.Type, Map<String, Contact>> contactsBook)
    {
        final Contacts.Builder builder = Contacts.builder();

        for (Map.Entry<Contact.Type, List<Recipient>> entry :
                recipients.entrySet()) {
            final Contact.Type type = entry.getKey();
            final List<Recipient> recipientList = entry.getValue();

            final Map<String, Contact> nameToContact = contactsBook.get(type);
            final Function<Recipient, Contact> mapper = recipient -> {
                final Contact defaultContact = nameToContact.get(recipient.getName());

                final int recipientId = recipient.getId();
                if (recipientId == Contact.UNKNOWN_ID) {
                    // Invalid recipient id, return default;
                    return defaultContact;
                }

                // Try lookup by id.
                return nameToContact.getOrDefault(
                        String.valueOf(recipientId),
                        defaultContact
                );
            };
            final List<Contact> contacts = recipientList.stream()
                    .map(mapper)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            builder.setContacts(type, contacts);
        }

        return builder.build();
    }

    /**
     * Update {@link #alertToRules}, {@link #alertToContacts},
     * {@link #alertToMetadata} entries for the given alert id.
     * <p>
     * Synchronized with {@link #getGroupingRules(long)},
     * {@link #getContacts(long)}, {@link #getMetadata(long)}.
     *
     * @param alertId       alert id
     * @param groupingRules grouping rules
     * @param contacts      contacts
     * @param metadata      metadata
     */
    private void updateAlertEntry(final long alertId,
                                  final List<String> groupingRules,
                                  final Contacts contacts,
                                  final Metadata metadata)
    {
        final Lock lock = rwLock.writeLock();
        lock.lock();
        try {
            alertToRules.put(alertId, groupingRules);
            alertToContacts.put(alertId, contacts);
            alertToMetadata.put(alertId, metadata);
        } finally {
            lock.unlock();
        }
    }

    private boolean updateMetadata(final String namespace)
    {
        final Optional<List<NAlertConfig>> maybeConfigs =
                configFetcher.getAlertConfigs(namespace);
        final Optional<Contacts> maybeContacts =
                configFetcher.getContacts(namespace);

        // Cannot update metadata without alert configuration and contacts.
        if (!maybeConfigs.isPresent() || !maybeContacts.isPresent()) {
            return false;
        }

        // Contact.Type -> (Contact Name -> Contact Object).
        final Map<Contact.Type, Map<String, Contact>> mappedContacts =
                mapContactsByTypeAndName(maybeContacts.get());

        final List<NAlertConfig> alertConfigs = maybeConfigs.get();
        for (final NAlertConfig config : alertConfigs) {
            final long alertId = config.getId();

            // Grouping rules.
            final List<String> groupingRules = config.getGroupingRules();

            final Notification notification = config.getNotification();

            // Contacts.
            final Contacts contacts =
                    join(
                            notification.getRecipients(),
                            mappedContacts
                    );

            // Metadata.
            final Metadata metadata = Metadata.builder()
                    .setSubject(notification.getSubject())
                    .setBody(notification.getBody())
                    .setOcSeverity(notification.getOcSeverity())
                    .setOcTier(notification.getOcTier())
                    .setOpsGeniePriority(notification.getOpsGeniePriority())
                    .setOpsGenieAutoClose(notification.isOpsGenieAutoClose())
                    .setOpsGenieTags(notification.getOpsGenieTags())
                    .setRunbookId(notification.getRunbookId())
                    .setLabels(config.getLabels().toArray(EMPTY_STRING_ARRAY))
                    .build();

            updateAlertEntry(alertId, groupingRules, contacts, metadata);
        }

        return true;
    }

    /**
     * Updates list of namespaces and all metadata per namespace.
     *
     * @throws RuntimeException if update failed.
     */
    @VisibleForTesting
    void updateAllMetadata()
    {
        final boolean nsOk = updateNamespaces();
        AppMonitor.get().countMetadataUpdateNamespaces(nsOk);
        if (!nsOk) {
            throw new RuntimeException("Failed to update namespaces.");
        }

        final List<String> namespaces = namespacesRef.get();
        try {
            workerPool.submit(() -> namespaces.parallelStream()
                    .forEach(ns -> {
                        final boolean metaOk = updateMetadata(ns);
                        AppMonitor.get().countMetadataUpdateNamespaceMeta(metaOk, ns);
                    })
            ).get(30, TimeUnit.MINUTES);
        } catch (Exception e) {
            throw new RuntimeException("Failed to update per namespace metadata in parallel.", e);
        }
    }

    /**
     * Starts periodic background updates.
     *
     * @return {@link ScheduledFuture} future.
     */
    private ScheduledFuture<?> startBackgroundMetadataUpdates()
    {
        return executor.scheduleAtFixedRate(
                () -> {
                    try {
                        updateAllMetadata();
                    } catch (Exception e) {
                        LOG.error("Full metadata update failed", e);
                    }
                },
                ZERO_DELAY,
                updateFrequencySec,
                TimeUnit.SECONDS
        );
    }

    /**
     * Does initial metadata pull, then starts periodic background updates.
     *
     * @return {@link ScheduledFuture} future.
     * @throws RuntimeException if initial metadata update failed.
     */
    public ScheduledFuture<?> start()
    {
        updateAllMetadata();
        return startBackgroundMetadataUpdates();
    }

    /**
     * Shuts down the underlying executor.
     */
    public void stop()
    {
        executor.shutdownNow();
    }

    /**
     * Returns grouping rules for the given alert id.
     * <p>
     * Synchronized with {@link #updateAlertEntry(long, List, Contacts, Metadata)}.
     *
     * @param alertId alert id
     * @return optional with a list of grouping rules.
     */
    @Override
    public Optional<List<String>> getGroupingRules(final long alertId)
    {
        final Lock lock = rwLock.readLock();
        lock.lock();
        try {
            return Optional.ofNullable(alertToRules.get(alertId));
        } finally {
            lock.unlock();
        }
    }

    /**
     * Returns contacts for the given alert id.
     * <p>
     * Synchronized with {@link #updateAlertEntry(long, List, Contacts, Metadata)}.
     *
     * @param alertId alert id
     * @return optional with contacts.
     */
    @Override
    public Optional<Contacts> getContacts(final long alertId)
    {
        final Lock lock = rwLock.readLock();
        lock.lock();
        try {
            return Optional.ofNullable(alertToContacts.get(alertId));
        } finally {
            lock.unlock();
        }
    }

    /**
     * Returns metadata for the given alert id.
     * <p>
     * Synchronized with {@link #updateAlertEntry(long, List, Contacts, Metadata)}.
     *
     * @param alertId alert id
     * @return optional with metadata instance.
     */
    @Override
    public Optional<Metadata> getMetadata(final long alertId)
    {
        final Lock lock = rwLock.readLock();
        lock.lock();
        try {
            return Optional.ofNullable(alertToMetadata.get(alertId));
        } finally {
            lock.unlock();
        }
    }
}
