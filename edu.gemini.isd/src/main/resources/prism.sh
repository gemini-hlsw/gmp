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

echo -n "Enter the degree you want to move both prisms to: " 
read grados

if [[ $grados -lt 0 || $grados -gt 360 ]]; then
    echo "Invalid value. Please enter a number between 0 and 360."
    exit 1
fi

# Validate if the input its a multiple of 5
if [[ $((grados % 5)) -ne 0 ]]; then
    echo "Invalid value. Please enter a multiple of 5 (0, 5, 10, ..., 355, 360)."
    exit 1
fi

valorDiff=0
valorJoint=0

while [[ $valorDiff -ne $grados || $valorJoint -ne $grados ]]; do
    if [[ $valorDiff -ne $grados ]]; then
        valorDiff=$((valorDiff + 5))

        if [[ $valorDiff -gt 360 ]]; then
            valorDiff=0
        fi

        java -jar "$JAR" -set SCO:CC:ADC.diffPrism -type integer -value $valorDiff
        echo "Moving diffPrism to $valorDiff degrees"
    fi

    if [[ $valorJoint -ne $grados ]]; then
        valorJoint=$((valorJoint - 5))

        if [[ $valorJoint -lt 0 ]]; then
            valorJoint=355
        fi

        java -jar "$JAR" -set SCO:CC:ADC.jointPrism -type integer -value $valorJoint
        echo "Moving jointPrism to $valorJoint degrees"
    fi
done

echo "Movement complete! Both prisms are at $grados degrees."