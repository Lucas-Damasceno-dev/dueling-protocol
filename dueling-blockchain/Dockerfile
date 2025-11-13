# Use an official Node.js runtime as a parent image
FROM node:18-alpine

# Set the working directory in the container
WORKDIR /usr/src/app

# Copy package.json and package-lock.json to the working directory
COPY package*.json ./

# Install any needed packages
RUN npm install

# Bundle app source
COPY . .

# Make start script executable
RUN chmod +x start-blockchain.sh

# Make port 8545 available to the world outside this container
EXPOSE 8545

# Run the blockchain with auto-deploy
CMD ["./start-blockchain.sh"]
