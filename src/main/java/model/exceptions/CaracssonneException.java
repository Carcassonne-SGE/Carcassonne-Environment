package model.exceptions;

/// CaracssonneException
///
/// base runtime exception for all env specific carcassonne errors
public class CaracssonneException extends RuntimeException {
    /// CaracssonneException
    ///
    /// @param message message describing the concrete error
    public CaracssonneException(String message) {
        super(message);
    }
}
