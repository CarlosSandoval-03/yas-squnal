FROM node:18-alpine

WORKDIR /app

COPY package*.json ./
RUN npm install

COPY . .

EXPOSE 3000

ENV REACT_APP_API_BASE=http://localhost:5000

CMD ["npm", "start"]

