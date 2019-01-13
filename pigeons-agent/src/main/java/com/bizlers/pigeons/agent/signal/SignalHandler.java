package com.bizlers.pigeons.agent.signal;

import java.util.Observable;

public class SignalHandler extends Observable implements sun.misc.SignalHandler {
	public void handleSignal(final String signalName)
			throws IllegalArgumentException {
		try {
			sun.misc.Signal.handle(new sun.misc.Signal(signalName), this);
		} catch (IllegalArgumentException x) {
			throw x;
		} catch (Throwable x) {
			throw new IllegalArgumentException("Signal unsupported: "
					+ signalName, x);
		}
	}

	public void handle(final sun.misc.Signal signal) {
		setChanged();
		notifyObservers(signal);
	}
}