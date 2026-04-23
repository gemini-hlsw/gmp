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


echo "NOT SVC"
java -jar $JAR -set SCO:DC:SVC.enabled -type integer -value 0

# prism to OUT
java -jar $JAR -set SCO:CC:ADC.corrector -type integer -value  1

# adc from manual to track
java -jar $JAR -set SCO:CC:ADC.prismControl -type integer -value 0

sleep 2

echo -n "Enter the observation time: "
read tiempoTotal  

echo -n "Enter the observation mode: "
echo "Imaging = 1, Spectroscopy = 2"
read estrategiaObservacion

if [[ tiempoTotal -lt 0 ]]; then
   echo "The entered value is not valid. Please enter a number greater than or equal to 0." 
   exit 1  
fi

java -jar $JAR -set SCO:IS:obs.state -type integer -value 1

MIN=10000000 
MAX=99999999 

datalabel=$(echo "$RANDOM % ($MAX - $MIN + 1) + $MIN" | bc)  
java -jar $JAR -set SCO:IS:obs.dataLabel -type integer -value $datalabel
echo "Data Label: $datalabel" 

if [[ $modoObservacion -lt 0 ]]; then
   echo "Invalid value for observation mode. Please enter a number greater than or equal to 0."
   exit 1    
elif [[ $modoObservacion -gt 2 ]]; then   
   echo "Invalid value for observation mode. Please enter a number less than 2." 
   exit 1      
else
    java -jar $JAR -set SCO:IS:obs.mode -type integer -value $modoObservacion 
    echo "Observation mode: $modoObservacion" 
fi

if [[ $estrategiaObservacion -lt -1 ]]; then  
   echo "Invalid value for observation mode. Please enter a number greater than or equal to -1."
   exit 1   
elif [[ $estrategiaObservacion -gt 2 ]]; then
   echo "Invalid value for observation strategy. Please enter a number less than 2." 
   exit 1
else
   java -jar $JAR -set SCO:IS:obs.strategy -type integer -value $estrategiaObservacion
   echo "Observation Strategy: $estrategiaObservacion" 
fi

java -jar $JAR -set SCO:IS:obs.state -type integer -value 2


java -jar $JAR -set SCO:IS:obs.timeTotal -type integer -value $tiempoTotal
echo "Total time: $tiempoTotal seconds"


echo "opening the cover, please pay attention to the shutter that we are going to open"
sleep 10
#Open the adc cover
java -jar $JAR -set SCO:CC:cover.cover -type integer -value 1

echo "opening the shutter"
sleep 5
java -jar $JAR -set SCO:DC:VIS_gr.shutterG -type integer -value 1
java -jar $JAR -set SCO:DC:VIS_gr.shutterR -type integer -value 1
sleep 5
java -jar $JAR -set SCO:DC:VIS_zi.shutterZ -type integer -value 1
java -jar $JAR -set SCO:DC:VIS_zi.shutterI -type integer -value 1


while [[ $tiempoTotal -gt 0 ]]; do
   java -jar $JAR -set SCO:IS:obs.timeLeft -type integer -value $tiempoTotal
   echo "Time left: $tiempoTotal seconds"
   tiempoTotal=$((tiempoTotal - 1))
   sleep 1
done

java -jar $JAR -set SCO:IS:obs.timeLeft -type integer -value $tiempoTotal
echo "Time left $tiempoTotal seconds"
java -jar $JAR -set SCO:IS:obs.state -type integer -value 3


