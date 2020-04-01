mkdir -p /home/ubuntu/darknet/tempdir
Xvfb :1 & export DISPLAY=:1
cd /home/ubuntu/darknet/ && java -jar CSE546piListener-0.0.1-SNAPSHOT.jar
shutdown -h now
