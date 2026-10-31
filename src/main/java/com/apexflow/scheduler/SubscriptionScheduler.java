package com.apexflow.scheduler;

import com.apexflow.model.Customer;
import com.apexflow.repository.CustomerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Component
public class SubscriptionScheduler {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionScheduler.class);

    private final CustomerRepository customerRepository;

    public SubscriptionScheduler(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        log.info("subscription_scheduler_startup_check running...");
        updateExpiredSubscriptions();
    }

    @Scheduled(cron = "0 0 1 * * ?")
    @Transactional
    public void updateExpiredSubscriptions() {
        LocalDate today = LocalDate.now();
        List<Customer> activeCustomers = customerRepository.findAll((root, query, cb) ->
                cb.and(
                        cb.equal(root.get("status"), "Attivo"),
                        cb.isNotNull(root.get("subscriptionEndDate")),
                        cb.lessThan(root.get("subscriptionEndDate"), today)
                )
        );

        if (!activeCustomers.isEmpty()) {
            log.info("updating_expired_subscriptions count={}", activeCustomers.size());
            for (Customer customer : activeCustomers) {
                customer.setStatus("Scaduto");
                customerRepository.save(customer);
            }
            log.info("expired_subscriptions_updated count={}", activeCustomers.size());
        } else {
            log.info("no_expired_subscriptions_found");
        }
    }
}
