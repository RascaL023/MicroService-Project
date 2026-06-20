# LAST:
    - user-service:
        - add majors
        - status = ACTIVE/GRADUATED/DROP_OUT
    - auth-service:
        - after activation, need manual login
    - course-service:
        - detail button to popup detail and edit (FE)
        - last login
    - frontend:
        - 2nd password input on new password, toggle show password
        - cleaning UI to button first
        - toast notification/confirmation
        - delete kursus saya in /enrollments
    - course-service: 
        - weekly grade report by subject
        - nearest schedule follow to user

# PENDING:
    - course-service: 
        - gradebook better call
        - subject search (FE)
        - rank system
    - security:
        - log activity
        - rate limiting on reverse proxy
        - laod balancer on api-gateway

# TODO:
    - user-service: 
        - profile page:
            - update email
            - user profile picture
        - better excel import template
    - course-service:
        - learner ack assessments

# ??
    - deleted user on group member response mark deleted or just show
