# only deploy on linux
if  [ "$TRAVIS_OS_NAME" = "linux" ]
then
  if [ ! -z "$TRAVIS_TAG" ]
  then
      echo "on a tag -> $TRAVIS_TAG and therefore we will do nothing. Tagged releases are deployed to sonatype manually."
  else
      echo "not on a tag -> deploying to sonatype snapshot repo..."
      ./mvnw clean deploy --settings .travis/settings.xml -DskipTests=true -B -U
  fi
else
   echo "Ignoring deploy step since we are not on linux. (TRAVIS_OS_NAME=${TRAVIS_OS_NAME})"
fi
