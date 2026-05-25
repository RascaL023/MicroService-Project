package service

import "errors"

var ErrValidation = errors.New("validation error")
var ErrUnauthorized = errors.New("unauthorized")
var ErrForbidden = errors.New("forbidden")
var ErrWrongCredentials = errors.New("wrong credentials")
