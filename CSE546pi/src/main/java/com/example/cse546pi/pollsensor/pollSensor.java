package com.example.cse546pi.pollsensor;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.sql.Timestamp;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.example.cse546pi.s3sqsconf.S3SQSConf;

@Component
public class pollSensor {

	@Autowired
	private S3SQSConf s3sqs;
	
	@EventListener(ApplicationReadyEvent.class)
	public void keepPollingSensor() {
		
		s3sqs.localContEvaluation();
		int count=0;
		
		while (true) {
			try {
		           ProcessBuilder checkSensor = new ProcessBuilder("/bin/sh", "-c", "cat /sys/class/gpio/gpio25/value");
		           Process p0 = checkSensor.start();
		           p0.waitFor();
		           BufferedReader br=new BufferedReader(new InputStreamReader(p0.getInputStream()));
		           String line = br.readLine();
		           if (line.equals("1")){
		        	   

		                Timestamp timestamp = new Timestamp(System.currentTimeMillis());
		                
		                count+=1;
		                
		                System.out.println("\n\n\nIntrusion detected number: "+ count+ "\n\n\n");
		                
				        String fileName="vid"+Long.toString((timestamp.getTime()))+".h264";
						
				        try {
				           ProcessBuilder createVideo = new ProcessBuilder("/bin/sh", "-c", "raspivid -o /home/pi/rvideos/"+fileName+" -t 5000").inheritIO();
				           Process p1 = createVideo.start();
				           p1.waitFor();
				           TimeUnit.MILLISECONDS.sleep(800);
				           
				        } catch (Exception ex) {
					           System.out.println(ex.getMessage());
								ex.printStackTrace();
					      }
				        
				        s3sqs.putonS3SQS(fileName,"/home/pi/rvideos/"+fileName);
		           }
			} catch (Exception ex) {
				System.out.println(ex.getMessage());
				ex.printStackTrace();
	          }
		}
	}
}
