package asd.protocols.agreement.requests;

import asd.protocols.agreement.Agreement;
import pt.unl.fct.di.novasys.babel.generic.ProtoRequest;
import pt.unl.fct.di.novasys.network.data.Host;

public class RemoveReplicaRequest extends ProtoRequest {

	public static final short ID = Agreement.ID + 2;

	private final int instance;
	private final Host replica;

	public RemoveReplicaRequest(int instance, Host replica) {
		super(ID);
		this.instance = instance;
		this.replica = replica;
	}

	public int getInstance() {
		return instance;
	}

	public Host getReplica() {
		return replica;
	}

	@Override
	public String toString() {
		return "RemoveReplicaRequest{" +
				"instance=" + instance +
				", replica=" + replica +
				'}';
	}
}
