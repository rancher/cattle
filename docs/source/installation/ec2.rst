AWS CloudFormation Installation
===============================

To get a sample installation up and running real fast you can use CloudFormation, just follow one of the links below.

Regions
*******

* `us-east-1 <https://console.aws.amazon.com/cloudformation/home?region=us-east-1#cstack=sn%7ECattle-Demo%7Cturl%7Ehttps://s3-us-west-1.amazonaws.com/0a3bab1d-805b-45af-b33b-81ea3701d56b/template.json>`_
* `us-west-1 <https://console.aws.amazon.com/cloudformation/home?region=us-west-1#cstack=sn%7ECattle-Demo%7Cturl%7Ehttps://s3-us-west-1.amazonaws.com/0a3bab1d-805b-45af-b33b-81ea3701d56b/template.json>`_
* `us-west-2 <https://console.aws.amazon.com/cloudformation/home?region=us-west-2#cstack=sn%7ECattle-Demo%7Cturl%7Ehttps://s3-us-west-1.amazonaws.com/0a3bab1d-805b-45af-b33b-81ea3701d56b/template.json>`_
* `eu-west-1 <https://console.aws.amazon.com/cloudformation/home?region=eu-west-1#cstack=sn%7ECattle-Demo%7Cturl%7Ehttps://s3-us-west-1.amazonaws.com/0a3bab1d-805b-45af-b33b-81ea3701d56b/template.json>`_
* `sa-east-1 <https://console.aws.amazon.com/cloudformation/home?region=sa-east-1#cstack=sn%7ECattle-Demo%7Cturl%7Ehttps://s3-us-west-1.amazonaws.com/0a3bab1d-805b-45af-b33b-81ea3701d56b/template.json>`_
* `ap-southeast-1 <https://console.aws.amazon.com/cloudformation/home?region=ap-southeast-1#cstack=sn%7ECattle-Demo%7Cturl%7Ehttps://s3-us-west-1.amazonaws.com/0a3bab1d-805b-45af-b33b-81ea3701d56b/template.json>`_
* `ap-southeast-2 <https://console.aws.amazon.com/cloudformation/home?region=ap-southeast-2#cstack=sn%7ECattle-Demo%7Cturl%7Ehttps://s3-us-west-1.amazonaws.com/0a3bab1d-805b-45af-b33b-81ea3701d56b/template.json>`_
* `ap-northeast-1 <https://console.aws.amazon.com/cloudformation/home?region=ap-northeast-1#cstack=sn%7ECattle-Demo%7Cturl%7Ehttps://s3-us-west-1.amazonaws.com/0a3bab1d-805b-45af-b33b-81ea3701d56b/template.json>`_

.. WARNING:: Make sure you know your SSH keyname for the appropriate region and enter it correctly.

Information
***********

This CloudFormation template will create a single EC2 instance that has Cattle installed and it will register itself as a docker and libvirt hypervisor.  Once the installation is done, which could take 20 minutes, the UI and API will be accessible at http://ec2-XX-XXX-XXX-XX.us-west-1.compute.amazonaws.com:8080/v1.


Logging in
**********

You can log into the server by running ssh similar to below

``ssh -l ubuntu -i mykey.pem ec2-XX-XXX-XXX-XX.us-west-1.compute.amazonaws.com``

The installation log is at :file:`/var/log/cattle-install.log`.  If everything ran successfully you should be able to run ``cattle list-host`` and see two hosts registered, one for Docker and one for Libvirt.
