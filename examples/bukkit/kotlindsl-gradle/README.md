# kotlin-dsl Gradle

A simple example showcasing creating a command using the Kotlin DSL for the CommandAPI!

Key points:

- You do not need to use the `.register()` method
- You do not need to initialise any arguments.
- Add the `commandapi-kotlin-bukkit` dependency to your project:

  ```kotlin
  compileOnly("dev.jorel:commandapi-bukkit-kotlin:10.0.1")
  ```

- The Kotlin DSL must not be shaded into your plugin
