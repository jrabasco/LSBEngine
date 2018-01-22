default: run

run: build
	sbt run

build: build-resume build-sass minify-js
	mkdir -p src/main/resources/assets/
	cp -r assets/* src/main/resources/assets/
	mkdir -p src/main/resources/js
	cp js/admin.min.js src/main/resources/js/admin.min.js
	mkdir -p src/main/resources/css
	cp css/styles.css src/main/resources/css

build-resume:
	mkdir -p assets
	bash build-resume.sh

build-sass:
	mkdir -p css
	sass --style compressed src/main/scss/styles.scss css/styles.css

minify-js:
	mkdir -p js
	uglifyjs src/main/js/admin.js > js/admin.min.js

build-lsbengine-docker: build
	sbt docker:publishLocal

build-nginx: build
	cp -r assets nginx/
	cp -r js nginx/
	cp -r css nginx/
	cp nginx/nginx.conf nginx/nginx.conf.back
	sed -i s/public.lsbengine.me/"${PUBLIC_DOMAINS}"/g nginx/nginx.conf
	sed -i s/admin.lsbengine.me/${ADMIN_DOMAINS}/g nginx/nginx.conf
	docker build nginx -t my-nginx:latest
	mv nginx/nginx.conf.back nginx/nginx.conf
	rm -r nginx/assets
	rm -r nginx/js
	rm -r nginx/css

build-docker: build-lsbengine-docker build-nginx
