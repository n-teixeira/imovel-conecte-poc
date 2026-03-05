package br.com.imovelconecte.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String LEADS_EXCHANGE = "imovelconecte.leads.exchange";
    public static final String LEADS_QUEUE = "imovelconecte.leads.queue";
    public static final String LEADS_ROUTING_KEY = "lead.webhook";
    public static final String LEADS_DLQ = "imovelconecte.leads.dlq";
    public static final String LEADS_DLX = "imovelconecte.leads.dlx";
    public static final String CONTINGENCY_QUEUE = "imovelconecte.leads.contingency";

    @Value("${imovelconecte.contingency.max-retries:5}")
    private int maxRetries;

    @Bean
    public DirectExchange leadsExchange() {
        return new DirectExchange(LEADS_EXCHANGE, true, false);
    }

    @Bean
    public Queue leadsQueue() {
        return QueueBuilder.durable(LEADS_QUEUE)
                .withArgument("x-dead-letter-exchange", LEADS_DLX)
                .withArgument("x-dead-letter-routing-key", "lead.dlq")
                .build();
    }

    @Bean
    public Binding leadsBinding(Queue leadsQueue, DirectExchange leadsExchange) {
        return BindingBuilder.bind(leadsQueue).to(leadsExchange).with(LEADS_ROUTING_KEY);
    }

    @Bean
    public DirectExchange leadsDlx() {
        return new DirectExchange(LEADS_DLX, true, false);
    }

    @Bean
    public Queue leadsDlq() {
        return QueueBuilder.durable(LEADS_DLQ).build();
    }

    @Bean
    public Binding leadsDlqBinding(Queue leadsDlq, DirectExchange leadsDlx) {
        return BindingBuilder.bind(leadsDlq).to(leadsDlx).with("lead.dlq");
    }

    @Bean
    public Queue contingencyQueue() {
        return QueueBuilder.durable(CONTINGENCY_QUEUE)
                .withArgument("x-message-ttl", 3600000)
                .build();
    }

    @Bean
    public Binding contingencyBinding(Queue contingencyQueue, DirectExchange leadsExchange) {
        return BindingBuilder.bind(contingencyQueue).to(leadsExchange).with("lead.contingency");
    }
}
