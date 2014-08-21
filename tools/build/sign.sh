#!/bin/bash

FILE="./dist/server/artifacts/cattle.jar"
SIG="${FILE}.asc"

if [ ! -e ${FILE} ]; then
    echo Missing $FILE to sign
    exit 1
fi

gpg -ba -o $SIG $FILE

echo Signed $FILE
echo Created $SIG
