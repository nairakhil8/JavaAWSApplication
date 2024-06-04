package com.assignment.application.repository;

import com.assignment.application.model.Job;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JobRepository extends JpaRepository<Job, Long> {
    Job findByJobDetails(String jobDetails);
}

