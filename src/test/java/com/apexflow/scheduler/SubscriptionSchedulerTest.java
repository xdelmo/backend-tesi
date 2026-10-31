package com.apexflow.scheduler;

import com.apexflow.model.Customer;
import com.apexflow.repository.CustomerRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class SubscriptionSchedulerTest {

    @Autowired
    private SubscriptionScheduler scheduler;

    @Autowired
    private CustomerRepository customerRepository;

    @AfterEach
    void tearDown() {
        if (customerRepository.existsById("EXPIRED-CUST-1")) {
            customerRepository.deleteById("EXPIRED-CUST-1");
        }
    }

    @Test
    void testUpdateExpiredSubscriptions() {
        Customer c = new Customer();
        c.setId("EXPIRED-CUST-1");
        c.setName("Past Due Customer");
        c.setStatus("Attivo");
        c.setSubscriptionEndDate(LocalDate.now().minusDays(2));
        customerRepository.save(c);

        scheduler.updateExpiredSubscriptions();

        Customer updated = customerRepository.findById("EXPIRED-CUST-1").orElseThrow();
        assertThat(updated.getStatus()).isEqualTo("Scaduto");
    }
}
