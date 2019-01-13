#!/bin/sh -e


openssl pkcs12 -export -in $1 -inkey $2 > $3





