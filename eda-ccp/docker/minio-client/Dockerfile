FROM --platform=linux/amd64 minio/mc:RELEASE.2023-01-28T20-29-38Z

RUN mkdir -p /home/.mc/config
RUN chmod 775 /home/.mc/config

CMD ["mc"]