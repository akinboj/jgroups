#!/bin/bash
set -e
# NOTE: this file should have Unix (LF) EOL conversion performed on it to avoid: "env: can't execute 'bash ': No such file or directory"

echo "Starting Wildfly server installation"

apt-get update

echo "Installing openjdk-11"
apt-get install openjdk-11-jdk

echo "Set JAVA_HOME PATH"
export JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64
echo $JAVA_HOME
export PATH=$PATH:$JAVA_HOME/bin
echo $PATH
echo java -version

echo "Create a user and group for WildFly"
groupadd -r jboss
useradd -r -g jboss -d ${HOME}/wildfly -s /sbin/nologin jboss


WILDFLY_VERSION=20.0.1.Final
WILDFLY_SHA1=95366b4a0c8f2e6e74e3e4000a98371046c83eeb
JBOSS_HOME=${HOME}/wildfly
cd $HOME \
    && wget -O wildfly-$WILDFLY_VERSION.tar.gz https://download.jboss.org/wildfly/$WILDFLY_VERSION/wildfly-$WILDFLY_VERSION.tar.gz \
    && sha1sum wildfly-$WILDFLY_VERSION.tar.gz | grep $WILDFLY_SHA1 \
    && tar xf wildfly-$WILDFLY_VERSION.tar.gz \
    && mv $HOME/wildfly-$WILDFLY_VERSION $JBOSS_HOME \
    && rm wildfly-$WILDFLY_VERSION.tar.gz \
    && chown -R jboss:0 ${HOME} \
    && chmod -R g+rw ${HOME}
	
mkdir -p /etc/wildfly
cp ${HOME}/wildfly/docs/contrib/scripts/systemd/wildfly.conf /etc/wildfly/
cp ${HOME}/wildfly/docs/contrib/scripts/systemd/launch.sh ${HOME}/wildfly/bin/
sh -c 'chmod +x ${HOME}/wildfly/bin/*.sh'
cp ${HOME}/wildfly/docs/contrib/scripts/systemd/wildfly.service /etc/systemd/system/
systemctl daemon-reload
systemctl start wildfly
systemctl enable wildfly
echo "Allow traffic on port 8080"
ufw allow 8080/tcp
ufw allow 7600:7610/tcp

echo "Create a WildFly Administrator" 

# Declare some variables
WILDFLY_MANAGEMENT_USER=pegacorn
WILDFLY_MANAGEMENT_PASSWORD=jgroups!
JBOSS_HOME=${HOME}/wildfly
WILDFLY_LOG_LEVEL=INFO
wildfly_runner=( ${HOME}/wildfly/bin/standalone.sh )

if [ -n "$WILDFLY_MANAGEMENT_USER" ]; then
    #From https://hub.docker.com/r/jboss/keycloak/
    ${HOME}/wildfly/bin/add-user.sh -u "$WILDFLY_MANAGEMENT_USER" -p "$WILDFLY_MANAGEMENT_PASSWORD"
	#Also create a login for the wildfly admin console
    ${HOME}/wildfly/bin/add-user.sh "$WILDFLY_MANAGEMENT_USER" "$WILDFLY_MANAGEMENT_PASSWORD" --silent
fi

echo "Configure JGroups"
cp ${HOME}/wildfly/standalone/configuration/standalone-full-ha.xml ${HOME}/wildfly/standalone/configuration/standalone.xml
cp ${HOME}/jgroups/jgroups-clustering.cli ${HOME}/wildfly/bin
echo "Change directory to wildfly location"
cd ${HOME}/wildfly/bin
sh ${HOME}/wildfly/bin/jboss-cli.sh --file=jgroups-clustering.cli
rm -rf ${HOME}/wildfly/standalone/configuration/standalone_xml_history/current/*

echo "Enhancing log leves"
if [ -n "$WILDFLY_LOG_LEVEL" ] && [ "$WILDFLY_LOG_LEVEL" = 'DEBUG' ]; then
	sed '/INFO/{s//DEBUG/;:p;n;bp}' $JBOSS_HOME/standalone/configuration/standalone.xml
	sed -i 's+<logger category="sun.rmi"+<logger category="org.jboss.as.server.deployment"><level name="DEBUG"/></logger><logger category="sun.rmi"+' $JBOSS_HOME/standalone/configuration/standalone.xml
	sed -i 's+<logger category="sun.rmi"+<logger category="org.jboss.jandex"><level name="DEBUG"/></logger><logger category="sun.rmi"+' $JBOSS_HOME/standalone/configuration/standalone.xml
	sed -i "s+<level name=\"INFO\"/>+<level name=\"$WILDFLY_LOG_LEVEL\"/>+g" "$JBOSS_HOME/standalone/configuration/standalone.xml"
fi

echo "Copying deployment file"
cp ${HOME}/jgroups/jgroups-mock-ladon.war ${HOME}/wildfly/standalone/deployments

echo "Starting WildFly server"

sh $wildfly_runner