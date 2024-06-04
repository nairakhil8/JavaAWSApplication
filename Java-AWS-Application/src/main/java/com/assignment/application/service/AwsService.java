package com.assignment.application.service;

import com.amazonaws.SdkClientException;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.assignment.application.model.Job;
import com.assignment.application.model.S3Buckets;
import com.assignment.application.repository.S3BucketsRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import com.assignment.application.repository.JobRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
public class AwsService {

    private static final Logger log = LogManager.getLogger(AwsService.class);
    private final AmazonEC2 amazonEC2;
    private final AmazonS3 amazonS3;
    private final JobService jobService;
    private final JobRepository jobRepository;
    private final S3BucketsRepository s3BucketsRepository;

    public AwsService(AmazonEC2 amazonEC2, AmazonS3 amazonS3, JobService jobService, JobRepository jobRepository, S3BucketsRepository s3BucketsRepository) {
        this.amazonEC2 = amazonEC2;
        this.amazonS3 = amazonS3;
        this.jobService = jobService;
        this.jobRepository = jobRepository;
        this.s3BucketsRepository = s3BucketsRepository;
    }

    @Async
    public CompletableFuture<Void> discoverEc2Instances(Long jobId) {
        try {
            List<String> instanceIds = amazonEC2.describeInstances(new DescribeInstancesRequest())
                    .getReservations().stream()
                    .flatMap(reservation -> reservation.getInstances().stream())
                    .map(Instance::getInstanceId)
                    .toList();

            Job job = jobService.getJobById(jobId);
            if(job.getJobDetails() != null){
                job.setJobDetails(job.getJobDetails() + "& Discovered EC2");
                job.setJobResult(job.getJobResult() + " " + instanceIds.toString());
            } else {
                job.setJobDetails("Discovered EC2 ");
                job.setJobResult(instanceIds.toString());
            }
            jobRepository.save(job);
            jobService.updateJobStatus(jobId, "Success");
        } catch (Exception e) {
            jobService.updateJobStatus(jobId, "Failed");
        }
        return CompletableFuture.completedFuture(null);
    }

    @Async
    public CompletableFuture<Void> discoverS3Buckets(Long jobId) {
        try {
            List<String> bucketNames = amazonS3.listBuckets().stream()
                    .map(Bucket::getName)
                    .toList();

            Job job = jobService.getJobById(jobId);
            if(job.getJobDetails() != null){
                job.setJobDetails(job.getJobDetails() + "& Discovered S3");
                job.setJobResult(job.getJobResult() + " " + bucketNames.toString());
            } else {
                job.setJobDetails("Discovered S3 ");
                job.setJobResult(bucketNames.toString());
            }
            job.setJobResult(bucketNames.toString());
            jobRepository.save(job);
            jobService.updateJobStatus(jobId, "Success");
            saveS3Record(bucketNames);
        } catch (Exception e) {
            jobService.updateJobStatus(jobId, "Failed");
        }
        return CompletableFuture.completedFuture(null);
    }

    public void saveS3Record(List<String> bucketNames) {
        try{
            for(String name : bucketNames){
                S3Buckets s3BucketExists = s3BucketsRepository.findFirstByBucketName(name);
                try{
                    if(s3BucketExists == null){
                        S3Buckets s3Bucket = new S3Buckets();
                        s3Bucket.setBucketName(name);
                        ObjectListing objectListing = amazonS3.listObjects(name);
                        List<String> objectKeys = new ArrayList<>();
                        for (S3ObjectSummary objectSummary : objectListing.getObjectSummaries()) {
                            objectKeys.add(objectSummary.getKey());
                        }
                        s3Bucket.setObjectList(objectKeys.toString());
                        s3Bucket.setObjectCount((long) objectKeys.size());
                        s3BucketsRepository.save(s3Bucket);
                    }
                }
                catch(SdkClientException e){
                    log.error("Invalid Region bucket");
                }
            }
        } catch (Exception e){
            log.error("Failed to Add S3 Buckets");
        }
    }

    public List<String> getDiscoveredEc2InstanceIds() {
        DescribeInstancesRequest request = new DescribeInstancesRequest();
        DescribeInstancesResult response = amazonEC2.describeInstances(request);
        List<String> instanceIds = new ArrayList<>();
        for (Reservation reservation : response.getReservations()) {
            for (Instance instance : reservation.getInstances()) {
                instanceIds.add(instance.getInstanceId());
            }
        }
        return instanceIds;
    }

    public List<String> getDiscoveredS3BucketNames() {
        List<String> bucketNames = new ArrayList<>();
        for (Bucket bucket : amazonS3.listBuckets()) {
            bucketNames.add(bucket.getName());
        }
        return bucketNames;
    }

    @Async
    public CompletableFuture<Void> discoverS3BucketObjects(String bucketName, Long jobId) {
        try{
            Job job = jobService.getJobById(jobId);
            List<String> objectKeys = new ArrayList<>();
            try {
                ObjectListing objectListing = amazonS3.listObjects(bucketName);
                for (S3ObjectSummary objectSummary : objectListing.getObjectSummaries()) {
                    objectKeys.add(objectSummary.getKey());
                }
            } catch (SdkClientException e) {
                job.setJobDetails("Invalid Bucket for Mumbai Region");
                job.setJobResult("No Data Found");
                jobRepository.save(job);
                jobService.updateJobStatus(jobId, "Failed");
                return CompletableFuture.failedFuture(e);
            }
            job.setJobDetails(bucketName + " Objects");
            job.setJobResult(String.join(", ", objectKeys));
            jobRepository.save(job);
            jobService.updateJobStatus(jobId, "Success");
        } catch (Exception e) {
            log.error("Request Failed", e);
        }
        return CompletableFuture.completedFuture(null);
    }

    public Long getS3BucketObjectsCount(String bucketName) {
        try{
            S3Buckets s3Bucket = s3BucketsRepository.findFirstByBucketName(bucketName);
            if(s3Bucket != null)
                return s3Bucket.getObjectCount();
        } catch (Exception e){
            log.error("Failed to get the count", e);
        }
       return 0L;
    }

    public List<String> getS3BucketObjectLike(String bucketName, String pattern) {
        List<String> result = new ArrayList<>();
        try{
            S3Buckets s3Bucket = s3BucketsRepository.findFirstByBucketName(bucketName);
            String fileNames = s3Bucket.getObjectList();
            fileNames = fileNames.substring(1, fileNames.length() - 1);
            String[] itemsArray = fileNames.split(",");
            for (String item : itemsArray) {
                if(item.contains(pattern))
                    result.add(item.trim());
            }
            if(result.isEmpty())
                result.add("NO DATA FOUND");
        } catch (Exception e){
            log.error("Failed to get the file with pattern", e);
        }
        return result;
    }
}

