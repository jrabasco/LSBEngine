#!/bin/bash

certbot renew --webroot -w /var/www/DOMAIN
nginx -s reload
