#!/bin/sh

# A test script which shall pound the semimeter solution, and
# give it data.

for a in $(seq 15000)
do
     /usr/sbin/ab2 -n 100 -c 10 http://localhost:9013/semimeter/c/mark/$a > /dev/null
done
