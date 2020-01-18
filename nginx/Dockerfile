FROM nginx:latest

# Certificate renewal
RUN apt-get update && apt-get install -y certbot && mkdir -p /var/www/DOMAIN
COPY renew_certificate.sh /
RUN chmod +x renew_certificate.sh

RUN mkdir -p /etc/nginx/logs /var/www/css /var/www/js /var/www/assets\
  && touch /etc/nginx/logs/lsbengine.access\
  && rm /etc/nginx/conf.d/default.conf
COPY nginx.conf /etc/nginx/

COPY startup.sh /
COPY css /var/www/css
COPY js /var/www/js
COPY assets /var/www/assets

CMD ["sh", "/startup.sh"]
