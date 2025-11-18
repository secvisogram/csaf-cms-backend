#!/bin/bash

if [ -z "$TAG" ]
then
    echo "Cloning latest version"
    git clone https://github.com/csaf-poc/csaf_distribution.git >/dev/null 2>&1
else
    echo "Cloning TAG $TAG"
    git clone --branch $TAG https://github.com/csaf-poc/csaf_distribution.git >/dev/null 2>&1
    if [ $? -ne 0 ]
	then
		echo "TAG $TAG not found. Cloning latest version"
		git clone https://github.com/csaf-poc/csaf_distribution.git >/dev/null 2>&1
	fi
fi

cd /app/csaf_distribution/
export PATH=$PATH:/usr/local/go/bin
make build_linux

git describe --tags --always > /version
