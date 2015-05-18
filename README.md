+ IntelliJ Plugin for generating classes that follow the Builder pattern 

Adds a 'Generate Builder' option to the actions menu for a class.

Create a simple class with a handful of final fields and no constructor to demonstrate.

WARNING: This plugin does not 'update' the builders it generates when it is re-run. It overwrites them - and any changes you may have made.

Optionally adds validation using validations API, and JSON marshalling hints using Jackson 2 (FasterXML). Validation requires a dependency on the validations
library which I'll eventually publish and link to....