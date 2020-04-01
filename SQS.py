
#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Created on Mon Mar 16 21:24:15 2020

@author: teja
"""


import boto3
from botocore.client import Config
from botocore.exceptions import ClientError


class InstanceManagement :
    
    def __init__(self):
        self.message_bodies=[]
        self.aws_access_key_id  = 'AKIAJFQZMNQMZQ5AFJRA'
        self.aws_secret_access_key = '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'
        self.region_name = 'us-east-1'
        self.queue_name = 'sqscse546'
        self.max_queue_messages = 10
        self.sqsUrl = 'https://sqs.us-east-1.amazonaws.com/146737287573/sqscse546'

    def monitorInstances(self):
        
        ec2 = boto3.resource(
        'ec2',
        aws_access_key_id=self.aws_access_key_id,
        aws_secret_access_key=self.aws_secret_access_key,
        config=Config(signature_version='s3v4',region_name = self.region_name )
        )
        
        self.stoppedEC2Instances = 0
        self.runningEC2Instances = 0
        self.pendingEC2Instances = 0
        self.terminatedEC2Instances = 0
        self.TotalEC2Instances   = 0
        self.stoppedEC2InstanceIds = []
        self.runningEC2InstanceIds = []
        self.pendingEC2InstanceIds = []
        self.terminatedEC2InstanceIds = []
              
        for instance in ec2.instances.all():
            if instance.state['Name'] == 'stopped' or instance.state['Code'] == '80':
                self.stoppedEC2Instances +=1
                self.stoppedEC2InstanceIds.append(instance.id)
            elif instance.state['Name'] == 'running' or instance.state['Code'] == '32':
                self.runningEC2Instances +=1
                self.runningEC2InstanceIds.append(instance.id)
            elif instance.state['Name'] == 'pending' or instance.state['Code'] == '0':
                self.pendingEC2Instances +=1
                self.pendingEC2InstanceIds.append(instance.id)                
            elif instance.state['Name'] == 'terminated' or instance.state['Code'] == '48':
                self.terminatedEC2Instances +=1
                self.terminatedEC2InstanceIds.append(instance.id)
            self.TotalEC2Instances+=1
            
        
    def SQSQueueLen(self):

        sqs = boto3.client('sqs', region_name=self.region_name,
                aws_access_key_id=self.aws_access_key_id,
                aws_secret_access_key=self.aws_secret_access_key)
           
        queueCountVisible = sqs.get_queue_attributes(QueueUrl=self.sqsUrl,AttributeNames=['All'])['Attributes']['ApproximateNumberOfMessages'] 
        queueCountNotVisible = sqs.get_queue_attributes(QueueUrl=self.sqsUrl,AttributeNames=['All'])['Attributes']['ApproximateNumberOfMessagesNotVisible']  
        queueCount =  int(queueCountVisible)  +  int(queueCountNotVisible)
        return  int(queueCount)


    def loadBalancer(self):
        
        ec2 = boto3.client(
        'ec2',
        aws_access_key_id=self.aws_access_key_id,
        aws_secret_access_key=self.aws_secret_access_key,
        config=Config(signature_version='s3v4',region_name = self.region_name )
        )
    
        SQSQueuelen = int(self.SQSQueueLen()) 

        self.monitorInstances()
        
        diff = (SQSQueuelen - self.runningEC2Instances-self.pendingEC2Instances-1)
        if diff > self.stoppedEC2Instances :
            diff  = self.stoppedEC2Instances 
        print("diff",diff)   
        print("sqslen",SQSQueuelen)         
        if diff>0:
              try:
                 response = ec2.start_instances(InstanceIds=self.stoppedEC2InstanceIds[:diff], DryRun=False)
              except ClientError as e:
                 print(e)
                 
                 
    def stopAllInstances(self):
        ec2 = boto3.client(
        'ec2',
        aws_access_key_id=self.aws_access_key_id,
        aws_secret_access_key=self.aws_secret_access_key,
        config=Config(signature_version='s3v4',region_name = self.region_name )
        )
        self.monitorInstances()
        
        try:
                 response = ec2.stop_instances(InstanceIds=self.runningEC2InstanceIds[:20], DryRun=False)
        except ClientError as e:
                 print(e)
        
        



    
if __name__ == "__main__":
    
     ec2Instance = InstanceManagement()
     #ec2Instance.stopAllInstances()
     
     
     while True:
       ec2Instance.loadBalancer()
     
      
     

    

'''
def createKeyPair(ACCESS_KEY_ID,ACCESS_SECRET_KEY):
    
   
   ec2 = boto3.resource(
    'ec2',
    aws_access_key_id=ACCESS_KEY_ID,
    aws_secret_access_key=ACCESS_SECRET_KEY,
    config=Config(signature_version='s3v4',region_name = 'us-east-1')
)
   # create a file to store the key locally
   outfile = open('ec2KeyPairCloudp1.pem','w')
   # call the boto ec2 function to create a key pair
   key_pair = ec2.create_key_pair(KeyName='ec2KeyPairCloudp1.pem')
   # capture the key and store it in a file
   KeyPairOut = str(key_pair.key_material)
   print(KeyPairOut)
   outfile.write(KeyPairOut)


def SQSEC2Creation(region_name,SQSQueueLen,ACCESS_KEY_ID,ACCESS_SECRET_KEY):
    i-0f5cf57e07a2832ce
    
    ec2 = boto3.resource('ec2',aws_access_key_id=ACCESS_KEY_ID,aws_secret_access_key=ACCESS_SECRET_KEY,
    config=Config(signature_version='s3v4',region_name = 'us-east-1'))
    instances = ec2.create_instances(
                 ImageId='ami-0903fd482d7208724',
                 #ImageId='ami-09a7fe78668f1e2c0',
                 MinCount=1,
                 MaxCount=1,
                 InstanceType='t2.micro',
                 KeyName='ec2KeyPairCloudp1.pem'
                 )
    for instance in ec2.instances.all():
       print (instance.id , instance.state) 
       
     def snapshotCreation(region_name,sqsQueueLen,aws_access_key_id,aws_secret_access_key):
    instance_id = 'i-0f5cf57e07a2832ce'
    ec2 = boto3.client('ec2',aws_access_key_id=aws_access_key_id,aws_secret_access_key=aws_secret_access_key,
    config=Config(signature_version='s3v4',region_name = 'us-east-1'))
    
    print('Final',sqsQueueLen)
    
    for i in range(int(sqsQueueLen)-1):  
        print("i",i)
        try:
          ec2.create_image(InstanceId=instance_id, NoReboot=True, Name="cloud"+str(i))
        except Exception as e:
            print ("e",e)
'''
    
    






   
