#!/bin/bash
(set -o igncr) 2>/dev/null && set -o igncr; # this comment is required for handling Windows cr/lf 
# See StackOverflow answer http://stackoverflow.com/a/14607651

GH_HOME=$(dirname "$0")
JAVA=$JAVA_HOME/bin/java
if [ "$JAVA_HOME" = "" ]; then
 JAVA=java
fi

vers=$($JAVA -version 2>&1 | grep "java version" | awk '{print $3}' | tr -d \")
bit64=$($JAVA -version 2>&1 | grep "64-Bit")
if [ "$bit64" != "" ]; then
  vers="$vers (64bit)"
fi
echo "## using java $vers from $JAVA_HOME"

ACTION=$1
FILE=$2

function printUsage {
 echo
 echo "./borouter.sh importTracks|server " 
 # echo "./graphhopper.sh clean|build|buildweb|help"
 echo
 echo "  help                 this message"
 echo "  importTracks path    import Tracks and store the often used edges in database"
 echo "  server               starts a local server for user access at localhost:8989 and API access at localhost:8989/route"
 exit
}

if [ "$ACTION" = "" ]; then
 echo "## action $ACTION not found. try" 
 printUsage
fi

VERSION=$(grep  "<name>" -A 1 pom.xml | grep version | cut -d'>' -f2 | cut -d'<' -f1)
JAR=target/popularRoutes-$VERSION-with-dep.jar

echo "Graph: $GRAPH"
echo "Version: $VERSION"
echo "Jar: $JAR"

if [ "$JAVA_OPTS" = "" ]; then
  JAVA_OPTS="-Xmx8G -Xms5G -server"
fi

echo "## now $ACTION. JAVA_OPTS=$JAVA_OPTS"

if [ "$ACTION" = "server" ]; then
  CONFIG="files/server.properties"
  CLASS="de.popularRoutes.http.PopRoutesServer"
  
  if [ "$JETTY_PORT" = "" ]; then  
    JETTY_PORT=8989
  fi

  RC_BASE=src/main/webapp

  exec "$JAVA" $JAVA_OPTS -cp "/home/vk/Dokumente/repositories/routenPlanung/popularRoutes/src/main/java/:$JAR" $CLASS \
  	jetty.resourcebase=$RC_BASE jetty.port=$JETTY_PORT jetty.host=$JETTY_HOST \
    config=$CONFIG

elif [ "$ACTION" = "importTracks" ]; then
  if [ "$FILE" = "" ]; then
    echo "## file not given. try" 
    printUsage
  fi


  CONFIG="files/import.properties"
  CLASS="de.popularRoutes.gpx.ParseAndImportGPXTrack"
  
  exec "$JAVA" $JAVA_OPTS -cp "/home/vk/Dokumente/repositories/routenPlanung/trunk/popularRoutes/src/main/java/:$JAR" $CLASS \
    config=$CONFIG gpxPath=$FILE

fi


