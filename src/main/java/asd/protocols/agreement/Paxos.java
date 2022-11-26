package asd.protocols.agreement;

import pt.unl.fct.di.novasys.babel.core.GenericProtocol;
import pt.unl.fct.di.novasys.babel.exceptions.HandlerRegistrationException;

import java.io.IOException;
import java.util.Properties;

public class Paxos extends GenericProtocol implements Agreement {

    public Paxos() {
        super("Paxos", PROTOCOL_ID);
    }

    @Override
    public void init(Properties properties) throws HandlerRegistrationException, IOException {

    }
}
