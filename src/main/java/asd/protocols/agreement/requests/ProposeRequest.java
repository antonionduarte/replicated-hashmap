package asd.protocols.agreement.requests;

import org.apache.commons.codec.binary.Hex;

import asd.protocols.agreement.Agreement;
import pt.unl.fct.di.novasys.babel.generic.ProtoRequest;

import java.util.UUID;

public class ProposeRequest extends ProtoRequest {

	public static final short ID = Agreement.ID + 1;

	private final int instance;
	private final UUID opId;
	private final byte[] operation;

	public ProposeRequest(int instance, UUID opId, byte[] operation) {
		super(ID);
		this.instance = instance;
		this.opId = opId;
		this.operation = operation;
	}

	public int getInstance() {
		return instance;
	}

	public byte[] getOperation() {
		return operation;
	}

	public UUID getOpId() {
		return opId;
	}

	@Override
	public String toString() {
		return "ProposeRequest{" +
				"instance=" + instance +
				", opId=" + opId +
				", operation=" + Hex.encodeHexString(operation) +
				'}';
	}
}
