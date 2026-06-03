package service

import "errors"

var ErrValidation = errors.New("validation error")
var ErrUnauthorized = errors.New("unauthorized")
var ErrForbidden = errors.New("forbidden")
var ErrWrongCredentials = errors.New("wrong credentials")
var ErrConflict = errors.New("conflict")

type FieldError struct {
	Field   string `json:"field"`
	Message string `json:"message"`
}

type ValidationError struct {
	Fields []FieldError
}

func (e ValidationError) Error() string {
	return ErrValidation.Error()
}

func (e ValidationError) Unwrap() error {
	return ErrValidation
}

func NewValidationError(fields ...FieldError) ValidationError {
	return ValidationError{Fields: fields}
}

func ValidationFields(err error) ([]FieldError, bool) {
	var validationErr ValidationError
	if errors.As(err, &validationErr) { return validationErr.Fields, true }

	return nil, false
}
