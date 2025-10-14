FROM redis:7-alpine

# Copy sentinel configuration file from the project root
COPY ./docker/redis-conf/sentinel.conf /etc/redis/sentinel.conf

# Ensure the configuration file is writable
RUN chmod 644 /etc/redis/sentinel.conf

# Set up data directory with proper permissions
RUN mkdir -p /data && chown redis:redis /data

# Expose sentinel port
EXPOSE 26379

# Start Redis as the sentinel
CMD ["redis-server", "/etc/redis/sentinel.conf", "--sentinel"]