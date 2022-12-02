package asd.protocols.statemachine.requests;

import asd.AsdUtils;
import pt.unl.fct.di.novasys.babel.generic.ProtoRequest;

import java.util.UUID;

public class OrderRequest extends ProtoRequest {

	public static final short REQUEST_ID = 201;

	private final UUID operationId;
	private final byte[] operation;

	public OrderRequest(UUID opId, byte[] operation) {
		super(REQUEST_ID);
		this.operationId = opId;
		this.operation = operation;
	}

	public byte[] getOperation() {
		return operation;
	}

	public UUID getOperationId() {
		return operationId;
	}

	@Override
	public String toString() {
		return "OrderRequest [operationId=" + operationId + ", operation=" + AsdUtils.sha256Hex(this.operation) + "]";
	}

}
