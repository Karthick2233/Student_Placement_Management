package com.college.placement.exception;

@SuppressWarnings("serial")
public class ConflictException extends RuntimeException {
	
	public ConflictException(String message) {
		super(message);
	}
}