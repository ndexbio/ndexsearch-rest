# -*- mode: ruby -*-
# vi: set ft=ruby :

# All Vagrant configuration is done below. The "2" in Vagrant.configure
# configures the configuration version (we support older styles for
# backwards compatibility). Please don't change it unless you know what
# you're doing.
Vagrant.configure(2) do |config|
  config.vm.box = "rockylinux/8"
  config.vm.box_version = "4.0.0"
  config.vm.provision :shell, path: "bootstrap.sh"
  config.ssh.forward_x11 = true
  config.vm.network "forwarded_port", guest: 8287, host: 8287
  config.vm.network "forwarded_port", guest: 8290, host: 8290
  config.vm.network "forwarded_port", guest: 8291, host: 8291
  config.vm.network "forwarded_port", guest: 8292, host: 8292
  config.vm.network "forwarded_port", guest: 8293, host: 8293
  config.vm.provider "virtualbox" do |vb|
    vb.memory = "2048"
    vb.cpus = "1"
  end
end
