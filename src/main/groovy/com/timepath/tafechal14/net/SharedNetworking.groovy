package com.timepath.tafechal14.net

import com.jme3.network.serializing.Serializer
import com.timepath.tafechal14.net.messages.EntityUpdate
import com.timepath.tafechal14.net.messages.HelloMessage
import com.timepath.tafechal14.net.messages.PlayerUpdate
import com.timepath.tafechal14.net.messages.SeedMessage
import groovy.transform.CompileStatic
import groovy.transform.TypeChecked
import groovy.util.logging.Log

/**
 * @author TimePath
 */
@CompileStatic
@TypeChecked
@Log('LOG')
@Singleton
class SharedNetworking {

    {
        Serializer.registerClass(EntityUpdate.class);
        Serializer.registerClass(HelloMessage.class);
        Serializer.registerClass(PlayerUpdate.class);
        Serializer.registerClass(SeedMessage.class);
    }

    static final int PORT = 6143

}
