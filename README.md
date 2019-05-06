# IntelliJ Plugin for generating classes that follow the Builder pattern

## Builder Generator for immutable POJO models.

What's the point of a builder pattern if it isn't immutable? If your model is immutable, what's the point of getter access?

Create a builder from your final fields. Add your @Immutable annotation of choice and make the class final. Done.

Alt-Enter ( "Show Intention Actions" ) -> Generate Builder. If the menu entry is missing, be sure the cursor is on a blank line within the class.
 
 Or invoke via right-click menu
 
#### ONLY SUPPORTS FINAL FIELDS
 
##### Overwrites on each invocation!. 

Be careful. Replacing this with 'update' is high priority, but also quite complex.

### Features   
- Add Jackson Annotations to your builders. Parse from JSON straight into an immutable POJO.
- Auto generate toJson and fromJson methods that use Jackson to marshal your object. 
- Generates Copy constructor and example usage code comment on builder constructor.
- Optionally applies Hibernate Validations, with the use of JavaSpork project ( https://github.com/ggranum/java-spork )

Adds a 'Generate Builder' option to the actions menu for a class.

## Use
Create a simple class with a handful of final fields and no constructor to demonstrate, then hit Alt-Enter ( "Show Intention Actions" ) -> Generate Builder.
 
If the menu entry is missing, be sure the cursor is on a blank line within the class.

### Add Jackson Annotations

### Generate toJson and fromJson methods

Enabling this option will create two methods:

```
public String toJson(ObjectMapper mapper);
public static YourClass fromJson(ObjectMapper mapper, String json);
```

We require Jackson's mapper class to be provided so that you may create one and reuse it, perhaps by injecting it into your calling classes via Guice, or however you so choose. 

The ObjectMapper is expensive to create, and thread safe. So, as per Jackson Databind recommendations, we don't want to create a new one every time we marshal or unmarshal an object.    

## About


## Issues / Todo
 1) Primitives like integers are getting wrapped and causing NPEs when unwrapped, regardless of @NotNull/Nullable
 1) Optional ignores the @Nullable/NotNull flags and always unwraps  
 1) Optional ignores the @Nullable/NotNull flags and always unwraps  
 
