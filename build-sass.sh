#!/bin/sh
mkdir -p src/main/resources/css
sass --style compressed src/main/resources/scss/styles.scss src/main/resources/css/styles.css
