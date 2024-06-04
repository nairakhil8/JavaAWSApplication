package com.assignment.application.repository;

import com.assignment.application.model.S3Buckets;
import org.springframework.data.jpa.repository.JpaRepository;

public interface S3BucketsRepository extends JpaRepository<S3Buckets, Long> {
    S3Buckets findFirstByBucketName(String bucketName);
}