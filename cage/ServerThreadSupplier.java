package net.xincraft.systems.match.cage;

public interface ServerThreadSupplier<R> extends ServerThreadWorker {

    /**
     * The result of the computation
     *
     * @return the resulting object
     * @since 1.0.0-SNAPSHOT
     */
    R getResult();

}
