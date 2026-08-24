FROM maven:3-openjdk-8

WORKDIR /home/software/gmp

COPY . /home/software/gmp

RUN mvn -Dmaven.test.skip=true install
RUN mvn -q -pl gmp-main dependency:copy-dependencies

CMD ["java", \
     "-Dconf.base=src/main/config", \
     "-Dlogs.dir=logs", \
     "-cp", "gmp-main/target/classes:gmp-main/target/dependency/*", \
     "edu.gemini.aspen.gmp.main.GmpMain"]

# docker run -d -p 61616:61616 <nombre-imagen>
