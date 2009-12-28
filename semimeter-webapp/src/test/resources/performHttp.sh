#!/bin/sh

# A test script which shall pound the semimeter solution, and
# give it data.

for a in $(seq 15000)
do
     GET -d http://localhost:9013/semimeter/c/mark/$a
done