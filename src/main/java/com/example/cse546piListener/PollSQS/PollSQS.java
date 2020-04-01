package com.example.cse546piListener.PollSQS;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.example.cse546piListener.s3sqsconf.S3SQSConf;

@Component
public class PollSQS {
	
	@Autowired
	private S3SQSConf s3sqs;
	
	@Autowired
    private ApplicationContext appContext;
	
	@EventListener(ApplicationReadyEvent.class)
	public void keepPollingSQS() {
            	
        s3sqs.retrieveFromS3SQS();
        SpringApplication.exit(appContext, () -> 0);
    }

}
