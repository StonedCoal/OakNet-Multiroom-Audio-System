#!/bin/sh
# redirect stdout/stderr to a file
exec > /var/app/data/aptlog.txt 2>&1

apt update
apt install -y build-essential python3-dev
apt install -y python3 python3-pip 
apt install -y libasound2-dev

exec > /var/app/data/pipllog.txt 2>&1

pip3 install pyalsaaudio

python3 -u /var/app/mainClient.py > /var/app/data/pythonlog.txt 2>&1

