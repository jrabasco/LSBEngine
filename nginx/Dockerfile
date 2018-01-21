FROM nginx:latest

# Configuration
RUN mkdir -p /etc/nginx/logs
RUN touch /etc/nginx/logs/lsbengine.access
RUN rm /etc/nginx/conf.d/default.conf
COPY nginx.conf /etc/nginx/

# Robots
RUN mkdir -p /var/www/robots
ADD robots.txt /var/www/robots/robots.txt

COPY startup.sh /
RUN mkdir -p /var/www/css
COPY css /var/www/css
RUN mkdir -p /var/www/js
COPY js /var/www/js
RUN mkdir -p /var/www/assets
COPY assets /var/www/assets

CMD ["sh", "/startup.sh"]
