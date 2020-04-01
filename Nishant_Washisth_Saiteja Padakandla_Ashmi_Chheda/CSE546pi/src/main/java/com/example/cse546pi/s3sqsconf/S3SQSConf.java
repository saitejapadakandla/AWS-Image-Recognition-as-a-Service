package com.example.cse546pi.s3sqsconf;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Async;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.sqs.AmazonSQSAsync;
import com.amazonaws.services.sqs.AmazonSQSAsyncClientBuilder;
import com.amazonaws.services.sqs.model.DeleteMessageRequest;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;

@Configuration
public class S3SQSConf {
	
	@Value("${cloud.aws.region.static}")
	private String region;
	
	@Value("${cloud.aws.credentials.accessKey}")
	private String awsAccessKey;
	
	@Value("${cloud.aws.credentials.secretKey}")
	private String awsSecretKey;
	
	@Value("${sqs.url}")
	private String DestSQS;
	
	@Value("${app.awsServices.bucketName}")
	private String s3bucket;
	
	private Boolean semaphore;
	
	private AmazonSQSAsync myAmazonSQSAsync;
	private AmazonS3 s3client;

	private ReceiveMessageRequest receiveMessageRequest;
	
	@PostConstruct
	private void postConstructor() {
		AWSCredentialsProvider myAWSCredentialsProvider = new AWSStaticCredentialsProvider(new BasicAWSCredentials(this.awsAccessKey,this.awsSecretKey));
		
		this.myAmazonSQSAsync=AmazonSQSAsyncClientBuilder.standard().withRegion(this.region).withCredentials(myAWSCredentialsProvider).build();
	    this.s3client = AmazonS3ClientBuilder.standard().withRegion(this.region).withCredentials(myAWSCredentialsProvider).build();
	    //this.semaphore = false;
	    this.semaphore = true;
	    
	    this.receiveMessageRequest = new ReceiveMessageRequest(this.DestSQS).withMaxNumberOfMessages(1).withWaitTimeSeconds(20);
	
	}
	
	public List<Message> getOneMessage() {
		return myAmazonSQSAsync.receiveMessage(receiveMessageRequest).getMessages();
	}
	
	@Async
	public void localContEvaluation() {
		
		while (true) {
            
			List<Message> messages = this.getOneMessage();
			for (Message theMessage : messages) {
				
				String videoFileName=theMessage.getBody().toString();
				
				File newvideofile = new File("tempdir/"+videoFileName);
				long startTime = 0;
				long estimatedTime = 0;
				
				
				try {
					
				    FileInputStream inputStream = new FileInputStream(new File("/home/pi/rvideos/"+videoFileName));
				    FileChannel inChannel = inputStream.getChannel();
				 
				    FileOutputStream outputStream = new FileOutputStream(newvideofile);
				    FileChannel outChannel = outputStream.getChannel();
				 
				    inChannel.transferTo(0, inChannel.size(), outChannel);
				 
				    inputStream.close();
				    outputStream.close();
					
					System.out.println("Started evaluating: "+videoFileName);
				
					startTime = System.currentTimeMillis();
					ProcessBuilder processVideo = new ProcessBuilder("/bin/sh", "-c", "./darknet detector demo cfg/coco.data cfg/yolov3-tiny.cfg yolov3-tiny.weights tempdir/"+videoFileName+" > tempdir/"+videoFileName+".resulti.txt").inheritIO();
			        Process p2 = processVideo.start();
			        p2.waitFor();
			        estimatedTime = System.currentTimeMillis() - startTime;
			        
				} catch (IOException e) {
					 System.err.println(e.getMessage());
					e.printStackTrace();
				} catch (InterruptedException e) {
					 System.err.println(e.getMessage());
					e.printStackTrace();
				}
		
				File resultiFile = new File("tempdir/"+videoFileName+".resulti.txt");
				File resultFile = new File("tempdir/"+videoFileName+".result.txt");
				
				try {
					
			        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(resultiFile), "ISO-8859-1"));
			        
			        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(resultFile)));
			        String data; int numberOfFrames = 0;
			        List<String> values = new ArrayList<>();
			        while ((data = br.readLine()) != null){
			            if(data.contains("video file")){
			                String videoName = "Video File name: "+(data.split(":")[1]);
			                writer.write(videoName);
			                writer.write("\n");
			            }
			            values.add(data);
			        }
			        List<Integer> index = new ArrayList<>();
			        for(int i = 0; i<values.size(); i++){
			            if(values.get(i).contains("Objects")){
			                index.add(i);
			            }
			        }
			        
			        Set<String> totalObjectsDetected= new HashSet<>();
			        Set<String> objectsDetected = new HashSet<>();
			        for(int i = 0; i<index.size()-1; i++){
			            int start = index.get(i)+1;
			            int end = index.get(i+1);
			            numberOfFrames++;
			            for(int m = start; m<end; m++){
			                if(values.get(m).contains("%")){
			                    objectsDetected.add(values.get(m).split(":")[0]);
			                    totalObjectsDetected.add(values.get(m).split(":")[0]);
			                }
			            }
			            String objects = "";
			            for(String val : objectsDetected){
			                objects = objects + val +", ";
			            }
			
			            if(objects.isEmpty()){
			                writer.write(("Frame: "+numberOfFrames+" Objects found: None"));
			            }
			            else{
			                writer.write("Frame: "+numberOfFrames+" Objects found: "+objects.substring(0, objects.length()-2));
			            }
			
			            writer.write("\n");
			            objectsDetected.clear();
			        }
			        
			        String totalObjects = "";
		            for(String val : totalObjectsDetected){
		            	totalObjects = totalObjects + val +", ";
		            }
		            
		            if(totalObjects.isEmpty()){
		                writer.write(("\nNo objects found in the video\n"));
		            }
		            else{
		                writer.write("\nAll objects Detected in the video: "+totalObjects.substring(0, totalObjects.length()-2)+"\n");
		            }
			        
			        writer.write("\nThe video was processed by RaspberryPi\n");
			        
			        
			        if (estimatedTime!=0) {
			        	writer.write("\nThe Darknet took "+ Long.toString(estimatedTime/1000)+" secs to process the video.\n");
			        }
			        
			        writer.close();
			        br.close();
				} catch(IOException e) {
					 System.err.println(e.getMessage());
						e.printStackTrace();
				}
				
				if (this.s3client.doesObjectExist(this.s3bucket, "results/"+videoFileName+".result.txt")==false) {
					this.s3client.putObject(this.s3bucket, "results/"+videoFileName+".result.txt", resultFile);
				}
				this.myAmazonSQSAsync.deleteMessage(new DeleteMessageRequest(this.DestSQS, theMessage.getReceiptHandle()));
				System.out.println("Finished evaluating: "+videoFileName);
				
				newvideofile.delete();
				resultFile.delete();
				resultiFile.delete();
			}
	
		}
	}
	
	@Async
	public void localEvaluation(File videoFile, String videoFileName) {
		
		System.out.println("Started evaluating: "+videoFileName);

		long startTime = 0;
		long estimatedTime = 0;
		
		try {
			startTime = System.currentTimeMillis();
			ProcessBuilder processVideo = new ProcessBuilder("/bin/sh", "-c", "./darknet detector demo cfg/coco.data cfg/yolov3-tiny.cfg yolov3-tiny.weights tempdir/"+videoFileName+" > tempdir/"+videoFileName+".resulti.txt").inheritIO();
	        Process p2 = processVideo.start();
	        p2.waitFor();
	        estimatedTime = System.currentTimeMillis() - startTime;
		} catch (IOException e) {
			 System.err.println(e.getMessage());
			e.printStackTrace();
		} catch (InterruptedException e) {
			 System.err.println(e.getMessage());
			e.printStackTrace();
		}

		File resultiFile = new File("tempdir/"+videoFileName+".resulti.txt");
		File resultFile = new File("tempdir/"+videoFileName+".result.txt");
		
		try {
			
	        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(resultiFile), "ISO-8859-1"));
	        
	        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(resultFile)));
	        String data; int numberOfFrames = 0;
	        List<String> values = new ArrayList<>();
	        while ((data = br.readLine()) != null){
	            if(data.contains("video file")){
	                String videoName = "Video File name: "+(data.split(":")[1]);
	                writer.write(videoName);
	                writer.write("\n");
	            }
	            values.add(data);
	        }
	        List<Integer> index = new ArrayList<>();
	        for(int i = 0; i<values.size(); i++){
	            if(values.get(i).contains("Objects")){
	                index.add(i);
	            }
	        }

	        Set<String> totalObjectsDetected= new HashSet<>();
	        Set<String> objectsDetected = new HashSet<>();
	        for(int i = 0; i<index.size()-1; i++){
	            int start = index.get(i)+1;
	            int end = index.get(i+1);
	            numberOfFrames++;
	            for(int m = start; m<end; m++){
	                if(values.get(m).contains("%")){
	                    objectsDetected.add(values.get(m).split(":")[0]);
	                    totalObjectsDetected.add(values.get(m).split(":")[0]);
	                }
	            }
	            String objects = "";
	            for(String val : objectsDetected){
	                objects = objects + val +", ";
	            }
	
	            if(objects.isEmpty()){
	                writer.write(("Frame: "+numberOfFrames+" Objects found: None"));
	            }
	            else{
	                writer.write("Frame: "+numberOfFrames+" Objects found: "+objects.substring(0, objects.length()-2));
	            }
	
	            writer.write("\n");
	            objectsDetected.clear();
	        }
	        
	        String totalObjects = "";
            for(String val : totalObjectsDetected){
            	totalObjects = totalObjects + val +", ";
            }
            
            if(totalObjects.isEmpty()){
                writer.write(("\nNo objects found in the video\n"));
            }
            else{
                writer.write("\nAll objects Detected in the video: "+totalObjects.substring(0, totalObjects.length()-2)+"\n");
            }
	        
	        writer.write("\nThe video was processed by RaspberryPi\n");
	        
	        
	        if (estimatedTime!=0) {
	        	writer.write("\nThe Darknet took "+ Long.toString(estimatedTime/1000)+" secs to process the video.\n");
	        }
	        
	        writer.close();
	        br.close();
		} catch(IOException e) {
			 System.err.println(e.getMessage());
				e.printStackTrace();
		}

		if (this.s3client.doesObjectExist(this.s3bucket, "results/"+videoFileName+".result.txt")==false) {
			this.s3client.putObject(this.s3bucket, "results/"+videoFileName+".result.txt", resultFile);
		}
		System.out.println("Finished evaluating: "+videoFileName);
		
		videoFile.delete();
		resultFile.delete();
		resultiFile.delete();
		this.semaphore=false;
	}
	
	
	@Async
	public void putonS3SQS(String file_name, String file_path) {
		
		try{
			File videofile = new File(file_path);
			
			if (this.semaphore==false) {
				
				this.semaphore=true;
				
				File newvideofile = new File("tempdir/"+file_name);
				
			    FileInputStream inputStream = new FileInputStream(videofile);
			    FileChannel inChannel = inputStream.getChannel();
			 
			    FileOutputStream outputStream = new FileOutputStream(newvideofile);
			    FileChannel outChannel = outputStream.getChannel();
			 
			    inChannel.transferTo(0, inChannel.size(), outChannel);
			 
			    inputStream.close();
			    outputStream.close();
				
				localEvaluation(newvideofile, file_name);
				
				this.s3client.putObject(this.s3bucket, "videos/"+file_name, videofile);
				
			}
			else {
				String qMessage = file_name;
				this.s3client.putObject(this.s3bucket, "videos/"+file_name, videofile);
				this.myAmazonSQSAsync.sendMessage(this.DestSQS,qMessage);
			}
			//videofile.delete();
		 
		} catch (AmazonServiceException e) { 
			 System.err.println(e.getErrorMessage());
				e.printStackTrace();
		 } catch (IOException e) {
			 System.err.println(e.getMessage());
				e.printStackTrace();
		} 
		 
	}
	
	public String getVideoUrl(String file_name) {

		 return s3client.getUrl(this.s3bucket, file_name).toString();
		
	}

}

