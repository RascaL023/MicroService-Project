package http

import (
	"encoding/json"
	"math"
	"net/http"
	"time"
)

type envelope struct {
	Success   bool         `json:"success"`
	Message   string       `json:"message"`
	Data      any          `json:"data,omitempty"`
	Errors    any          `json:"errors,omitempty"`
	ErrorCode string       `json:"errorCode,omitempty"`
	Meta      responseMeta `json:"meta"`
}

type responseMeta struct {
	Pagination *paginationMeta `json:"pagination,omitempty"`
	Timestamp  string          `json:"timestamp"`
}

type paginationMeta struct {
	CurrentPage int  `json:"currentPage"`
	PerPage     int  `json:"perPage"`
	TotalItems  int  `json:"totalItems"`
	TotalPages  int  `json:"totalPages"`
	HasNextPage bool `json:"hasNextPage"`
	HasPrevPage bool `json:"hasPrevPage"`
}

func writeJSON(w http.ResponseWriter, status int, message string, data any) {
	writeResponse(w, status, envelope{
		Success: true,
		Message: message,
		Data:    data,
		Meta:    newMeta(nil),
	})
}

func writePage(w http.ResponseWriter, status int, message string, data any, page, size, total int) {
	writeResponse(w, status, envelope{
		Success: true,
		Message: message,
		Data:    data,
		Meta:    newMeta(newPaginationMeta(page, size, total)),
	})
}

func writeValidationError(w http.ResponseWriter, status int, message string, errors any) {
	writeResponse(w, status, envelope{
		Success: false,
		Message: message,
		Errors:  errors,
		Meta:    newMeta(nil),
	})
}

func writeError(w http.ResponseWriter, status int, message, errorCode string) {
	writeResponse(w, status, envelope{
		Success:   false,
		Message:   message,
		ErrorCode: errorCode,
		Meta:      newMeta(nil),
	})
}

func writeResponse(w http.ResponseWriter, status int, response envelope) {
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(status)
	_ = json.NewEncoder(w).Encode(response)
}

func newMeta(pagination *paginationMeta) responseMeta {
	return responseMeta{
		Pagination: pagination,
		Timestamp:  time.Now().UTC().Format(time.RFC3339),
	}
}

func newPaginationMeta(page, size, total int) *paginationMeta {
	totalPages := int(math.Ceil(float64(total) / float64(size)))
	if total == 0 {
		totalPages = 0
	}

	return &paginationMeta{
		CurrentPage: page,
		PerPage:     size,
		TotalItems:  total,
		TotalPages:  totalPages,
		HasNextPage: page < totalPages,
		HasPrevPage: page > 1,
	}
}
