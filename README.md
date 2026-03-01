# Hytale Plugin Template

A template for Hytale java plugins. Created by [Up](https://github.com/UpcraftLP), and slightly modified by Kaupenjoe. 

### Configuring the Template
If you for example installed the game in a non-standard location, you will need to tell the project about that.
The recommended way is to create a file at `%USERPROFILE%/.gradle/gradle.properties` to set these properties globally.

```properties
# Set a custom game install location
hytale.install_dir=path/to/Hytale

# Speed up the decompilation process significantly, by only including the core hytale packages.
# Recommended if decompiling the game takes a very long time on your PC.
hytale.decompile_partial=true
```


# Add HytaleServer.jar

1. gradle decompileServer
2. Go to HytaleServer.zip and add that to porject
3. Settings → Project Structure → Libraries

# Start the server 

1. runServer
2. /auth login device 
3. /auth persistence Encrypted

# Use Asset Editor to edit items
When server is stopped, BUILD SYNC WITH SOURCE CODE!
You can still run syncAssets

