package ss;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;

public class SkinSwapPlugin extends JavaPlugin {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public SkinSwapPlugin(JavaPluginInit init) {
        super(init);
    }

    /*
        Register plugin classes
     */
    @Override
    protected void setup() {
        this.getCommandRegistry().registerCommand(new SSCommand());
    }
}
