package com.github.jborza.camel.component.smbj;

import org.apache.camel.AsyncCallback;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.camel.component.file.FileComponent;
import org.apache.camel.component.file.GenericFile;
import org.apache.camel.component.file.GenericFileConsumer;
import org.apache.camel.component.file.GenericFileEndpoint;
import org.apache.camel.component.file.GenericFileOnCompletion;
import org.apache.camel.component.file.GenericFileOperationFailedException;
import org.apache.camel.component.file.GenericFileOperations;
import org.apache.camel.component.file.GenericFileProcessStrategy;

/**
 * #FUSE - porting of the GenericFileConsumer version of camel v2.22.1
 */
public abstract class GenericFileConsumer_v2_22_1<T> extends GenericFileConsumer<T> {

    public GenericFileConsumer_v2_22_1(GenericFileEndpoint<T> endpoint, Processor processor, GenericFileOperations<T> operations, GenericFileProcessStrategy<T> processStrategy) {
        super(endpoint, processor, operations);
    }

    protected boolean processExchange(final Exchange exchange) {
        GenericFile<T> file = getExchangeFileProperty(exchange);
        log.trace("Processing file: {}", file);

        // must extract the absolute name before the begin strategy as the file could potentially be pre moved
        // and then the file name would be changed
        String absoluteFileName = file.getAbsoluteFilePath();

        // check if we can begin processing the file
        final GenericFileProcessStrategy<T> processStrategy = endpoint.getGenericFileProcessStrategy();

        Exception beginCause = null;
        boolean begin = false;
        try {
            begin = processStrategy.begin(operations, endpoint, exchange, file);
        } catch (Exception e) {
            beginCause = e;
        }

        if (!begin) {
            // no something was wrong, so we need to abort and remove the file from the in progress list
            Exception abortCause = null;
            log.debug("{} cannot begin processing file: {}", endpoint, file);
            try {
                // abort
                processStrategy.abort(operations, endpoint, exchange, file);
            } catch (Exception e) {
                abortCause = e;
            } finally {
                // begin returned false, so remove file from the in progress list as its no longer in progress
                endpoint.getInProgressRepository().remove(absoluteFileName);
            }
            if (beginCause != null) {
                String msg = endpoint + " cannot begin processing file: " + file + " due to: " + beginCause.getMessage();
                handleException(msg, beginCause);
            }
            if (abortCause != null) {
                String msg2 = endpoint + " cannot abort processing file: " + file + " due to: " + abortCause.getMessage();
                handleException(msg2, abortCause);
            }
            return false;
        }

        // must use file from exchange as it can be updated due the
        // preMoveNamePrefix/preMoveNamePostfix options
        final GenericFile<T> target = getExchangeFileProperty(exchange);

        // we can begin processing the file so update file headers on the Camel message
        // in case it took some time to acquire read lock, and file size/timestamp has been updated since etc
        updateFileHeaders(target, exchange.getIn());

        // must use full name when downloading so we have the correct path
        final String name = target.getAbsoluteFilePath();
        try {

            if (isRetrieveFile()) {
                // retrieve the file using the stream
                log.trace("Retrieving file: {} from: {}", name, endpoint);

                // retrieve the file and check it was a success
                boolean retrieved;
                Exception cause = null;
                try {
                    retrieved = operations.retrieveFile(name, exchange);
                } catch (Exception e) {
                    retrieved = false;
                    cause = e;
                }

                if (!retrieved) {
                    if (ignoreCannotRetrieveFile(name, exchange, cause)) {
                        log.trace("Cannot retrieve file {} maybe it does not exists. Ignoring.", name);
                        // remove file from the in progress list as we could not retrieve it, but should ignore
                        endpoint.getInProgressRepository().remove(absoluteFileName);
                        return false;
                    } else {
                        // throw exception to handle the problem with retrieving the file
                        // then if the method return false or throws an exception is handled the same in here
                        // as in both cases an exception is being thrown
                        if (cause != null && cause instanceof GenericFileOperationFailedException) {
                            throw cause;
                        } else {
                            throw new GenericFileOperationFailedException("Cannot retrieve file: " + file + " from: " + endpoint, cause);
                        }
                    }
                }

                log.trace("Retrieved file: {} from: {}", name, endpoint);
            } else {
                log.trace("Skipped retrieval of file: {} from: {}", name, endpoint);
                exchange.getIn().setBody(null);
            }

            // register on completion callback that does the completion strategies
            // (for instance to move the file after we have processed it)
            exchange.addOnCompletion(new GenericFileOnCompletion<T>(endpoint, operations, target, absoluteFileName));

            log.debug("About to process file: {} using exchange: {}", target, exchange);

            if (endpoint.isSynchronous()) {
                // process synchronously
                getProcessor().process(exchange);
            } else {
                // process the exchange using the async consumer to support async routing engine
                // which can be supported by this file consumer as all the done work is
                // provided in the GenericFileOnCompletion
                getAsyncProcessor().process(exchange, new AsyncCallback() {
                    public void done(boolean doneSync) {
                        // noop
                        if (log.isTraceEnabled()) {
                            log.trace("Done processing file: {} {}", target, doneSync ? "synchronously" : "asynchronously");
                        }
                    }
                });
            }

        } catch (Exception e) {
            // remove file from the in progress list due to failure
            // (cannot be in finally block due to GenericFileOnCompletion will remove it
            // from in progress when it takes over and processes the file, which may happen
            // by another thread at a later time. So its only safe to remove it if there was an exception)
            endpoint.getInProgressRepository().remove(absoluteFileName);

            String msg = "Error processing file " + file + " due to " + e.getMessage();
            handleException(msg, e);
        }

        return true;
    }

    protected abstract void updateFileHeaders(GenericFile<T> file, Message message);

    @SuppressWarnings("unchecked")
    private GenericFile<T> getExchangeFileProperty(Exchange exchange) {
        return (GenericFile<T>) exchange.getProperty(FileComponent.FILE_EXCHANGE_FILE);
    }

}
