package a7.tweakception.proxies;

public interface IProxy
{
    // Registers client side commands
    void registerClientCommands();

    // Registers server side commands
    void registerServerCommands();

    // Registers client side event handlers
    void registerClientEventHandlers();


    // Registers server side event handlers
    void registerServerEventHandlers();
}
