package com.apexflow.repository;

import com.apexflow.model.CustomerStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CustomerStatsRepository extends JpaRepository<CustomerStats, String>, JpaSpecificationExecutor<CustomerStats> {
    List<CustomerStats> findByCustomerId(String customerId);
}

