package com.apexflow.config;

import com.apexflow.model.Customer;
import com.apexflow.repository.CustomerRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration // Dice a Spring che questa è una classe di configurazione
public class DataInitializer {

    @Bean // Questo metodo verrà eseguito all'avvio
    CommandLineRunner initDatabase(CustomerRepository repository) {
        return args -> {
            // Controlliamo se il database è già popolato per non duplicare i dati
            if (repository.count() == 0) {
                System.out.println("--- Popolamento database in corso... ---");

                // Creiamo il primo cliente dal tuo db.json
                Customer c1 = new Customer();
                c1.setId("A1b2C3d4E5f6G7h8");
                c1.setName("Mario Rossi");
                c1.setEmail("mario@rossi.tech");
                c1.setCompany("Tech Spa");
                c1.setStatus("Attivo");
                c1.setIndustry("Software");
                c1.setPlan("Enterprise");

                // Salviamo il cliente nel database
                repository.save(c1);

                // Aggiungiamone un secondo per prova
                Customer c2 = new Customer();
                c2.setId("I9j0K1l2M3n4O5p6");
                c2.setName("Luca Bianchi");
                c2.setEmail("l.bianchi@consulting.it");
                c2.setCompany("Consulting Srl");
                c2.setStatus("In Attesa");
                c2.setIndustry("Software");
                c2.setPlan("Professional");

                repository.save(c2);

                System.out.println("--- Database popolato con successo! ---");
            } else {
                System.out.println("--- Il database contiene già dei dati, salto il popolamento. ---");
            }
        };
    }
}