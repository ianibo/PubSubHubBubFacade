#!/bin/bash
export SDKMAN_DIR="/home/ibbo/.sdkman"
[[ -s "/home/ibbo/.sdkman/bin/sdkman-init.sh" ]] && source "/home/ibbo/.sdkman/bin/sdkman-init.sh"

export FF_VER=`grep appVersion ./feedFacade/gradle.properties | cut -f2 -d=`

echo Release FeedFacade $FF_VER

sdk use grails 4.0.3
sdk use java 11.0.6.j9-adpt
cd feedFacade
grails clean
grails prod war
cp build/libs/feedFacade-*.war ../docker/feedFacade.war
cd ../docker
docker login

if [[ "$FF_VER" == *-SNAPSHOT ]]
then
  echo  SNAPSHOT release - only tagging :v$FF_VER and :latest
  docker build -t semweb/caphub_feedfacade:v$FF_VER -t semweb/caphub_feedfacade:latest .
  docker push semweb/caphub_feedfacade:v$FF_VER
  docker push semweb/caphub_feedfacade:latest
else
  echo  Standard Release
  docker build -t semweb/caphub_feedfacade:v$FF_VER -t semweb/caphub_feedfacade:v2.0 -t semweb/caphub_feedfacade:v2 -t semweb/caphub_feedfacade:latest .
  docker push semweb/caphub_feedfacade:v$FF_VER
  docker push semweb/caphub_feedfacade:v2.1
  docker push semweb/caphub_feedfacade:v2
  docker push semweb/caphub_feedfacade:latest
fi

echo Completed release of FeedFacade $FF_VER
echo to deploy
echo ssh cap
echo docker service update --image semweb/caphub_feedfacade:v$FF_VER fah_feedFacade
