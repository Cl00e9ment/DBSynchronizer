Release 1.5.1 (security update)

    changelog :
    
    - Now the client can't modify data without the server permission that can use allowAccessToAll(), allowAccessTo(), allowAccessToAllExcept(), forbidAccessToAll(), forbidAccessTo() and forbidAccessToAllExcept().
    - Now the server verify all packets content received from clients for avoid that a malicious client sends a bad packet that can crash the server.
    - Now the clients can't add database instances but the server must use DatabaseGetter.registerInstance().
    - Now the method copy() in DBFolder copies also sub-folders.
    
    tech changelog :
    
    - All (x.class == X.getClass()) has been replaced by (x instanceof X).
    - Better respect of the OOP rules in DBSynchronizer.class

Release 1.5.0.2 (bug fix)

    changelog :
    
    - Fixed the bug that create a non-synchronized DBFolder when the method getDBFolder() is called with a key that isn't yet stored.

Release 1.5.0.1 (bug fix)

    changelog :
    
    - bug fix : Fixed the bug that throws a NullPointerException when you try to get the database instance from a client in multiplayer.
    - mod logo changed

Release 1.5.0 (first release)

    changelog :
    
    - First release for Minecraft 1.12