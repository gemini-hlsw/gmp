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

echo -n "Add a status to set it to all the health status: " 
echo "0: Unknown, 1: Bad, 2: Warning, 3: Good " 

read state

java -jar $JAR -set SCO:IS.health -type integer -value $state
java -jar $JAR -set SCO:CC:ADC_CC.health -type integer -value $state
java -jar $JAR -set SCO:DET:CRYO.health -type integer -value $state
java -jar $JAR -set SCO:CC:VPH_gr.health -type integer -value $state
java -jar $JAR -set SCO:CC:VPH_hk.health -type integer -value $state
java -jar $JAR -set SCO:CC:VPH_jy.health -type integer -value $state
java -jar $JAR -set SCO:CC:VPH_zi.health -type integer -value $state
java -jar $JAR -set SCO:CC:FP.health -type integer -value $state
java -jar $JAR -set SCO:DC:NIR_h.health -type integer -value $state
java -jar $JAR -set SCO:DC:NIR_k.health -type integer -value $state
java -jar $JAR -set SCO:DC:NIR_j.health -type integer -value $state
java -jar $JAR -set SCO:DC:NIR_y.health -type integer -value $state
java -jar $JAR -set SCO:CC:PIC.health -type integer -value $state
java -jar $JAR -set SCO:DC:SVC.health -type integer -value $state
java -jar $JAR -set SCO:DC:VIS_gr.health -type integer -value $state
java -jar $JAR -set SCO:DC:VIS_zi.health -type integer -value $state
