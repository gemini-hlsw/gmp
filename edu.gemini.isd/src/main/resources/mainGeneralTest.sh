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

#Open the adc cover
read -p "Press enter to open the adc cover"
java -jar $JAR -set SCO:CC:cover.cover -type integer -value 1

# prism to OUT
read -p "Press enter to change the prisms to OUT"
java -jar $JAR -set SCO:CC:ADC.corrector -type integer -value  1

# adc from manual to track
read -p "Press enter to adc mode from manual to track"
java -jar $JAR -set SCO:CC:ADC.prismControl -type integer -value 1

# Close VPH
read -p "Press enter to collapse VPH GR"
java -jar $JAR -set SCO:CC:VPH_gr.grism -type integer -value 0

# Open Shutter
read -p "Press enter to open the shutter G"
java -jar $JAR -set SCO:DC:VIS_gr.shutterG -type integer -value 1

# Change the health of VPH to warning, bad and unknown
read -p "Press enter to change the health of the detector of NIR Y to warning (yellow border)"
java -jar $JAR -set SCO:DC:NIR_y.health -type integer -value 2
read -p "Press enter to change the health of the detector of NIR Y to bad (red border)"
java -jar $JAR -set SCO:DC:NIR_y.health -type integer -value 1
read -p "Press enter to change the health of the detector of NIR Y to unknown (no border)"
java -jar $JAR -set SCO:DC:NIR_y.health -type integer -value 0
