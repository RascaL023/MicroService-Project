package http

import (
	"encoding/json"
	"net/http"
)

type envelope struct {
	Status int `json:"status"`
	Data   any `json:"data,omitempty"`
	Error  any `json:"error,omitempty"`
}

type pageEnvelope struct {
	Status int `json:"status"`
	Data   any `json:"data"`
	Page   int `json:"page"`
	Size   int `json:"size"`
	Total  int `json:"total"`
}

func writeJSON(w http.ResponseWriter, status int, data any) {
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(status)
	_ = json.NewEncoder(w).Encode(envelope{Status: status, Data: data})
}

func writePage(w http.ResponseWriter, status int, data any, page, size, total int) {
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(status)
	_ = json.NewEncoder(w).Encode(pageEnvelope{Status: status, Data: data, Page: page, Size: size, Total: total})
}

func writeError(w http.ResponseWriter, status int, message string) {
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(status)
	_ = json.NewEncoder(w).Encode(envelope{Status: status, Error: message})
}
