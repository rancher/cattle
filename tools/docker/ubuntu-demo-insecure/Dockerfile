FROM phusion/baseimage:0.9.9
RUN groupadd admin
RUN useradd -m -G admin -s /bin/bash user
RUN echo 'user:password' | chpasswd
EXPOSE 22
