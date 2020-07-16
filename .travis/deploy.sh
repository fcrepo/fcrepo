if [ ! -z "$TRAVIS_TAG" ]
then
    echo "on a tag -> $TRAVIS_TAG and therefore we will do nothing. Tagged releases are deployed to sonatype and dockerhub manually."
else
    echo "not on a tag -> deploying to dockerhub and sonatype..."
    FCREPO_VERSION=$(./mvnw org.apache.maven.plugins:maven-help-plugin:3.1.0:evaluate -Dexpression=project.version -q -DforceStdout)
    
    echo "clone fcrepo4-labs/fcrepo-docker repository"
    git clone https://github.com/fcrepo4-labs/fcrepo-docker.git
    echo "building docker container"
    cd fcrepo-docker
    ./build.sh ../fcrepo-webapp/target/fcrepo-webapp-${FCREPO_VERSION}.war
    echo "push image to dockerhub"
    ./push.sh latest ${FCREPO_VERSION}
    cd ..

    echo "deploying to sonatype snapshot repo..."
    ./mvnw clean deploy --settings .travis/settings.xml -DskipTests=true -B -U
fi
