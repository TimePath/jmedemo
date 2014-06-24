package com.timepath.tafechal14.net

import com.jme3.network.serializing.Serializer
import com.timepath.tafechal14.net.messages.HelloMessage
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
        Serializer.registerClass(HelloMessage.class);
    }

    static final int PORT = 6143

}
