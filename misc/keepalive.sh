#!/bin/bash
function check(){
	ps -ef | grep -v grep | grep java
	# if not found - equals to 1, start it
	if [ $? -eq 1 ]
		then
		echo "Resarting"
		echo `java Test &`
	else
		echo "Everything is fine"
	fi
}

while [ condition ]
do
	check 
	sleep 300
done