#!/bin/bash

usage ()
{
  echo 'Usage : generate_identifiers -n <num-identifiers> <filename>'
  exit
}

if [ "$#" -ne 3 ]
then
  usage
fi

while [ "$1" != "" ]; do
case $1 in
        -n )           shift
                       NUM=$1
                       ;;
        * )            FILENAME=$1
    esac
    shift
done

if [ "$NUM" = "" ]
then
    usage
fi
if [ "$FILENAME" = "" ]
then
    usage
fi

I=1

echo "Generating $NUM identifiers in $FILENAME..."

while [ $I -le $NUM ]; do
  uuidgen >> $FILENAME
	I=$(($I+1))
done

