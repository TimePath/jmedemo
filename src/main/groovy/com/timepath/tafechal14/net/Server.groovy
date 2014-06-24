package com.timepath.tafechal14.net

import com.jme3.app.SimpleApplication
import com.jme3.network.HostedConnection as Connection
import com.jme3.network.Message
import com.jme3.network.MessageListener
import com.jme3.network.Network
import com.jme3.network.Server as JmeServer
import com.jme3.system.JmeContext
import com.timepath.tafechal14.net.messages.HelloMessage
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import groovy.transform.TypeChecked
import groovy.util.logging.Log

/**
 * @author TimePath
 */
@CompileStatic
@TypeChecked
@Log('LOG')
class Server extends SimpleApplication implements MessageListener<Connection> {

    static void main(String[] args) {
        SharedNetworking.instance
        new Server().start(JmeContext.Type.Headless);
    }

    JmeServer server

    @Override
    void simpleInitApp() {
        server = Network.createServer(SharedNetworking.PORT)
        server.addMessageListener(this);
        server.start()
    }

    @CompileDynamic
    @Override
    void messageReceived(final Connection source, final Message m) {
        messageReceived(source, m)
    }

    void messageReceived(final Connection source, final HelloMessage m) {
        println m.hello
    }
}
