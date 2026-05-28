package repository

import (
	"context"
	"strconv"
)

func (r *SessionRepository) BanSubject(ctx context.Context, subject int64, revokeSessions bool) (int, error) {
	revoke := "0"
	if revokeSessions {
		revoke = "1"
	}

	return r.client.Eval(
		ctx,
		banSubjectScript,
		[]string{r.banKey, r.userSessionsKey(subject)},
		formatSubject(subject),
		r.keyPrefix,
		revoke,
	).Int()
}

func (r *SessionRepository) UnbanSubject(ctx context.Context, subject int64) error {
	return r.client.SRem(ctx, r.banKey, formatSubject(subject)).Err()
}

func (r *SessionRepository) IsSubjectBanned(ctx context.Context, subject int64) (bool, error) {
	return r.client.SIsMember(ctx, r.banKey, formatSubject(subject)).Result()
}

func formatSubject(subject int64) string {
	return strconv.FormatInt(subject, 10)
}
