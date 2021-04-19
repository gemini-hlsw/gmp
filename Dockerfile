FROM maven:3-openjdk-8

WORKDIR /home/software/gmp

COPY . /home/software/gmp

RUN mvn -Dmaven.test.skip=true install
CMD mvn pax:provision

# docker run -d -p 61616:61616 <nombre-imagen>