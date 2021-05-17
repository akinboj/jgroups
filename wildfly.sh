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
groupadd -r wildfly
useradd -r -g wildfly -d /opt/wildfly -s /sbin/nologin wildfly

Version_Number=20.0.1.Final
wget https://download.jboss.org/wildfly/$Version_Number/wildfly-$Version_Number.tar.gz -P /tmp
tar xf /tmp/wildfly-$Version_Number.tar.gz -C /opt/
ln -s /opt/wildfly-$Version_Number /opt/wildfly
chown -RH wildfly: /opt/wildfly
mkdir -p /etc/wildfly
cp /opt/wildfly/docs/contrib/scripts/systemd/wildfly.conf /etc/wildfly/
cp /opt/wildfly/docs/contrib/scripts/systemd/launch.sh /opt/wildfly/bin/
sh -c 'chmod +x /opt/wildfly/bin/*.sh'
cp /opt/wildfly/docs/contrib/scripts/systemd/wildfly.service /etc/systemd/system/
systemctl daemon-reload
systemctl start wildfly
systemctl enable wildfly
echo "Allow traffic on port 8080"
ufw allow 8080/tcp

echo "Create a WildFly Administrator" 

# Declare some variables
WILDFLY_MANAGEMENT_USER=pegacorn
WILDFLY_MANAGEMENT_PASSWORD=jgroups!
JBOSS_HOME=/opt/wildfly
WILDFLY_LOG_LEVEL=DEBUG
wildfly_runner=( /opt/wildfly/bin/standalone.sh -b 0.0.0.0 )

if [ -n "$WILDFLY_MANAGEMENT_USER" ]; then
    #From https://hub.docker.com/r/jboss/keycloak/
    /opt/wildfly/bin/add-user.sh -u "$WILDFLY_MANAGEMENT_USER" -p "$WILDFLY_MANAGEMENT_PASSWORD"
	#Also create a login for the wildfly admin console
    /opt/wildfly/bin/add-user.sh "$WILDFLY_MANAGEMENT_USER" "$WILDFLY_MANAGEMENT_PASSWORD" --silent
fi

echo "Configure JGroups"
cp /opt/wildfly/standalone/configuration/standalone-ha.xml /opt/wildfly/standalone/configuration/standalone.xml
cp jgroups-clustering.cli /opt/wildfly/bin
sh /opt/wildfly/bin/jboss-cli.sh --file=jgroups-clustering.cli
rm -rf /opt/wildfly/standalone/configuration/standalone_xml_history/current/*

echo "Enhancing log leves"
if [ -n "$WILDFLY_LOG_LEVEL" ] && [ "$WILDFLY_LOG_LEVEL" = 'DEBUG' ]; then
	sed '/INFO/{s//DEBUG/;:p;n;bp}' $JBOSS_HOME/standalone/configuration/standalone.xml
	sed -i 's+<logger category="sun.rmi"+<logger category="org.jboss.as.server.deployment"><level name="DEBUG"/></logger><logger category="sun.rmi"+' $JBOSS_HOME/standalone/configuration/standalone.xml
	sed -i 's+<logger category="sun.rmi"+<logger category="org.jboss.jandex"><level name="DEBUG"/></logger><logger category="sun.rmi"+' $JBOSS_HOME/standalone/configuration/standalone.xml
	sed -i 's+<level name="INFO"/>+<level name="DEBUG"/>+g' $JBOSS_HOME/standalone/configuration/standalone.xml
fi

echo "Starting server"

sh $wildfly_runner