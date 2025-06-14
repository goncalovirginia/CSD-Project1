package csd.server.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.BAD_REQUEST, reason = "Insufficient funds")
public class InsufficientFundsException extends RuntimeException {

}
