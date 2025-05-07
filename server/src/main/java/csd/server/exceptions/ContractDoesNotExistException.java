package csd.server.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.NOT_FOUND, reason = "Contract does not exist")
public class ContractDoesNotExistException extends RuntimeException {

}
