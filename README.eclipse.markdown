# Work With Eclipse

## Moxie Proxy

To use Eclipse as a development environment for Moxie Proxy, do this:

* Execute an ANT build:`ant`
* Extract some files from proxy/build/target/moxie-proxy-0.8.3-SNAPSHOT.zip:
  `unzip -d proxy  proxy/build/target/moxie-proxy-0.8.3-SNAPSHOT.zip "ext/*"`
* Within Eclipse, do
    * File - Import...
    * Existing Projects into Workspace - Next
    * Select root directory - Browse
    * {Select the moxie folder} -> Projects: will show Moxie
    * Finish
