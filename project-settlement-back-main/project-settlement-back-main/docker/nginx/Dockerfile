FROM alpine:latest
RUN apk add --no-cache nginx nginx-mod-http-headers-more
RUN mkdir -p \
    /run/nginx \
    /var/cache/nginx \
    /var/log/nginx
COPY nginx/nginx.conf /etc/nginx/nginx.conf
COPY frontend/ /usr/share/nginx/html/
EXPOSE 80 443
CMD ["/bin/sh", "-c", "\
  mkdir -p \
    /var/cache/nginx/client_temp \
    /var/cache/nginx/proxy_temp \
    /var/cache/nginx/fastcgi_temp \
    /var/cache/nginx/uwsgi_temp \
    /var/cache/nginx/scgi_temp \
    /var/log/nginx && \
  nginx -g 'daemon off;' \
"]