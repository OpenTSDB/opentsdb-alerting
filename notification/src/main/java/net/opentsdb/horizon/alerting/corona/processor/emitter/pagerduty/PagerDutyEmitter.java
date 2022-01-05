package net.opentsdb.horizon.alerting.corona.processor.emitter.pagerduty;

import net.opentsdb.horizon.alerting.core.validate.Validate;
import net.opentsdb.horizon.alerting.corona.model.contact.Contact;
import net.opentsdb.horizon.alerting.corona.model.contact.impl.PagerDutyContact;
import net.opentsdb.horizon.alerting.corona.model.messagekit.MessageKit;
import net.opentsdb.horizon.alerting.corona.model.messagekit.meta.PagerDutyMeta;
import net.opentsdb.horizon.alerting.corona.monitoring.AppMonitor;
import net.opentsdb.horizon.alerting.corona.processor.Processor;
import net.opentsdb.horizon.alerting.corona.processor.emitter.view.AlertView;
import net.opentsdb.horizon.alerting.corona.processor.emitter.view.MessageKitView;
import net.opentsdb.horizon.alerting.corona.processor.emitter.view.Views;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class PagerDutyEmitter implements Processor<MessageKit> {
    private static final Logger LOG =
            LoggerFactory.getLogger(PagerDutyEmitter.class);

    private PagerDutyClient client;
    private PagerDutyFormatter formatter;

    private PagerDutyEmitter(final DefaultBuilder builder)
    {
        Validate.isTrue(builder != null, "Builder cannot be null.");
        Validate.isTrue(builder.client != null, "Client has to be set.");
        Validate.isTrue(builder.formatter != null, "Formatter has to be set.");
        this.client = builder.client;
        this.formatter = builder.formatter;
    }

    @Override
    public void process(MessageKit messageKit)
    {
        if (messageKit.getType() != Contact.Type.PAGERDUTY) {
            LOG.error("Unexpected MessageKit type: {}", messageKit);
            return;
        }

        final String namespace = messageKit.getNamespace();
        final PagerDutyMeta meta = (PagerDutyMeta) messageKit.getMeta();
        final MessageKitView messageKitView = Views.of(messageKit);
        @SuppressWarnings("unchecked") final List<AlertView> alertViews = messageKitView.getAllViews();
        @SuppressWarnings("unchecked") final List<PagerDutyContact> contacts =
                (List<PagerDutyContact>) messageKit.getContacts();

        for (AlertView alertView : alertViews) {
            for (final PagerDutyContact contact : contacts) {
                doProcess(namespace, messageKitView, alertView, meta, contact);
            }
        }
    }

    private void doProcess(final String namespace,
                           final MessageKitView messageKitView,
                           final AlertView alertView,
                           final PagerDutyMeta meta,
                           final PagerDutyContact contact)
    {
        final PagerDutyEvent event;
        try {
            event = formatter.format(messageKitView, alertView, meta, contact);
        } catch (Exception e) {
            AppMonitor.get().countAlertFormatFailed(namespace);
            LOG.error("Failed to format: alert_id={}, ns={}, view={}, meta={}, contact={}",
                    messageKitView.getAlertId(), namespace, alertView, meta, contact);
            return;
        }

        try {
            client.send(event);
            AppMonitor.get().countAlertSendSuccess(namespace);
        } catch (Exception e) {
            AppMonitor.get().countAlertSendFailed(namespace);
            LOG.error("Send failed:", e);
        }
    }

    public interface Builder extends net.opentsdb.horizon.alerting.Builder<Builder, PagerDutyEmitter> {
        Builder setClient(PagerDutyClient client);
        Builder setFormatter(PagerDutyFormatter formatter);
    }

    private static final class DefaultBuilder implements Builder {
        private PagerDutyClient client;
        private PagerDutyFormatter formatter;

        @Override
        public Builder setClient(PagerDutyClient client) {
            this.client = client;
            return this;
        }

        @Override
        public Builder setFormatter(PagerDutyFormatter formatter) {
            this.formatter = formatter;
            return this;
        }

        @Override
        public PagerDutyEmitter build() {
            return new PagerDutyEmitter(this);
        }
    }

    public static Builder builder() {
        return new DefaultBuilder();
    }
}
