#!/usr/bin/env bash

# install needed packages (maven is needed to build REST service)
dnf install -y java-11-openjdk java-11-openjdk-devel wget

# install maven
pushd /usr/local/
wget https://downloads.apache.org/maven/maven-3/3.6.3/binaries/apache-maven-3.6.3-bin.tar.gz
tar -xvf apache-maven-3.6.3-bin.tar.gz
mv apache-maven-3.6.3 maven
rm -f apache-maven-3.6.3-bin.tar.gz
popd
pushd /etc/profile.d
echo "# Configuration of Apache Maven Environment Variables" > maven.sh
echo "export M2_HOME=/usr/local/maven" >> maven.sh
echo "export PATH=\${M2_HOME}/bin:\$PATH" >> maven.sh
source /etc/profile.d/maven.sh

# create ndex user
adduser ndex

# create log directory
mkdir -p /var/log/ndexsearch-rest
chown ndex.ndex /var/log/ndexsearch-rest

# create tasks directory
mkdir -p /opt/ndex/services/ndexsearch-rest/tasks
ln -s /var/log/ndexsearch-rest /opt/ndex/services/ndexsearch-rest/logs
chown -R ndex.ndex /opt/ndex

# copy REST jar
pushd /vagrant/
mvn install -Dmaven.test.skip=true
JAR_PATH=`/bin/ls target/ndexsearch*with*dependencies.jar`
JAR_FILE=`basename $JAR_PATH`
cp $JAR_PATH /opt/ndex/services/ndexsearch-rest/.

if [ $? != 0 ] ; then
	echo "ERROR: Something messed up building the jar"
    echo ""
    exit 1
fi

popd
pushd /opt/ndex/services/ndexsearch-rest
ln -s /opt/ndex/services/ndexsearch-rest/${JAR_FILE} ndexsearch-rest.jar
popd

# copy source configuration from systemd/ 
cp /vagrant/systemdservice/source.configurations.json /opt/ndex/services/ndexsearch-rest/.

mkdir /etc/ndexsearch-rest
# copy configuration from systemd/ 
cp /vagrant/systemdservice/search.conf /etc/ndexsearch-rest/.

# copy systemd service
cp /vagrant/systemdservice/ndexsearch-rest.service /lib/systemd/system

# enable communitydetection service
systemctl enable ndexsearch-rest

echo "Starting NDEx Integrated Search service"
# start service
systemctl start ndexsearch-rest

# sleep a few seconds
sleep 3

# output status
systemctl status ndexsearch-rest

echo ""
echo "NDEx Integrated Search service configured (IQuery)"
echo "Configuration file: /etc/ndexsearch-rest/search.conf"
echo "Log files: /var/log/ndexsearch-rest"
echo "Task directory: /opt/ndex/ndexsearch-rest/tasks"
echo "To stop service: systemctl stop ndexsearch-rest"
echo "To start service: systemctl start ndexsearch-rest"  
echo ""
echo "Visit http://localhost:8290/v1/status for service status endpoint"
echo ""
echo "To update the service:"
echo " 1) Create new jar via mvn install from host computer"
echo " 2) Connect to VM via vagrant ssh and become root (sudo -u root /bin/bash)"
echo " 3) Copy /vagrant/target/ndexsearch*jar to /opt/ndex/services/ndexsearch-rest"
echo " 4) Update /opt/ndex/services/ndexsearch-rest.jar symlink if needed"
echo " 5) Run systemctl restart ndexsearch-rest"
echo ""
echo "Have a nice day!!!"
echo ""

