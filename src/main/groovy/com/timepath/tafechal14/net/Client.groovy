package com.timepath.tafechal14.net
import com.jme3.app.SimpleApplication
import com.jme3.network.Client as Connection
import com.jme3.network.Message
import com.jme3.network.MessageListener
import com.jme3.network.Network
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
class Client extends SimpleApplication implements MessageListener<Connection> {

    static void main(String[] args) {
        SharedNetworking.instance
        new Client().start(JmeContext.Type.Headless);
    }

    Connection conn

    @Override
    void simpleInitApp() {
        conn = Network.connectToServer("127.0.0.1", SharedNetworking.PORT)
        conn.addMessageListener(this)
        conn.start()
        conn.send(new HelloMessage(hello: 'world'))
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
