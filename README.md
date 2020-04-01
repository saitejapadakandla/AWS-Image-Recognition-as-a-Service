# AWS-Image-Recognition-as-a-Service
Project 1 - IaaS - Amazon Web Services


1) The aim of the project was to develop an elastic and responsive application that utilizes cloud resources and IoT devices (Raspberry Pi B+) in order to provide Image Recognition as a Service to users by using the AWS cloud resources and Raspberry Pi as edge computing model to perform deep learning on images provided by the users by utilizing a lightweight deep learning framework, Darknet.

2) The deep learning model was provided in an AWS image(Custom image ID:ami-0903fd482d7208724,Region:us-east-1(North Virginia))and Raspberry Pi. These applications invokes this model to perform image recognition on the received images.The load should be distributed accordingly between Raspberry Pi and EC2 instances in order to achieve better end to end latency.

3) These applications(EC2 instances and Raspberry Pi) will handle multiple requests concurrently. EC2 instances will automatically scale out when the request demand increases, and automatically scale in when the demand drops. However, this number is limited to 20 instances as per project requirements. Any request more than 20 will be kept waiting.

4) AWS services used in the project are : 
  i) Elastic Compute Cloud (EC2)
 ii) Simple Queue Service (SQS)
iii) Simple Storage Service (S3)

Further details are provided in the report.


Listener :
----------

1) This application runs inside the app instances and listens for messages (requests) in the Input Queue.
2) When the message arrives, it takes the message and runs the deep learning model for classification and puts the      classification result into a S3 bucket. The classification result is also inserted into the Output Queue.
3) When there is no message in the Input queue, the application shuts down the instance in which its running, facilitating scale in.

Listener Running :
------------------
This is the same as Listener application but the instance which is running this application won't terminate at all, facilitating quick response to the user.
