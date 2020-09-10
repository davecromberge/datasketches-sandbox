#!/bin/bash

PORT=8099

usage ()
{
  echo 'Uploads a multipart file to the distinct count endpoint.'
  echo 'The file should contain line separated identifiers'
  echo ''
  echo 'Usage : count_distinct -k <sketch-key> <filename>'
  exit
}

if [ "$#" -ne 3 ]
then
  usage
fi

while [ "$1" != "" ]; do
case $1 in
        -k )           shift
                       SKETCH_KEY=$1
                       ;;
        * )            FILENAME=$1
    esac
    shift
done

if [ "$SKETCH_KEY" = "" ]
then
    usage
fi
if [ "$FILENAME" = "" ]
then
    usage
fi

echo "Uploading to sketch endpoint..."
time curl -F filename=@$FILENAME  "http://127.0.0.1:$PORT/v1/distinct/count/$SKETCH_KEY"

echo "Uploading to exact endpoint..."
time curl -F filename=@$FILENAME  "http://127.0.0.1:$PORT/v1/distinct/count/$SKETCH_KEY?exact"

