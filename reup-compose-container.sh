#!/bin/sh
[ -z ${1+x} ] && echo "First arg is container name" && exit 1
docker-compose stop "$1" && docker-compose rm -f "$1" && docker-compose up -d "$1"
