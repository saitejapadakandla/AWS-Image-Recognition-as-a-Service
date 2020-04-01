mkdir -p /home/pi/darknet/tempdir
Xvfb :1 & export DISPLAY=:1
echo 23 > /sys/class/gpio/export
echo in > /sys/class/gpio/gpio23/direction

#read status using "cat /sys/class/gpio/gpio23/value"

