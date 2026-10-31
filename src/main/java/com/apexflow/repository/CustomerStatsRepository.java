package com.apexflow.repository;

import com.apexflow.model.CustomerStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface CustomerStatsRepository extends JpaRepository<CustomerStats, String>, JpaSpecificationExecutor<CustomerStats> {
}

