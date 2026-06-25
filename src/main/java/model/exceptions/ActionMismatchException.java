package model.exceptions;

/// ActionMismatchException
///
/// thrown if a draw action and place action are mixed up with the current state phase
public class ActionMismatchException extends CaracssonneException {
    /// ActionMismatchException
    public ActionMismatchException() {
        super("Can not perform type of action in current state. You most likely mixed up draw and place actions");
    }
}
