FROM ubuntu:latest
RUN apt-get update && apt-get install -y --no-install-recommends npm iptables curl tcpdump
ADD app.js /app/
ADD entry.sh /
ADD package.json /app/
RUN cd /app && npm install .
ENTRYPOINT ["/entry.sh"]
CMD ["nodejs", "/app/app.js"]
EXPOSE 3000
