.. _simple_handler:

Simple External Process Handler
===============================

To add logic to an orchestration process you must implement a Process Handler.  You can write Java code and package it with Cattle, or the more desirable approach is to write the logic in the language of your choice and run that externally.  This example uses bash scripting to integrate with the "instance.start" process.  Refer to the inline comments in https://github.com/rancherio/cattle/blob/master/docs/examples/handler-bash/simple_handler.sh
