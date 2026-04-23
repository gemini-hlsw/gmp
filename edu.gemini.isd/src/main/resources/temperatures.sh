#!/bin/bash
# These StatusItem are defined in the spreadsheet Data dictionary
# https://docs.google.com/spreadsheets/d/1OMu5tEHHBd4GNI9MWErGx2CvYIffGhWO/edit?gid=1641973094#gid=1641973094

# Just a way to interact directly with giapi tester without moving the jar file
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
GIAPI_ROOT="$(cd "$SCRIPT_DIR/../../../.." && pwd)"
JAR="$GIAPI_ROOT/giapi-tester/target/giapi-tester.jar"

if [ ! -f "$JAR" ]; then
    echo "JAR not found: $JAR"
    exit 1
fi

MIN=60
MAX=90

while true; do
    # VIS Detector
    tempG=$(echo "scale=1; $(( RANDOM % (MAX*100 - MIN*100 + 1) + MIN*100 )) / 100" | bc -l)
    tempR=$(echo "scale=1; $(( RANDOM % (MAX*100 - MIN*100 + 1) + MIN*100 )) / 100" | bc -l)
    tempI=$(echo "scale=1; $(( RANDOM % (MAX*100 - MIN*100 + 1) + MIN*100 )) / 100" | bc -l)
    tempZ=$(echo "scale=1; $(( RANDOM % (MAX*100 - MIN*100 + 1) + MIN*100 )) / 100" | bc -l)

	java -jar $JAR -set SCO:CC:DET:temp_gr.detector_g -type double -value $tempG
    echo "Temperature of detector G: $tempG °K"

	java -jar $JAR -set SCO:CC:DET:temp_gr.detector_r -type double -value $tempR
    echo "Temperature of detector R: $tempR °K"

	java -jar $JAR -set SCO:CC:DET:temp_iz.detector_i -type double -value $tempI
    echo "Temperature of detector I: $tempI °K"

	java -jar $JAR -set SCO:CC:DET:temp_iz.detector_z -type double -value $tempZ
    echo "Temperature of detector Z: $tempZ °K"

    # NIR Detector
    tempY=$(echo "scale=1; $(( RANDOM % (MAX*100 - MIN*100 + 1) + MIN*100 )) / 100" | bc -l)
    tempJ=$(echo "scale=1; $(( RANDOM % (MAX*100 - MIN*100 + 1) + MIN*100 )) / 100" | bc -l)
    tempH=$(echo "scale=1; $(( RANDOM % (MAX*100 - MIN*100 + 1) + MIN*100 )) / 100" | bc -l)
    tempK=$(echo "scale=1; $(( RANDOM % (MAX*100 - MIN*100 + 1) + MIN*100 )) / 100" | bc -l)

	java -jar $JAR -set SCO:CC:DET:temp_Y.detector -type double -value $tempY
    echo "Temperature of detector Y: $tempY °K"

	java -jar $JAR -set SCO:CC:DET:temp_J.detector -type double -value $tempJ
    echo "Temperature of detector J: $tempJ °K"

	java -jar $JAR -set SCO:CC:DET:temp_H.detector -type double -value $tempH
    echo "Temperature of detector H: $tempH °K"

	java -jar $JAR -set SCO:CC:DET:temp_K.detector -type double -value $tempK
    echo "Temperature of detector K: $tempK °K"

    # VIS Bench
    temp1=$(echo "scale=1; $(( RANDOM % (MAX*100 - MIN*100 + 1) + MIN*100 )) / 100" | bc -l)
    temp2=$(echo "scale=1; $(( RANDOM % (MAX*100 - MIN*100 + 1) + MIN*100 )) / 100" | bc -l)
    temp3=$(echo "scale=1; $(( RANDOM % (MAX*100 - MIN*100 + 1) + MIN*100 )) / 100" | bc -l)
    temp4=$(echo "scale=1; $(( RANDOM % (MAX*100 - MIN*100 + 1) + MIN*100 )) / 100" | bc -l)

	java -jar $JAR -set SCO:CC:DET:temp_vis.bench1 -type double -value $temp1
    echo "Temperature of detector 1: $temp1 °K"

	java -jar $JAR -set SCO:CC:DET:temp_vis.bench2 -type double -value $temp2
    echo "Temperature of detector 2: $temp2 °K"

	java -jar $JAR -set SCO:CC:DET:temp_vis.bench3 -type double -value $temp3
    echo "Temperature of detector 3: $temp3 °K"

	java -jar $JAR -set SCO:CC:DET:temp_vis.bench4 -type double -value $temp4
    echo "Temperature of detector 4: $temp4 °K"

    # NIR Bench
    tempA1=$(echo "scale=1; $(( RANDOM % (MAX*100 - MIN*100 + 1) + MIN*100 )) / 100" | bc -l)
    tempA2=$(echo "scale=1; $(( RANDOM % (MAX*100 - MIN*100 + 1) + MIN*100 )) / 100" | bc -l)
    tempA3=$(echo "scale=1; $(( RANDOM % (MAX*100 - MIN*100 + 1) + MIN*100 )) / 100" | bc -l)
    tempA4=$(echo "scale=1; $(( RANDOM % (MAX*100 - MIN*100 + 1) + MIN*100 )) / 100" | bc -l)
    tempB1=$(echo "scale=1; $(( RANDOM % (MAX*100 - MIN*100 + 1) + MIN*100 )) / 100" | bc -l)
    tempB2=$(echo "scale=1; $(( RANDOM % (MAX*100 - MIN*100 + 1) + MIN*100 )) / 100" | bc -l)
    tempB3=$(echo "scale=1; $(( RANDOM % (MAX*100 - MIN*100 + 1) + MIN*100 )) / 100" | bc -l)
    tempB4=$(echo "scale=1; $(( RANDOM % (MAX*100 - MIN*100 + 1) + MIN*100 )) / 100" | bc -l)

	java -jar $JAR -set SCO:CC:DET:temp_nir.benchA1 -type double -value $tempA1
    echo "Temperature of detector A1: $tempA1 °K"

	java -jar $JAR -set SCO:CC:DET:temp_nir.benchA2 -type double -value $tempA2
    echo "Temperature of detector A2: $tempA2 °K"

	java -jar $JAR -set SCO:CC:DET:temp_nir.benchA3  -type double -value $tempA3
    echo "Temperature of detector A3: $tempA3 °K"

	java -jar $JAR -set SCO:CC:DET:temp_nir.benchA4 -type double -value $tempA4
    echo "Temperature of detector A4: $tempA4 °K"

	java -jar $JAR -set SCO:CC:DET:temp_nir.benchB1 -type double -value $tempB1
    echo "Temperature of detector B1: $tempB1 °K"

	java -jar $JAR -set SCO:CC:DET:temp_nir.benchB2 -type double -value $tempB2
    echo "Temperature of detector B2: $tempB2 °K"

	java -jar $JAR -set SCO:CC:DET:temp_nir.benchB3 -type double -value $tempB3
    echo "Temperature of detector B3: $tempB3 °K"

	java -jar $JAR -set SCO:CC:DET:temp_nir.benchB4 -type double -value $tempB4
    echo "Temperature of detector B4: $tempB4 °K"

done