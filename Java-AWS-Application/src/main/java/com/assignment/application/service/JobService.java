package com.assignment.application.service;

import com.assignment.application.model.Job;
import com.assignment.application.repository.JobRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class JobService {

    private final JobRepository jobRepository;

    public JobService(JobRepository jobRepository) {
        this.jobRepository = jobRepository;
    }

    @Transactional
    public Long createJob() {
        Job job = new Job();
        job.setStatus("In Progress");
        job = jobRepository.save(job);
        return job.getId();
    }

    @Transactional
    public void updateJobStatus(Long jobId, String status) {
        Job job = jobRepository.findById(jobId).orElseThrow(() -> new IllegalArgumentException("Invalid job ID"));
        if("Invalid".equalsIgnoreCase(status)){
            job.setJobDetails("Error");
            job.setJobResult("Invalid Data");
            job.setStatus("Failed");
            jobRepository.save(job);
        }
        else {
            job.setStatus(status);
            jobRepository.save(job);
        }
    }

    public String getJobStatus(Long jobId) {
        return jobRepository.findById(jobId)
                .map(Job::getStatus)
                .orElse("Job not found");
    }

    public Job getJobById(Long jobId) {
        return jobRepository.findById(jobId).orElseThrow(() -> new IllegalArgumentException("Invalid job ID"));
    }

    public Job getJobByJobDetails(String jobDetails) {
        return jobRepository.findByJobDetails(jobDetails);
    }
}


