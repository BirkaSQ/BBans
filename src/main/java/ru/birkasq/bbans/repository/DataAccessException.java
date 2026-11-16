package ru.birkasq.bbans.repository;

/** Ошибка доступа к базе данных. */
public class DataAccessException extends RuntimeException {

    public DataAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}
