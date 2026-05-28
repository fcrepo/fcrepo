/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.kernel.api.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Test class for various exceptions in the kernel API.
 *
 * @author whikloj
 */
public class KernelApiExceptionTest {


    private static final List<ExceptionTestInfo> EXCEPTIONS = List.of(
            new ExceptionTestInfo(AccessDeniedException.class, "Access denied", ConstructorType.STRING_THROWABLE),
            new ExceptionTestInfo(ACLAuthorizationConstraintViolationException.class,
                    "ACL authorization constraint violation", ConstructorType.STRING),
            new ExceptionTestInfo(CannotCreateMementoException.class, "Cannot create memento", ConstructorType.STRING),
            new ExceptionTestInfo(CannotCreateResourceException.class, "Cannot create resource",
                    ConstructorType.STRING),
            new ExceptionTestInfo(ConstraintViolationException.class, "Constraint violation", ConstructorType.BOTH),
            new ExceptionTestInfo(ExternalContentAccessException.class, "External content access exception",
                    ConstructorType.STRING_THROWABLE),
            new ExceptionTestInfo(ExternalMessageBodyException.class, "External message body exception",
                    ConstructorType.BOTH),
            new ExceptionTestInfo(GhostNodeException.class, "Ghost node exception", ConstructorType.STRING),
            new ExceptionTestInfo(InteractionModelViolationException.class, "Interaction model violation",
                    ConstructorType.STRING),
            new ExceptionTestInfo(InvalidChecksumException.class, "Invalid checksum", ConstructorType.STRING),
            new ExceptionTestInfo(InvalidMementoPathException.class, "Invalid memento path", ConstructorType.BOTH),
            new ExceptionTestInfo(InvalidResourceIdentifierException.class, "Invalid resource", ConstructorType.BOTH),
            new ExceptionTestInfo(ItemNotFoundException.class, "Item not found", ConstructorType.BOTH),
            new ExceptionTestInfo(MalformedRdfException.class, "Malformed RDF", ConstructorType.BOTH),
            new ExceptionTestInfo(MementoDatetimeFormatException.class, "Memento Datetime Format exception",
                    ConstructorType.STRING_THROWABLE),
            new ExceptionTestInfo(PathNotFoundRuntimeException.class, "Path Not Found", ConstructorType.BOTH),
            new ExceptionTestInfo(RelaxableServerManagedPropertyException.class,
                    "Relaxable Server Managed Property exception", ConstructorType.STRING),
            new ExceptionTestInfo(RepositoryRuntimeException.class, "Repository runtime exception",
                    ConstructorType.BOTH),
            new ExceptionTestInfo(RequestWithAclLinkHeaderException.class, "Request with ACL link header exception",
                    ConstructorType.STRING),
            new ExceptionTestInfo(ResourceTypeException.class, "Resource Type Exception", ConstructorType.STRING),
            new ExceptionTestInfo(ServerManagedPropertyException.class, "Server managed property exception",
                    ConstructorType.STRING),
            new ExceptionTestInfo(ServerManagedTypeException.class, "Server managed type exception",
                    ConstructorType.STRING),
            new ExceptionTestInfo(UnsupportedAlgorithmException.class, "Unsupported Algorithm exception",
                    ConstructorType.BOTH),
            new ExceptionTestInfo(UnsupportedMediaTypeException.class, "Unsupported media type exception",
                    ConstructorType.STRING)
    );

    @Test
    public void testAllRepositoryRuntimeExceptions() throws Exception {
        for (final ExceptionTestInfo exceptionTestInfo : EXCEPTIONS) {
            final Class<? extends RepositoryRuntimeException> exceptionClass = exceptionTestInfo.getExceptionClass();
            if (exceptionTestInfo.getConstructorType().equals(ConstructorType.BOTH)) {
                final RepositoryRuntimeException exception1 = exceptionClass.getConstructor(String.class)
                        .newInstance(exceptionTestInfo.getMessage());
                assertEquals(exceptionTestInfo.getMessage(), exception1.getMessage());
                assertInstanceOf(exceptionClass, exception1);
                final RepositoryRuntimeException exception2 = exceptionClass.getConstructor(String.class,
                                Throwable.class).newInstance(exceptionTestInfo.getMessage(), new Throwable());
                assertEquals(exceptionTestInfo.getMessage(), exception2.getMessage());
                assertInstanceOf(exceptionClass, exception2);
            } else {
                final RepositoryRuntimeException exception;
                if (exceptionTestInfo.getConstructorType().equals(ConstructorType.STRING_THROWABLE)) {
                    exception = exceptionClass.getConstructor(String.class, Throwable.class)
                            .newInstance(exceptionTestInfo.getMessage(), new Throwable());
                } else {
                    exception = exceptionClass.getConstructor(String.class)
                            .newInstance(exceptionTestInfo.getMessage());
                }
                assertEquals(exceptionTestInfo.getMessage(), exception.getMessage());
                assertInstanceOf(exceptionClass, exception);
            }
        }
    }

    /**
     * Test the MultipleConstraintViolationException
     */
    @Test
    public void testMultipleConstraintViolationException() {
        final var listOfExceptions = List.of(
                new ConstraintViolationException("Constraint violation 1"),
                new CannotCreateMementoException("Constraint violation 2"),
                new ACLAuthorizationConstraintViolationException("Constraint violation 3")
        );
        final var exception = new MultipleConstraintViolationException(listOfExceptions);
        assertEquals(3, exception.getExceptionTypes().size());
        assertEquals("Constraint violation 1\nConstraint violation 2\nConstraint violation 3\n",
                exception.getMessage());
    }

    @Test
    public void testConcurrentUpdateException() {
        final var exception = new ConcurrentUpdateException("resourceId", "conflictingTx", "existingTx");
        assertEquals("Cannot update resourceId because it is being updated by another transaction (existingTx).",
                exception.getMessage());
        assertInstanceOf(ConcurrentUpdateException.class, exception);
        assertEquals("Cannot update resourceId because it is being updated by another transaction",
                exception.getResponseMessage());
        assertEquals("existingTx", exception.getExistingTransactionId());
        assertEquals("conflictingTx", exception.getConflictingTransactionId());
    }

    @Test
    public void testPreconditionException() {
        final var exception = new PreconditionException("Precondition failed", 412);
        assertEquals("Precondition failed", exception.getMessage());
        assertInstanceOf(PreconditionException.class, exception);
        assertEquals(412, exception.getHttpStatus());
        final var exception2 = new PreconditionException("Precondition failed", 304);
        assertEquals("Precondition failed", exception2.getMessage());
        assertInstanceOf(PreconditionException.class, exception2);
        assertEquals(304, exception2.getHttpStatus());
        assertThrows(IllegalArgumentException.class, () ->
            new PreconditionException("Precondition failed", 500)
        );
    }

    @Test
    public void testTransactionRuntimeException() {
        final var exception = new TransactionRuntimeException("Transaction runtime exception");
        assertEquals("Transaction runtime exception", exception.getMessage());
        assertInstanceOf(TransactionRuntimeException.class, exception);
        final var exception2 = new TransactionRuntimeException("Transaction runtime exception", new Throwable());
        assertEquals("Transaction runtime exception", exception2.getMessage());
        assertInstanceOf(TransactionRuntimeException.class, exception2);
    }

    @Test
    public void testTransactionClosedException() {
        final var exception = new TransactionClosedException("Transaction closed exception");
        assertEquals("Transaction closed exception", exception.getMessage());
        assertInstanceOf(TransactionClosedException.class, exception);
        final var exception2 = new TransactionClosedException("Transaction closed exception", new Throwable());
        assertEquals("Transaction closed exception", exception2.getMessage());
        assertInstanceOf(TransactionClosedException.class, exception2);
    }

    @Test
    public void testTransactionNotFoundException() {
        final var exception = new TransactionNotFoundException("Transaction not found exception");
        assertEquals("Transaction not found exception", exception.getMessage());
        assertInstanceOf(TransactionNotFoundException.class, exception);
        final var exception2 = new TransactionNotFoundException("Transaction not found exception", new Throwable());
        assertEquals("Transaction not found exception", exception2.getMessage());
        assertInstanceOf(TransactionNotFoundException.class, exception2);
    }

    protected enum ConstructorType {
        STRING,
        STRING_THROWABLE,
        BOTH
    }

    static class ExceptionTestInfo {
        private final String message;
        private final ConstructorType constructorType;
        private final Class<? extends RepositoryRuntimeException> exceptionClass;

        ExceptionTestInfo(
                final Class<? extends RepositoryRuntimeException> exceptionClass,
                final String message,
                final ConstructorType constructorType
        ) {
            this.message = message;
            this.constructorType = constructorType;
            this.exceptionClass = exceptionClass;
        }

        ConstructorType getConstructorType() {
            return constructorType;
        }

        String getMessage() {
            return message;
        }

        Class<? extends RepositoryRuntimeException> getExceptionClass() {
            return exceptionClass;
        }
    }
}
