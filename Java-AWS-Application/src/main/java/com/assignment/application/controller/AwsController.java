package com.assignment.application.controller;

import com.assignment.application.service.AwsService;
import com.assignment.application.service.JobService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/aws")
public class AwsController {

    private static final Logger log = LogManager.getLogger(AwsController.class);
    private final AwsService awsService;
    private final JobService jobService;

    public AwsController(AwsService awsService, JobService jobService) {
        this.awsService = awsService;
        this.jobService = jobService;
    }

    @PostMapping("/discover-services")
    public ResponseEntity<Map<String,Long>> discoverServices(@RequestBody List<String> services) {
        Long jobId = jobService.createJob();
        Map<String,Long> result = new HashMap<>();
        result.put("JobID", jobId);
        try{
            if (services.contains("EC2")) {
                awsService.discoverEc2Instances(jobId);
            }
            if (services.contains("S3")) {
                awsService.discoverS3Buckets(jobId);
            }
            else if(!services.contains("EC2") && !services.contains("S3")){
                throw new IllegalArgumentException("Invalid Service Name");
            }
        } catch (Exception e){
            log.error("Request Failed", e);
            jobService.updateJobStatus(jobId, "Invalid");
            return new ResponseEntity<>(result, HttpStatus.FORBIDDEN);
        }
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    @GetMapping("/job-result/{jobId}")
    public ResponseEntity<Map<String,String>> getJobResult(@PathVariable Long jobId) {
        Map<String,String> result = new HashMap<>();
        result.put("Status", jobService.getJobStatus(jobId));
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    @GetMapping("/discovery-result/{service}")
    public ResponseEntity<List<String>> getDiscoveryResult(@PathVariable String service) {
        try{
            if ("EC2".equalsIgnoreCase(service)) {
                return new ResponseEntity<>(awsService.getDiscoveredEc2InstanceIds(), HttpStatus.OK);
            } else if ("S3".equalsIgnoreCase(service)) {
                return new ResponseEntity<>(awsService.getDiscoveredS3BucketNames(), HttpStatus.OK);
            } else {
                throw new IllegalArgumentException("Invalid Service Name");
            }
        } catch(Exception e){
            log.error("Request Failed", e);
        }
        return new ResponseEntity<>(new ArrayList<>(), HttpStatus.FORBIDDEN);
    }

    @PostMapping("/discover-s3-bucket-objects/{bucketName}")
    public ResponseEntity<Map<String,Long>> discoverS3BucketObjects(@PathVariable String bucketName) {
        Long jobId = jobService.createJob();
        Map<String,Long> result = new HashMap<>();
        result.put("JobID", jobId);
        try{
            awsService.discoverS3BucketObjects(bucketName, jobId);
        } catch (Exception e){
            log.error("Request Failed", e);
            return new ResponseEntity<>(result, HttpStatus.FORBIDDEN);
        }
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    @GetMapping("/s3-bucket-objects-count/{bucketName}")
    public ResponseEntity<Map<String,Long>> getS3BucketObjectsCount(@PathVariable String bucketName) {
        Map<String,Long> result = new HashMap<>();
        Long count = awsService.getS3BucketObjectsCount(bucketName);
        result.put("Count", count);
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    @GetMapping("/s3-bucket-objects/{bucketName}/{pattern}")
    public ResponseEntity<List<String>> getS3BucketObjectLike(@PathVariable String bucketName, @PathVariable String pattern) {
        List<String> result = new ArrayList<>();
        try{
            result = awsService.getS3BucketObjectLike(bucketName, pattern);
        } catch (Exception e){
            log.error("Request Failed", e);
            return new ResponseEntity<>(result, HttpStatus.FORBIDDEN);
        }
        return new ResponseEntity<>(result, HttpStatus.OK);
    }
}



