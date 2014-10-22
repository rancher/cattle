# Development Environment

## Quick command line tools

The following commands will let you run the cattle build with no other setup than a Linux box.  This may even work with boot2docker on a Mac.

`make` - Builds the full cattle

`make test` -  Runs the full test suite

`make run` - Runs Cattle in a Docker container exposing port 8080 locally

All of the those commands are not particularly fast and the very first invocation will take a very long time.  Subsequent invocations should be faster.

## Full Development Environment

The following instructions are for setting up a development environment on Linux. **After cloning the git repo you should run `make` at least one to ensure all of the dependencies are properly built.**

### Eclipse

Eclipse is the preferred IDE for Java development for Cattle.  Download the latest Eclipse IDE for Java Developers (currently 4.4 Luna at the time of writing).  Install the following plugins from the Eclipse Marketplace (`About->Eclipse Marketplace`)

* m2eclipse - This should be included by default in Eclipse now
* Spring Tool Suite - Only **Spring IDE Core** is required
* Vrapper - If you want VIM key bindings

#### Setup Eclipse

##### Code Formatting

Indentation -> Tab Policy: Spaces Only

Line Wrapping -> Maximum line width: 120

##### Other Preferences

Java -> Code Style -> Organize Imports -> Number of static imports needed for .*: 1

General -> Editors -> Text Editors -> Displayed tab width: 4

Java -> Editor -> Save Actions: As below

```
Remove unused imports
Add missing '@Override' annotations
Add missing '@Override' annotations to implementations of interface methods
Add missing '@Deprecated' annotations
Remove unnecessary casts
Remove trailing white spaces on all lines
Correct indentation
```

#### Import Projects

Go to `File->Import->Existing Maven Projects` and select the cattle root folder.

#### Launching Cattle

Copy `tools/eclipse/Cattle.launch` to `code/packaging/app`.   Close the `cattle` project in Eclipse as it is not needed and will cause there to be two of everything in your workspace.  Go to `Run->Debug Configurations...` and then select `Java Application->Cattle`.  The logs and H2 database will be storage in the `cattle-app` project at `code/packaging/app`

### Python

Virtualenv and virtualenvwrappers are highly preferred.

#### Setup new virtualenv

```bash
cd tests/integration/
mkvirtualenv cattle
pip install -r requirements.txt
pip install -r test-requirements.txt
```

#### Running Tests

```bash
cd tests/integration
workon cattle
# Run all tests.  Ensure Cattle is running, listening on port 8080
py.test
# Or specific tests
py.test -k test_create_container cattletest/core/test_ssh_key.py
```

#### PyCharm

PyCharm community edition is very good.  Download PyCharm and go to `File->Open` and select the cattle folder.  Once loaded go to `File->Settings` and change `Project Interpreter` to your virtualenv.  Additionally set `Python Integrated Tools -> Default test runner` to py.test.

You can install IdeaVim if you want VIM key bindings.

You can select and test file and right click and execute it as a py.test.

### Database

By default Cattle will run with a H2 database.  For development is it much nicer to run with MySQL so that you can more easily run tools against it.

#### Create user and database

Run `cattle/resources/content/db/mysql/create_db_and_user_dev.sql` to setup the default user and database

    mysql -u root < create_db_and_user_dev.sql

#### Switch Cattle to use MySQL

In your Eclipse launch configuration just add the follow in the VM arguments

```
-Ddb.cattle.database=mysql
```

## Idempotency

By default Cattle when built from source will run with idempotency checks on.  What this means is that it will rerun the same piece of code three times.  Additionally, whenever a database commit is about to happen it will abort and restart.  This is to help building crash consistent and idempotent code.

Unfortunately this makes things run quite slow.  To disable the idempotent checks set the following in your Eclipse launch configuration.

    -Didempotent.checks=false
