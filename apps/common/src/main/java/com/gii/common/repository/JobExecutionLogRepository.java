package com.gii.common.repository;

import com.gii.common.model.JobExecutionLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JobExecutionLogRepository extends JpaRepository<JobExecutionLog, Long> {
    Optional<JobExecutionLog> findByJobId(String jobId);
}