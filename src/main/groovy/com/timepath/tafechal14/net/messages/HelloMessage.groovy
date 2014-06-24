package com.timepath.tafechal14.net.messages

import com.jme3.network.AbstractMessage
import com.jme3.network.serializing.Serializable
import groovy.transform.CompileStatic
import groovy.transform.TypeChecked
import groovy.util.logging.Log

/**
 * @author TimePath
 */
@CompileStatic
@TypeChecked
@Log('LOG')
@Serializable
class HelloMessage extends AbstractMessage {
    String hello;
}
