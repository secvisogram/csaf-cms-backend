#!/bin/bash

set -eo pipefail

if [ -z "$TAG" ]
then
    echo "Cloning latest version"
    git clone https://github.com/gocsaf/csaf.git
else
    echo "Cloning TAG $TAG"
    if ! git clone --branch $TAG https://github.com/gocsaf/csaf.git
    then
		echo "TAG $TAG not found. Cloning latest version"
		git clone https://github.com/gocsaf/csaf.git
	fi
fi

cd /app/csaf/
export PATH=$PATH:/usr/local/go/bin
make build_linux

git describe --tags --always > /version
